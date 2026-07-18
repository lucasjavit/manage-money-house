package com.managehouse.ingest.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Configuração do app: URL do backend, token de ingestão e quais apps de banco monitorar.
 */
class SettingsStore(private val context: Context) {

    private val baseUrlKey = stringPreferencesKey("base_url")
    private val tokenKey = stringPreferencesKey("ingest_token")
    private val packagesKey = stringSetPreferencesKey("monitored_packages")
    // Cache dos tipos da casa, no formato "id:nome" por item, para não depender de rede na hora.
    private val houseTypesKey = stringSetPreferencesKey("house_types")

    companion object {
        const val DEFAULT_BASE_URL = "http://192.168.1.20:8081"
        // Apps de banco monitorados por padrão. O usuário pode editar.
        val DEFAULT_PACKAGES = setOf(
            "com.nu.production",        // Nubank
            "br.com.intermedium",       // Inter
            "com.c6bank.app",           // C6
            "com.itau",                 // Itaú
            "com.bradesco",             // Bradesco
            "br.com.bb.android",        // Banco do Brasil
            "com.santander.app"         // Santander
        )
    }

    suspend fun baseUrl(): String =
        context.dataStore.data.map { it[baseUrlKey] ?: DEFAULT_BASE_URL }.first()

    suspend fun token(): String =
        context.dataStore.data.map { it[tokenKey] ?: "" }.first()

    suspend fun monitoredPackages(): Set<String> =
        context.dataStore.data.map { it[packagesKey] ?: DEFAULT_PACKAGES }.first()

    suspend fun save(baseUrl: String, token: String) {
        context.dataStore.edit {
            it[baseUrlKey] = baseUrl.trim().trimEnd('/')
            it[tokenKey] = token.trim()
        }
    }

    suspend fun saveMonitoredPackages(packages: Set<String>) {
        context.dataStore.edit { it[packagesKey] = packages }
    }

    /** Salva os tipos da casa em cache local (cada item "id:nome"). */
    suspend fun saveHouseTypes(types: List<Pair<Long, String>>) {
        context.dataStore.edit { prefs ->
            prefs[houseTypesKey] = types.map { "${it.first}:${it.second}" }.toSet()
        }
    }

    /** Lê os tipos da casa do cache local, ordenados por id. */
    suspend fun houseTypes(): List<Pair<Long, String>> =
        context.dataStore.data.map { prefs ->
            (prefs[houseTypesKey] ?: emptySet()).mapNotNull { entry ->
                val i = entry.indexOf(':')
                if (i <= 0) return@mapNotNull null
                val id = entry.substring(0, i).toLongOrNull() ?: return@mapNotNull null
                id to entry.substring(i + 1)
            }.sortedBy { it.first }
        }.first()
}
