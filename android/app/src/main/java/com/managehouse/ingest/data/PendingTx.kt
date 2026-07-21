package com.managehouse.ingest.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Uma transação capturada, aguardando classificação e/ou envio ao backend. A PK é o externalId
 * (hash da notificação ou id manual), então reposts do Android não duplicam localmente.
 *
 * Fluxo da notificação: o listener grava a linha na hora (destination = null, classified = false)
 * e posta uma notificação; ao tocar, a ClassifyActivity define o destino e marca classified = true.
 * O SyncWorker só envia linhas classified = true. Registros manuais já nascem classificados.
 */
@Entity(tableName = "pending_tx")
data class PendingTx(
    @PrimaryKey val externalId: String,
    val rawText: String,
    val packageName: String,
    val timestamp: Long,
    val destination: String? = null,   // "personal" ou "house"; null = ainda não classificada
    val expenseTypeId: Long? = null,   // escolhido pelo usuário quando "house"
    // Preenchidos no registro manual (null nas notificações — a IA extrai no backend).
    val amount: Double? = null,
    val description: String? = null,
    val classified: Boolean = false,   // destino já definido pelo usuário
    val sent: Boolean = false
)
