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

package com.android.intentresolver.interactive.domain.interactor

import android.graphics.Rect
import android.service.chooser.IChooserController
import android.service.chooser.IChooserControllerCallback
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "SessionCallback"

class SafeChooserControllerCallback(private val delegate: IChooserControllerCallback) :
    IChooserControllerCallback by delegate {
    private val isOnCloseReported = AtomicBoolean(false)

    override fun registerChooserController(updater: IChooserController?) {
        if (!isAlive) return
        runCatching { delegate.registerChooserController(updater) }
            .onFailure { Log.e(TAG, "Failed to invoke registerChooserController", it) }
    }

    override fun onSizeChanged(size: Rect) {
        if (!isAlive) return
        runCatching { delegate.onSizeChanged(size) }
            .onFailure { Log.e(TAG, "Failed to invoke onDrawerVerticalOffsetChanged", it) }
    }

    override fun onClosed() {
        if (!isAlive) return
        if (isOnCloseReported.compareAndSet(false, true)) {
            runCatching { delegate.onClosed() }
                .onFailure { Log.e(TAG, "Failed to invoke onClosed", it) }
        } else {
            Log.d(TAG, "Session closure has already been reported")
        }
    }

    private val isAlive: Boolean
        get() = delegate.asBinder().isBinderAlive
}
