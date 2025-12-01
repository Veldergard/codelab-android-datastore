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

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import com.codelab.android.datastore.UserPreferences
import com.codelab.android.datastore.UserPreferences.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Class that handles saving and retrieving user preferences
 */
class UserPreferencesRepository(
    private val userPreferencesStore: DataStore<UserPreferences>,
) {

    private val tag: String = "UserPreferencesRepo"

    val userPreferencesFlow: Flow<UserPreferences> = userPreferencesStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(tag, "Error reading sort order preferences.", exception)
                emit(UserPreferences.getDefaultInstance())
            } else {
                throw exception
            }
        }

    suspend fun updateShowCompleted(completed: Boolean) {
        userPreferencesStore.updateData { preferences ->
            preferences.toBuilder().setShowCompleted(completed).build()
        }
    }

    suspend fun enableSortByDeadline(enable: Boolean) {
        // updateData handles data transactionally, ensuring that if the sort is updated at the same
        // time from another thread, we won't have conflicts
        userPreferencesStore.updateData { preferences ->
            val currentOrder = preferences.sortOrder
            val newSortOrder =
                if (enable) {
                    if (currentOrder == SortOrder.BY_PRIORITY) {
                        SortOrder.BY_DEADLINE_AND_PRIORITY
                    } else {
                        SortOrder.BY_DEADLINE
                    }
                } else {
                    if (currentOrder == SortOrder.BY_DEADLINE_AND_PRIORITY) {
                        SortOrder.BY_PRIORITY
                    } else {
                        SortOrder.NONE
                    }
                }
            preferences.toBuilder().setSortOrder(newSortOrder).build()
        }
    }

    suspend fun enableSortByPriority(enable: Boolean) {
        userPreferencesStore.updateData { preferences ->
            val currentOrder = preferences.sortOrder
            val newSortOrder =
                if (enable) {
                    if (currentOrder == SortOrder.BY_DEADLINE) {
                        SortOrder.BY_DEADLINE_AND_PRIORITY
                    } else {
                        SortOrder.BY_PRIORITY
                    }
                } else {
                    if (currentOrder == SortOrder.BY_DEADLINE_AND_PRIORITY) {
                        SortOrder.BY_DEADLINE
                    } else {
                        SortOrder.NONE
                    }
                }
            preferences.toBuilder().setSortOrder(newSortOrder).build()
        }
    }

}
