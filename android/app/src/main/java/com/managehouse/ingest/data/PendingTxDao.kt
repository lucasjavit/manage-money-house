package com.managehouse.ingest.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingTxDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tx: PendingTx)

    @Query("SELECT * FROM pending_tx WHERE sent = 0 ORDER BY timestamp ASC")
    suspend fun pending(): List<PendingTx>

    @Query("UPDATE pending_tx SET sent = 1 WHERE externalId = :externalId")
    suspend fun markSent(externalId: String)

    @Query("SELECT COUNT(*) FROM pending_tx WHERE sent = 0")
    suspend fun pendingCount(): Int
}
