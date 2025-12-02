/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codelab.android.datastore.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val SORT_ORDER_KEY = "sort_order"
private const val SHOW_COMPLETED_KEY = "show_completed"

enum class SortOrder {
    NONE,
    BY_DEADLINE,
    BY_PRIORITY,
    BY_DEADLINE_AND_PRIORITY
}

data class UserPreferences(
    val showCompleted: Boolean,
    val sortOrder: SortOrder,
)

/**
 * Class that handles saving and retrieving user preferences
 */
class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {

    private object PreferenceKeys {
        val SHOW_COMPLETED = booleanPreferencesKey(SHOW_COMPLETED_KEY)
        val SORT_ORDER = stringPreferencesKey(SORT_ORDER_KEY)
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            mapUserPreferences(preferences)
        }

    suspend fun updateShowCompleted(showCompleted: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SHOW_COMPLETED] = showCompleted
        }
    }

    suspend fun enableSortByDeadline(enable: Boolean) {
        dataStore.edit { preferences ->
            val currentOrder = SortOrder.valueOf(
                preferences[PreferenceKeys.SORT_ORDER] ?: SortOrder.NONE.name
            )

            val newSortOrder =
                if (enable) {
                    when (currentOrder) {
                        SortOrder.BY_PRIORITY, SortOrder.BY_DEADLINE_AND_PRIORITY -> {
                            SortOrder.BY_DEADLINE_AND_PRIORITY
                        }

                        else -> {
                            SortOrder.BY_DEADLINE
                        }
                    }
                } else {
                    when (currentOrder) {
                        SortOrder.BY_DEADLINE_AND_PRIORITY, SortOrder.BY_PRIORITY -> {
                            SortOrder.BY_PRIORITY
                        }

                        else -> {
                            SortOrder.NONE
                        }
                    }
                }

            preferences[PreferenceKeys.SORT_ORDER] = newSortOrder.name
        }
    }

    suspend fun enableSortByPriority(enable: Boolean) {
        dataStore.edit { preferences ->
            val currentOrder = SortOrder.valueOf(
                preferences[PreferenceKeys.SORT_ORDER] ?: SortOrder.NONE.name
            )

            val newSortOrder =
                if (enable) {
                    when (currentOrder) {
                        SortOrder.BY_DEADLINE, SortOrder.BY_DEADLINE_AND_PRIORITY -> {
                            SortOrder.BY_DEADLINE_AND_PRIORITY
                        }

                        else -> {
                            SortOrder.BY_PRIORITY
                        }
                    }
                } else {
                    when (currentOrder) {
                        SortOrder.BY_DEADLINE_AND_PRIORITY, SortOrder.BY_DEADLINE -> {
                            SortOrder.BY_DEADLINE
                        }

                        else -> {
                            SortOrder.NONE
                        }
                    }
                }

            preferences[PreferenceKeys.SORT_ORDER] = newSortOrder.name
        }
    }

    suspend fun fetchInitialPreferences() =
        mapUserPreferences(dataStore.data.first().toPreferences())

    private fun mapUserPreferences(preferences: Preferences): UserPreferences {
        // Get the sort order from preferences and convert it to a [SortOrder] object
        val sortOrder = SortOrder.valueOf(
            preferences[PreferenceKeys.SORT_ORDER] ?: SortOrder.NONE.name
        )

        // Get our show completed value, defaulting to false if not set:
        val showCompleted = preferences[PreferenceKeys.SHOW_COMPLETED] ?: false
        return UserPreferences(
            showCompleted,
            sortOrder
        )
    }
}
