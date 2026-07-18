package com.managehouse.ingest.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.managehouse.ingest.data.AppDatabase
import com.managehouse.ingest.data.PendingTx
import com.managehouse.ingest.data.SettingsStore
import com.managehouse.ingest.net.ApiFactory
import com.managehouse.ingest.net.ExpenseType
import com.managehouse.ingest.work.Sync
import kotlinx.coroutines.launch

/**
 * Tela disparada por cada notificação capturada. O usuário decide "seu gasto" ou "da casa";
 * se da casa, escolhe o tipo (os 23 da planilha, com o valor extraído pela IA vindo depois).
 * A tela NÃO extrai valor — só encaminha o texto cru; a IA no backend faz o resto.
 */
class ClassifyActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EXTERNAL_ID = "externalId"
        const val EXTRA_RAW_TEXT = "rawText"
        const val EXTRA_PACKAGE = "package"
        const val EXTRA_TIMESTAMP = "timestamp"
    }

    private lateinit var externalId: String
    private lateinit var rawText: String
    private lateinit var pkg: String
    private var timestamp: Long = 0
    private var houseTypes: List<ExpenseType> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        externalId = intent.getStringExtra(EXTRA_EXTERNAL_ID) ?: run { finish(); return }
        rawText = intent.getStringExtra(EXTRA_RAW_TEXT).orEmpty()
        pkg = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }

        root.addView(TextView(this).apply {
            text = "Nova transação"
            textSize = 20f
        })
        root.addView(TextView(this).apply {
            text = rawText
            textSize = 15f
            setPadding(0, 24, 0, 32)
        })

        val spinner = Spinner(this).apply { visibility = View.GONE }
        root.addView(spinner)

        val personalBtn = Button(this).apply {
            text = "Meu gasto (Lucas)"
            setOnClickListener { save("personal", null) }
        }
        val houseBtn = Button(this).apply { text = "Gasto da casa" }
        val confirmHouseBtn = Button(this).apply {
            text = "Confirmar (casa)"
            visibility = View.GONE
            setOnClickListener {
                val idx = spinner.selectedItemPosition
                if (idx in houseTypes.indices) save("house", houseTypes[idx].id)
            }
        }
        houseBtn.setOnClickListener {
            if (houseTypes.isEmpty()) {
                Toast.makeText(this, "Tipos ainda não carregaram", Toast.LENGTH_SHORT).show()
            } else {
                spinner.visibility = View.VISIBLE
                confirmHouseBtn.visibility = View.VISIBLE
            }
        }

        root.addView(personalBtn)
        root.addView(houseBtn)
        root.addView(confirmHouseBtn)
        root.gravity = Gravity.CENTER_VERTICAL
        setContentView(root)

        loadHouseTypes(spinner)
    }

    private fun loadHouseTypes(spinner: Spinner) {
        lifecycleScope.launch {
            try {
                val api = ApiFactory.create(SettingsStore(applicationContext).baseUrl())
                val resp = api.expenseTypes()
                if (resp.isSuccessful) {
                    houseTypes = resp.body().orEmpty()
                    spinner.adapter = ArrayAdapter(
                        this@ClassifyActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        houseTypes.map { it.name }
                    )
                }
            } catch (_: Exception) {
                // Sem rede agora: o botão "da casa" avisa. "Meu gasto" segue funcionando offline.
            }
        }
    }

    private fun save(destination: String, expenseTypeId: Long?) {
        lifecycleScope.launch {
            val dao = AppDatabase.get(applicationContext).pendingTxDao()
            dao.insert(
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
