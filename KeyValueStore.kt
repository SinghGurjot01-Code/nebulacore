package com.nebula.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Small string key-value abstraction so repositories do not depend on DataStore directly. */
interface KeyValueStore {
    fun observe(key: String): Flow<String?>
    suspend fun put(key: String, value: String?)
}

private val Context.nebulaDataStore: DataStore<Preferences> by preferencesDataStore(name = "nebula_prefs")

class DataStoreKeyValueStore(context: Context) : KeyValueStore {
    private val dataStore = context.applicationContext.nebulaDataStore

    override fun observe(key: String): Flow<String?> =
        dataStore.data.map { prefs -> prefs[stringPreferencesKey(key)] }

    override suspend fun put(key: String, value: String?) {
        dataStore.edit { prefs ->
            val prefKey = stringPreferencesKey(key)
            if (value == null) prefs.remove(prefKey) else prefs[prefKey] = value
        }
    }
}

/** Used by unit tests and previews. */
class InMemoryKeyValueStore : KeyValueStore {
    private val values = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun observe(key: String): Flow<String?> = values.map { it[key] }

    override suspend fun put(key: String, value: String?) {
        values.value = if (value == null) values.value - key else values.value + (key to value)
    }
}
