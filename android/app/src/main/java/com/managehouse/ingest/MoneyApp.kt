package com.managehouse.ingest

import android.app.Application

class MoneyApp : Application() {
    companion object {
        // Dono das transações no backend (Lucas). Fixo por enquanto (não é multiusuário).
        const val USER_ID = 1L
    }
}
