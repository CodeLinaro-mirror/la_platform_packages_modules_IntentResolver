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

package com.android.intentresolver.interactive.domain.model

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ChooserControllerTest {
    @Test
    fun test_updateIntent() = runTest {
        val intentOne = Intent(Intent.ACTION_SEND)
        val intentTwo = Intent(Intent.ACTION_VIEW)
        val testSubject = ChooserController()
        val receivedIntents = mutableListOf<Intent?>()

        val collectionJob = launch { testSubject.chooserIntent.toList(receivedIntents) }
        testScheduler.runCurrent()
        assertThat(receivedIntents).isEmpty()

        testSubject.updateIntent(intentOne)
        testScheduler.runCurrent()
        assertThat(receivedIntents).containsExactly(intentOne)

        testSubject.updateIntent(intentTwo)
        testScheduler.runCurrent()
        assertThat(receivedIntents).containsExactly(intentOne, intentTwo)

        testSubject.updateIntent(null)
        testScheduler.runCurrent()
        assertThat(receivedIntents).containsExactly(intentOne, intentTwo, null)

        collectionJob.cancel()
    }

    @Test
    fun test_setTargetsEnabled() = runTest {
        val testSubject = ChooserController()
        val receivedTargetStatuses = mutableListOf<Boolean>()

        val collectionJob = launch { testSubject.targetStatusFlow.toList(receivedTargetStatuses) }
        testScheduler.runCurrent()
        assertThat(receivedTargetStatuses).isEmpty()

        testSubject.setTargetsEnabled(true)
        testScheduler.runCurrent()
        assertThat(receivedTargetStatuses).containsExactly(true)

        testSubject.setTargetsEnabled(false)
        testScheduler.runCurrent()
        assertThat(receivedTargetStatuses).containsExactly(true, false)

        collectionJob.cancel()
    }

    @Test
    fun test_setMinimized() = runTest {
        val testSubject = ChooserController()
        val minimizeRequests = mutableListOf<Boolean>()

        val collectionJob = launch {
            for (isMinimized in testSubject.minimizeRequests) {
                minimizeRequests.add(isMinimized)
            }
        }
        testScheduler.runCurrent()

        assertThat(minimizeRequests).isEmpty()

        testSubject.setMinimized(true)
        testScheduler.runCurrent()
        testSubject.setMinimized(false)
        testScheduler.runCurrent()

        assertThat(minimizeRequests).containsExactly(true, false).inOrder()

        collectionJob.cancel()
    }
}
