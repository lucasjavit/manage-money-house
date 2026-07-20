package com.managehouse.ingest.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.managehouse.ingest.R
import com.managehouse.ingest.data.AppDatabase
import com.managehouse.ingest.data.PendingTx
import com.managehouse.ingest.data.SettingsStore
import com.managehouse.ingest.databinding.ActivityManualEntryBinding
import com.managehouse.ingest.work.Sync
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Tela inicial: registro manual de um gasto, escolhendo destino Casa ou Lucas.
 * Diferente da ClassifyActivity, aqui o usuário digita o valor — não há IA envolvida.
 * Reusa a mesma fila offline (Room + WorkManager) das notificações.
 */
class ManualEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManualEntryBinding
    private var houseTypes: List<Pair<Long, String>> = emptyList()
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManualEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.settingsBtn.setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }

        updateDateField()
        binding.dateField.setOnClickListener { pickDate() }

        // Casa selecionada => mostra o seletor de tipo.
        binding.destinationToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val isHouse = checkedId == binding.destHouseBtn.id
                binding.houseTypeContainer.visibility = if (isHouse) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
        binding.destinationToggle.check(binding.destPersonalBtn.id)

        binding.saveBtn.setOnClickListener { save() }

        loadHouseTypes()
    }

    private fun loadHouseTypes() {
        lifecycleScope.launch {
            houseTypes = SettingsStore(applicationContext).houseTypes()
            val labels = if (houseTypes.isEmpty()) {
                listOf("Abra Configurações e toque \"Atualizar tipos\"")
            } else {
                houseTypes.map { it.second }
            }
            binding.typeSpinner.adapter = ArrayAdapter(
                this@ManualEntryActivity,
                R.layout.spinner_item,
                labels
            ).apply { setDropDownViewResource(R.layout.spinner_dropdown_item) }
        }
    }

    private fun pickDate() {
        DatePickerDialog(
            this,
            { _, y, m, d ->
                calendar.set(y, m, d)
                updateDateField()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateField() {
        val d = calendar.get(Calendar.DAY_OF_MONTH)
        val m = calendar.get(Calendar.MONTH) + 1
        val y = calendar.get(Calendar.YEAR)
        binding.dateField.setText("%02d/%02d/%04d".format(d, m, y))
    }

    private fun save() {
        val amountText = binding.amountField.text?.toString()?.trim().orEmpty()
            .replace(".", "").replace(",", ".")
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Informe um valor válido", Toast.LENGTH_SHORT).show()
            return
        }

        val description = binding.descriptionField.text?.toString()?.trim().orEmpty()
        if (description.isEmpty()) {
            Toast.makeText(this, "Informe uma descrição", Toast.LENGTH_SHORT).show()
            return
        }

        val isHouse = binding.destinationToggle.checkedButtonId == binding.destHouseBtn.id
        var expenseTypeId: Long? = null
        if (isHouse) {
            val idx = binding.typeSpinner.selectedItemPosition
            if (idx !in houseTypes.indices) {
                Toast.makeText(this, "Escolha o tipo da casa", Toast.LENGTH_SHORT).show()
                return
            }
            expenseTypeId = houseTypes[idx].first
        }

        // Meia-noite do dia escolhido, para o backend derivar mês/ano corretos.
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val timestamp = cal.timeInMillis

        // Registro manual não tem notificação: gera um externalId único próprio.
        val externalId = "manual-" + System.currentTimeMillis()

        lifecycleScope.launch {
            AppDatabase.get(applicationContext).pendingTxDao().insert(
                PendingTx(
                    externalId = externalId,
                    rawText = "",
                    packageName = "manual",
                    timestamp = timestamp,
                    destination = if (isHouse) "house" else "personal",
                    expenseTypeId = expenseTypeId,
                    amount = amount,
                    description = description
                )
            )
            Sync.enqueue(applicationContext)
            Toast.makeText(applicationContext, "Gasto salvo. Será enviado ao Pi.", Toast.LENGTH_SHORT).show()
            clearForm()
        }
    }

    private fun clearForm() {
        binding.amountField.text?.clear()
        binding.descriptionField.text?.clear()
    }
}
