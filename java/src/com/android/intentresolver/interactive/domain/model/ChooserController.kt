/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.intentresolver.interactive.domain.model

import android.content.Intent
import android.service.chooser.IChooserController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

private val NotSet = Intent()

class ChooserController : IChooserController.Stub() {
    private val updates = MutableStateFlow<Intent?>(NotSet)
    private val _targetStatusFlow = MutableStateFlow(TargetStatus.NotSet)

    val chooserIntent: Flow<Intent?>
        get() = updates.filter { it !== NotSet }

    val targetStatusFlow: Flow<Boolean> =
        _targetStatusFlow.filter { it != TargetStatus.NotSet }.map { it == TargetStatus.Enabled }

    override fun updateIntent(chooserIntent: Intent?) {
        updates.value = chooserIntent
    }

    override fun collapse() {
        // TODO (b/404593897)
    }

    override fun setTargetsEnabled(isEnabled: Boolean) {
        _targetStatusFlow.value = if (isEnabled) TargetStatus.Enabled else TargetStatus.Disabled
    }

    private enum class TargetStatus {
        NotSet,
        Enabled,
        Disabled,
    }
}
