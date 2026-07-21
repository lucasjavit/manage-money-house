package com.managehouse.ingest.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTxDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tx: PendingTx)

    @Update
    suspend fun update(tx: PendingTx)

    @Query("SELECT * FROM pending_tx WHERE externalId = :externalId LIMIT 1")
    suspend fun byId(externalId: String): PendingTx?

    /** Prontas para envio: já classificadas e ainda não enviadas. */
    @Query("SELECT * FROM pending_tx WHERE classified = 1 AND sent = 0 ORDER BY timestamp ASC")
    suspend fun pending(): List<PendingTx>

    /** Capturadas mas ainda sem destino (aguardando o usuário classificar). Reativo para a UI. */
    @Query("SELECT * FROM pending_tx WHERE classified = 0 ORDER BY timestamp DESC")
    fun unclassified(): Flow<List<PendingTx>>

    @Query("UPDATE pending_tx SET sent = 1 WHERE externalId = :externalId")
    suspend fun markSent(externalId: String)

    @Query("SELECT COUNT(*) FROM pending_tx WHERE classified = 0")
    suspend fun unclassifiedCount(): Int
}
