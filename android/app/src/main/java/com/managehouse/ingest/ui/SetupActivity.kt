package com.managehouse.ingest.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.managehouse.ingest.data.SettingsStore
import kotlinx.coroutines.launch

/**
 * Tela inicial: configura URL do backend e token, habilita o listener de notificações,
 * e permite disparar uma notificação de teste (valida o fluxo sem gasto real).
 */
class SetupActivity : AppCompatActivity() {

    private lateinit var settings: SettingsStore
    private lateinit var urlField: EditText
    private lateinit var tokenField: EditText
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsStore(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }

        root.addView(TextView(this).apply { text = "Money Ingest — Configuração"; textSize = 20f })

        root.addView(TextView(this).apply { text = "URL do backend"; setPadding(0, 32, 0, 8) })
        urlField = EditText(this).apply {
            hint = SettingsStore.DEFAULT_BASE_URL
            setText(SettingsStore.DEFAULT_BASE_URL)
        }
        root.addView(urlField)

        root.addView(TextView(this).apply { text = "Token (X-Ingest-Token)"; setPadding(0, 24, 0, 8) })
        tokenField = EditText(this).apply {
            hint = "cole o token do backend"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        root.addView(tokenField)

        root.addView(Button(this).apply {
            text = "Salvar configuração"
            setOnClickListener { saveConfig() }
        })

        root.addView(Button(this).apply {
            text = "Habilitar leitura de notificações"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        })

        root.addView(Button(this).apply {
            text = "Notificação de teste"
            setOnClickListener { testFlow() }
        })

        statusView = TextView(this).apply { setPadding(0, 32, 0, 0) }
        root.addView(statusView)

        setContentView(root)
        loadConfig()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun loadConfig() {
        lifecycleScope.launch {
            urlField.setText(settings.baseUrl())
            tokenField.setText(settings.token())
            updateStatus()
        }
    }

    private fun saveConfig() {
        lifecycleScope.launch {
            settings.save(urlField.text.toString(), tokenField.text.toString())
            Toast.makeText(this@SetupActivity, "Configuração salva", Toast.LENGTH_SHORT).show()
            updateStatus()
        }
    }

    private fun updateStatus() {
        val enabled = isListenerEnabled()
        lifecycleScope.launch {
            val hasToken = settings.token().isNotBlank()
            statusView.text = buildString {
                append("Leitura de notificações: ").append(if (enabled) "ATIVA" else "DESATIVADA").append("\n")
                append("Token: ").append(if (hasToken) "configurado" else "faltando")
            }
        }
    }

    private fun isListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) == true
    }

    /** Simula uma notificação de banco: injeta um texto direto na tela de classificação. */
    private fun testFlow() {
        val fakeText = "Compra aprovada: IFOOD *IFOOD no valor de R\$ 47,90"
        val intent = Intent(this, ClassifyActivity::class.java).apply {
            putExtra(ClassifyActivity.EXTRA_EXTERNAL_ID, "test-" + System.currentTimeMillis())
            putExtra(ClassifyActivity.EXTRA_RAW_TEXT, fakeText)
            putExtra(ClassifyActivity.EXTRA_PACKAGE, "com.nu.production")
            putExtra(ClassifyActivity.EXTRA_TIMESTAMP, System.currentTimeMillis())
        }
        startActivity(intent)
    }
}
