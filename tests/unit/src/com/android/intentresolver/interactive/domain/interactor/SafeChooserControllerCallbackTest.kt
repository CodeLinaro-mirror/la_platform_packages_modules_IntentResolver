/*
 * Copyright 2025 The Android Open Source Project
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
import android.os.IBinder
import android.service.chooser.IChooserControllerCallback
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SafeChooserControllerCallbackTest {
    @Test
    fun doNotCallMethodsOnInactiveBinder() {
        val binder = mock<IBinder> { on { isBinderAlive } doReturn false }
        val callback = mock<IChooserControllerCallback> { on { asBinder() } doReturn binder }
        val testSubject = SafeChooserControllerCallback(callback)

        testSubject.registerChooserController(mock())
        testSubject.onBoundsChanged(Rect(0, 0, 0, 0), Rect(0, 0, 0, 0))
        testSubject.onClosed()

        verify(callback) {
            0 * { registerChooserController(any()) }
            0 * { onBoundsChanged(any(), any()) }
            0 * { onClosed() }
        }
    }

    @Test
    fun onClosedGetsSendOnlyOnce() {
        val binder = mock<IBinder> { on { isBinderAlive } doReturn true }
        val callback = mock<IChooserControllerCallback> { on { asBinder() } doReturn binder }
        val testSubject = SafeChooserControllerCallback(callback)

        testSubject.onClosed()
        testSubject.onClosed()

        verify(callback) { 1 * { onClosed() } }
    }
}
