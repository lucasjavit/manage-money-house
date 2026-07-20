package com.managehouse.ingest.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.managehouse.ingest.R
import com.managehouse.ingest.data.AppDatabase
import com.managehouse.ingest.data.PendingTx
import com.managehouse.ingest.data.SettingsStore
import com.managehouse.ingest.databinding.ActivityClassifyBinding
import com.managehouse.ingest.work.Sync
import kotlinx.coroutines.launch

/**
 * Tela disparada por cada notificação capturada. O usuário decide o destino:
 *  - "Meu gasto (Lucas)": transação pessoal, não vai à planilha da casa.
 *  - "Salvar na planilha da casa": usa o tipo escolhido no seletor (23 tipos, do cache local).
 * A tela NÃO extrai valor — encaminha o texto cru; a IA no backend faz o resto.
 */
class ClassifyActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EXTERNAL_ID = "externalId"
        const val EXTRA_RAW_TEXT = "rawText"
        const val EXTRA_PACKAGE = "package"
        const val EXTRA_TIMESTAMP = "timestamp"
    }

    private lateinit var binding: ActivityClassifyBinding
    private lateinit var externalId: String
    private lateinit var rawText: String
    private lateinit var pkg: String
    private var timestamp: Long = 0
    private var houseTypes: List<Pair<Long, String>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        externalId = intent.getStringExtra(EXTRA_EXTERNAL_ID) ?: run { finish(); return }
        rawText = intent.getStringExtra(EXTRA_RAW_TEXT).orEmpty()
        pkg = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())

        binding = ActivityClassifyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rawTextView.text = rawText

        binding.personalBtn.setOnClickListener { save("personal", null) }
        binding.saveHouseBtn.setOnClickListener {
            val idx = binding.typeSpinner.selectedItemPosition
            if (idx in houseTypes.indices) {
                save("house", houseTypes[idx].first)
            } else {
                Toast.makeText(this, "Escolha um tipo", Toast.LENGTH_SHORT).show()
            }
        }

        loadHouseTypes()
    }

    private fun loadHouseTypes() {
        lifecycleScope.launch {
            houseTypes = SettingsStore(applicationContext).houseTypes()
            val labels = if (houseTypes.isEmpty()) {
                listOf("Abra o app e toque \"Atualizar tipos\"")
            } else {
                houseTypes.map { it.second }
            }
            binding.typeSpinner.adapter = ArrayAdapter(
                this@ClassifyActivity,
                R.layout.spinner_item,
                labels
            ).apply { setDropDownViewResource(R.layout.spinner_dropdown_item) }
            binding.saveHouseBtn.isEnabled = houseTypes.isNotEmpty()
        }
    }

    private fun save(destination: String, expenseTypeId: Long?) {
        lifecycleScope.launch {
            AppDatabase.get(applicationContext).pendingTxDao().insert(
                PendingTx(
                    externalId = externalId,
                    rawText = rawText,
                    packageName = pkg,
                    timestamp = timestamp,
                    destination = destination,
                    expenseTypeId = expenseTypeId
                )
            )
            Sync.enqueue(applicationContext)
            Toast.makeText(applicationContext, "Salvo. Será enviado ao Pi.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
