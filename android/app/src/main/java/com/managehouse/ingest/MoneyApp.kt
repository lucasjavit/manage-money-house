package com.managehouse.ingest

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MoneyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CLASSIFY_CHANNEL,
                "Classificar gastos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisa quando um gasto do banco é capturado, para classificar."
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        // Dono das transações no backend (Lucas). Fixo por enquanto (não é multiusuário).
        const val USER_ID = 1L
        const val CLASSIFY_CHANNEL = "classify_expenses"
    }
}
