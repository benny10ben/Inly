package com.ben.inly.data.local.prefs

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles saving and retrieving user preferences (currently just the note sorting rules).
 * I use SharedPreferences here since it's just a couple of strings, so there's no need to spin up a full database table.
 */
@Singleton
class SettingsManager @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val KEY_SORT_TYPE = "sort_type"
        private const val KEY_SORT_ORDER = "sort_order"
    }

    /**
     * Turns the old-school SharedPreferences listener into a reactive Kotlin Flow.
     * This means whenever the user changes their sort type, the UI instantly updates automatically without needing a manual refresh.
     */
    val sortTypeFlow: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == KEY_SORT_TYPE) {
                trySend(prefs.getString(KEY_SORT_TYPE, "LAST_EDITED") ?: "LAST_EDITED")
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        // Send the initial value right away so the UI has something to show on boot
        trySend(sharedPreferences.getString(KEY_SORT_TYPE, "LAST_EDITED") ?: "LAST_EDITED")

        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    /**
     * Just like the sort type, this listens for changes to ascending/descending order
     * and immediately pushes the new value down the pipeline.
     */
    val sortOrderFlow: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == KEY_SORT_ORDER) {
                trySend(prefs.getString(KEY_SORT_ORDER, "DESCENDING") ?: "DESCENDING")
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        trySend(sharedPreferences.getString(KEY_SORT_ORDER, "DESCENDING") ?: "DESCENDING")

        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    /**
     * Updates the user's sorting preferences.
     * I'm using .apply() instead of .commit() here because it writes to disk asynchronously,
     * which prevents the main UI thread from freezing or stuttering.
     */
    fun saveSortSettings(type: String, order: String) {
        sharedPreferences.edit()
            .putString(KEY_SORT_TYPE, type)
            .putString(KEY_SORT_ORDER, order)
            .apply()
    }
}