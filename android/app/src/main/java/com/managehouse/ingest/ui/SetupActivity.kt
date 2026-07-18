package com.managehouse.ingest.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.managehouse.ingest.data.SettingsStore
import com.managehouse.ingest.databinding.ActivitySetupBinding
import com.managehouse.ingest.net.ApiFactory
import kotlinx.coroutines.launch

/**
 * Tela inicial: configura URL do backend e token, habilita o listener de notificações,
 * atualiza o cache dos tipos da casa, e dispara uma notificação de teste.
 */
class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private lateinit var settings: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsStore(this)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.saveBtn.setOnClickListener { saveConfig() }
        binding.enableListenerBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        binding.refreshTypesBtn.setOnClickListener { refreshHouseTypes() }
        binding.testBtn.setOnClickListener { testFlow() }

        loadConfig()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun loadConfig() {
        lifecycleScope.launch {
            binding.urlField.setText(settings.baseUrl())
            binding.tokenField.setText(settings.token())
            updateStatus()
        }
    }

    private fun saveConfig() {
        lifecycleScope.launch {
            settings.save(binding.urlField.text.toString(), binding.tokenField.text.toString())
            Toast.makeText(this@SetupActivity, "Configuração salva", Toast.LENGTH_SHORT).show()
            updateStatus()
            refreshHouseTypes()
        }
    }

    /** Busca os tipos da casa e guarda no cache local (usado pela tela de classificação). */
    private fun refreshHouseTypes() {
        lifecycleScope.launch {
            try {
                val api = ApiFactory.create(settings.baseUrl())
                val resp = api.expenseTypes()
                if (resp.isSuccessful) {
                    val types = resp.body().orEmpty().map { it.id to it.name }
                    settings.saveHouseTypes(types)
                    Toast.makeText(this@SetupActivity, "${types.size} tipos atualizados", Toast.LENGTH_SHORT).show()
                    updateStatus()
                } else {
                    Toast.makeText(this@SetupActivity, "Falha ao buscar tipos (HTTP ${resp.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SetupActivity, "Sem conexão com o Pi para buscar tipos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStatus() {
        val enabled = isListenerEnabled()
        lifecycleScope.launch {
            val hasToken = settings.token().isNotBlank()
            val typeCount = settings.houseTypes().size
            binding.statusView.text = buildString {
                append("Leitura de notificações: ").append(if (enabled) "ATIVA" else "DESATIVADA").append("\n")
                append("Token: ").append(if (hasToken) "configurado" else "faltando").append("\n")
                append("Tipos da casa em cache: ").append(typeCount)
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
