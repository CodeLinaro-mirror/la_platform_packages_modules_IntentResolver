/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.intentresolver.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.android.intentresolver.Flags.includeLinkStickyCheckbox
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/* Repository for storing and retrieving chooser selection data. */
class ChooserSelectionRepository
@Inject
constructor(@ApplicationContext private val context: Context) {
    private val preferences: SharedPreferences
        get() = context.getSharedPreferences("chooser_pin_settings", Context.MODE_PRIVATE)

    var isTextIncluded: Boolean = resolveTextIncluded()
        set(value) {
            field = value
            if (includeLinkStickyCheckbox()) {
                preferences.edit().putBoolean(INCLUDE_SHARED_TEXT_PREFS_KEY, value).apply()
            }
        }

    private fun resolveTextIncluded(): Boolean {
        return if (includeLinkStickyCheckbox()) {
            preferences.getBoolean(INCLUDE_SHARED_TEXT_PREFS_KEY, true)
        } else {
            true
        }
    }

    companion object {
        const val INCLUDE_SHARED_TEXT_PREFS_KEY = "include_shared_text"
    }
}
