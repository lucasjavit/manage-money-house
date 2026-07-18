package com.managehouse.ingest.service

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.managehouse.ingest.data.SettingsStore
import com.managehouse.ingest.ui.ClassifyActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Captura notificações dos apps de banco monitorados e abre a tela de classificação.
 * NÃO faz parsing: envia o texto CRU — a IA no backend extrai valor/estabelecimento/tipo.
 */
class TxNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val rawText = listOf(title, big.ifBlank { text }).filter { it.isNotBlank() }.joinToString(" — ")

        if (rawText.isBlank()) return

        scope.launch {
            val monitored = SettingsStore(applicationContext).monitoredPackages()
            if (pkg !in monitored) return@launch

            val externalId = Hashing.externalId(pkg, rawText, sbn.postTime)
            val intent = Intent(applicationContext, ClassifyActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(ClassifyActivity.EXTRA_EXTERNAL_ID, externalId)
                putExtra(ClassifyActivity.EXTRA_RAW_TEXT, rawText)
                putExtra(ClassifyActivity.EXTRA_PACKAGE, pkg)
                putExtra(ClassifyActivity.EXTRA_TIMESTAMP, sbn.postTime)
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Log.w("TxListener", "Não foi possível abrir a tela: ${e.message}")
            }
        }
    }
}
