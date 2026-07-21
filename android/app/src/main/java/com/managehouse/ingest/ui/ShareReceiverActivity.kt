package com.managehouse.ingest.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.managehouse.ingest.data.AppDatabase
import com.managehouse.ingest.data.PendingTx
import com.managehouse.ingest.service.Hashing
import kotlinx.coroutines.launch

/**
 * Recebe texto compartilhado (Compartilhar > Money Ingest) de qualquer app — notificação do
 * banco, extrato, etc. Grava a transação no Room e abre a classificação. Não depende de
 * permissão de notificações nem do listener — é o caminho manual mais robusto.
 */
class ShareReceiverActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }?.trim()

        if (text.isNullOrBlank()) {
            Toast.makeText(this, "Nada para importar", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            val externalId = Hashing.externalId("share", text, System.currentTimeMillis())
            val dao = AppDatabase.get(applicationContext).pendingTxDao()

            if (dao.byId(externalId) == null) {
                dao.insert(
                    PendingTx(
                        externalId = externalId,
                        rawText = text,
                        packageName = "share",
                        timestamp = System.currentTimeMillis(),
                        destination = null,
                        classified = false
                    )
                )
            }

            startActivity(Intent(this@ShareReceiverActivity, ClassifyActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(ClassifyActivity.EXTRA_EXTERNAL_ID, externalId)
            })
            finish()
        }
    }
}
