package com.managehouse.ingest.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.managehouse.ingest.MoneyApp
import com.managehouse.ingest.R
import com.managehouse.ingest.data.AppDatabase
import com.managehouse.ingest.data.PendingTx
import com.managehouse.ingest.data.SettingsStore
import com.managehouse.ingest.ui.ClassifyActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Captura notificações dos apps de banco monitorados. Ao capturar, GRAVA a transação no Room
 * imediatamente e posta uma notificação própria para o usuário classificar (Casa/Lucas).
 *
 * Por que não abre a tela direto: NotificationListenerService roda em background e, desde o
 * Android 10, startActivity a partir de background é bloqueado pelo sistema — a Activity nunca
 * abriria. A notificação (com PendingIntent) é o caminho permitido: o toque do usuário abre a tela.
 */
class TxNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Listener CONECTADO — pronto para capturar notificações.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "Listener DESCONECTADO — solicitando rebind.")
        // Reata o binding após reboot/atualização/kill do sistema.
        requestRebind(android.content.ComponentName(this, TxNotificationListenerService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg == null) {
            Log.d(TAG, "Notificação sem packageName — ignorada.")
            return
        }
        val extras = sbn.notification?.extras
        if (extras == null) {
            Log.d(TAG, "[$pkg] sem extras — ignorada.")
            return
        }

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        // InboxStyle/MessagingStyle às vezes só preenchem EXTRA_TEXT_LINES.
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString(" ") { it.toString() }.orEmpty()
        val body = big.ifBlank { text }.ifBlank { lines }
        val rawText = listOf(title, body).filter { it.isNotBlank() }.joinToString(" — ")

        Log.d(TAG, "Notificação recebida [$pkg]: \"${rawText.take(80)}\"")

        if (rawText.isBlank()) {
            Log.d(TAG, "[$pkg] texto vazio (layout custom?) — ignorada.")
            return
        }

        scope.launch {
            try {
                val settings = SettingsStore(applicationContext)

                // Diagnóstico: registra o app de TODA notificação de texto (para descobrir o pacote real).
                val appLabel = runCatching {
                    val pm = applicationContext.packageManager
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                }.getOrDefault(pkg)
                settings.recordSeenPackage("$appLabel|$pkg")

                val monitored = settings.monitoredPackages()
                if (pkg !in monitored) {
                    Log.d(TAG, "[$pkg] não está na lista de monitorados (${monitored.size} apps) — ignorada.")
                    return@launch
                }

                val externalId = Hashing.externalId(pkg, rawText, sbn.postTime)
                val dao = AppDatabase.get(applicationContext).pendingTxDao()

                // Dedupe: se já existe, não regrava nem re-notifica.
                if (dao.byId(externalId) != null) {
                    Log.d(TAG, "[$pkg] já capturada (externalId=$externalId) — ignorada.")
                    return@launch
                }

                // Grava JÁ no Room — não depende de a tela abrir.
                dao.insert(
                    PendingTx(
                        externalId = externalId,
                        rawText = rawText,
                        packageName = pkg,
                        timestamp = sbn.postTime,
                        destination = null,
                        classified = false
                    )
                )
                Log.i(TAG, "[$pkg] CAPTURADA e salva (externalId=$externalId). Postando notificação.")
                postClassifyNotification(externalId, rawText)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar notificação [$pkg]", e)
            }
        }
    }

    /** Notificação cujo toque abre a tela de classificação (launch permitido: vem de tap do usuário). */
    private fun postClassifyNotification(externalId: String, rawText: String) {
        val intent = Intent(applicationContext, ClassifyActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(ClassifyActivity.EXTRA_EXTERNAL_ID, externalId)
        }
        val pending = PendingIntent.getActivity(
            applicationContext,
            externalId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, MoneyApp.CLASSIFY_CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Classificar gasto")
            .setContentText(rawText.take(100))
            .setStyle(NotificationCompat.BigTextStyle().bigText(rawText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext)
                .notify(externalId.hashCode(), notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS não concedida (Android 13+): a transação já está salva no Room.
            Log.w(TAG, "Sem permissão para notificar; transação salva mas sem aviso visual.", e)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "TxListener"
    }
}
