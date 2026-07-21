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
        binding.refreshSeenBtn.setOnClickListener { loadSeenPackages() }

        loadConfig()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        loadSeenPackages()
    }

    /** Mostra os apps cujas notificações chegaram; tocar num deles passa a monitorá-lo. */
    private fun loadSeenPackages() {
        lifecycleScope.launch {
            val seen = settings.seenPackages()
            val monitored = settings.monitoredPackages()
            if (seen.isEmpty()) {
                binding.seenPackagesView.text = "(nenhuma notificação capturada ainda)"
                return@launch
            }
            // Cada item guardado é "NomeApp|pacote".
            val lines = seen.sorted().joinToString("\n") { entry ->
                val parts = entry.split("|", limit = 2)
                val label = parts.getOrElse(0) { entry }
                val pkg = parts.getOrElse(1) { "" }
                val mark = if (pkg in monitored) "✓ " else "• "
                "$mark$label\n    $pkg"
            }
            binding.seenPackagesView.text = lines

            // Toque no texto abre a escolha de qual pacote passar a monitorar.
            binding.seenPackagesView.setOnClickListener { promptAdoptPackage(seen, monitored) }
        }
    }

    private fun promptAdoptPackage(seen: Set<String>, monitored: Set<String>) {
        val candidates = seen.map { it.split("|", limit = 2) }
            .filter { it.size == 2 && it[1] !in monitored }
        if (candidates.isEmpty()) {
            Toast.makeText(this, "Todos os apps vistos já estão monitorados", Toast.LENGTH_SHORT).show()
            return
        }
        val labels = candidates.map { "${it[0]} (${it[1]})" }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Monitorar qual app?")
            .setItems(labels) { _, which ->
                val pkg = candidates[which][1]
                lifecycleScope.launch {
                    settings.addMonitoredPackage(pkg)
                    Toast.makeText(this@SetupActivity, "Agora monitorando ${candidates[which][0]}", Toast.LENGTH_SHORT).show()
                    loadSeenPackages()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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

    /** Simula uma notificação: grava no Room (como o listener faz) e abre a classificação. */
    private fun testFlow() {
        val fakeText = "Compra aprovada: IFOOD *IFOOD no valor de R\$ 47,90"
        val externalId = "test-" + System.currentTimeMillis()
        lifecycleScope.launch {
            com.managehouse.ingest.data.AppDatabase.get(applicationContext).pendingTxDao().insert(
                com.managehouse.ingest.data.PendingTx(
                    externalId = externalId,
                    rawText = fakeText,
                    packageName = "test",
                    timestamp = System.currentTimeMillis(),
                    destination = null,
                    classified = false
                )
            )
            startActivity(Intent(this@SetupActivity, ClassifyActivity::class.java).apply {
                putExtra(ClassifyActivity.EXTRA_EXTERNAL_ID, externalId)
            })
        }
    }
}
