package com.managehouse.ingest.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.managehouse.ingest.MoneyApp
import com.managehouse.ingest.data.AppDatabase
import com.managehouse.ingest.data.SettingsStore
import com.managehouse.ingest.net.ApiFactory
import com.managehouse.ingest.net.IngestRequest

/**
 * Envia as transações pendentes ao backend. Roda com constraint de rede conectada,
 * então quando o celular volta ao Wi-Fi de casa a fila é drenada automaticamente.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsStore(applicationContext)
        val token = settings.token()
        if (token.isBlank()) {
            // Sem token configurado não há como enviar; tenta de novo depois.
            return Result.retry()
        }

        val api = ApiFactory.create(settings.baseUrl())
        val dao = AppDatabase.get(applicationContext).pendingTxDao()
        val pending = dao.pending()
        if (pending.isEmpty()) return Result.success()

        var anyNetworkFailure = false
        for (tx in pending) {
            try {
                val response = api.ingest(
                    token,
                    IngestRequest(
                        userId = MoneyApp.USER_ID,
                        externalId = tx.externalId,
                        rawText = tx.rawText,
                        timestamp = tx.timestamp,
                        packageName = tx.packageName,
                        // pending() só traz classified=true, então destination nunca é null aqui.
                        destination = tx.destination ?: "personal",
                        expenseTypeId = tx.expenseTypeId,
                        amount = tx.amount,
                        description = tx.description
                    )
                )
                // 200 (created/duplicate/needs_review) => tratado; some da fila.
                // 400 (payload inválido) também não adianta reenviar; marca como enviado.
                if (response.isSuccessful || response.code() == 400) {
                    dao.markSent(tx.externalId)
                } else {
                    // 401 (token) ou 5xx: mantém na fila para nova tentativa.
                    anyNetworkFailure = true
                }
            } catch (e: Exception) {
                // Falha de rede (fora de casa): mantém na fila.
                anyNetworkFailure = true
            }
        }

        return if (anyNetworkFailure) Result.retry() else Result.success()
    }
}
