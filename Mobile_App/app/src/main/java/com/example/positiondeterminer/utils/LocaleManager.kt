package com.example.positiondeterminer.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.localeDataStore: DataStore<Preferences> by preferencesDataStore(name = "locale_prefs")

class LocaleManager(private val context: Context) {
    
    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("app_language")
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_TURKISH = "tr"
    }
    
    val currentLanguage: Flow<String> = context.localeDataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: LANGUAGE_ENGLISH
    }
    
    suspend fun setLanguage(languageCode: String) {
        context.localeDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = languageCode
        }
    }
    
    fun updateLocale(languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = context.resources.configuration
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
}
