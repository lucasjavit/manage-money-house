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
 * Aberta pelo toque na notificação de captura. Carrega a PendingTx do Room (pelo externalId),
 * o usuário escolhe o destino, e a linha é atualizada (classified = true) e enfileirada.
 * A tela NÃO extrai valor — o texto cru vai ao backend, onde a IA faz o resto.
 */
class ClassifyActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EXTERNAL_ID = "externalId"
    }

    private lateinit var binding: ActivityClassifyBinding
    private var externalId: String = ""
    private var current: PendingTx? = null
    private var houseTypes: List<Pair<Long, String>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        externalId = intent.getStringExtra(EXTRA_EXTERNAL_ID) ?: run { finish(); return }

        binding = ActivityClassifyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.personalBtn.setOnClickListener { classify("personal", null) }
        binding.saveHouseBtn.setOnClickListener {
            val idx = binding.typeSpinner.selectedItemPosition
            if (idx in houseTypes.indices) {
                classify("house", houseTypes[idx].first)
            } else {
                Toast.makeText(this, "Escolha um tipo", Toast.LENGTH_SHORT).show()
            }
        }

        loadTransaction()
        loadHouseTypes()
    }

    private fun loadTransaction() {
        lifecycleScope.launch {
            current = AppDatabase.get(applicationContext).pendingTxDao().byId(externalId)
            val tx = current
            if (tx == null) {
                Toast.makeText(this@ClassifyActivity, "Transação não encontrada", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            binding.rawTextView.text = tx.rawText
        }
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

    private fun classify(destination: String, expenseTypeId: Long?) {
        val tx = current ?: return
        lifecycleScope.launch {
            val dao = AppDatabase.get(applicationContext).pendingTxDao()
            dao.update(
                tx.copy(
                    destination = destination,
                    expenseTypeId = expenseTypeId,
                    classified = true
                )
            )
            Sync.enqueue(applicationContext)
            Toast.makeText(applicationContext, "Classificado. Será enviado ao Pi.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
