package com.managehouse.ingest.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Uma transação capturada, aguardando envio ao backend. A PK é o externalId (hash da
 * notificação), então reposts do Android não duplicam localmente.
 */
@Entity(tableName = "pending_tx")
data class PendingTx(
    @PrimaryKey val externalId: String,
    val rawText: String,
    val packageName: String,
    val timestamp: Long,
    val destination: String,          // "personal" ou "house"
    val expenseTypeId: Long?,         // escolhido pelo usuário quando "house"
    // Preenchidos no registro manual (null nas notificações — a IA extrai no backend).
    val amount: Double? = null,
    val description: String? = null,
    val sent: Boolean = false
)
