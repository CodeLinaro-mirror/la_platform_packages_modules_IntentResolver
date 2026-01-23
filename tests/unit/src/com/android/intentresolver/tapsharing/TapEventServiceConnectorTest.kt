/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.intentresolver.tapsharing

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.service.chooser.TapToShareClient
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.Executor
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class TapEventServiceConnectorTest {

    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock
    private lateinit var mockUserContext: Context
    @Mock
    private lateinit var mockClientWrapper: TapToShareClientWrapper

    private val sessionListenerCaptor = argumentCaptor<TapToShareClient.SessionListener>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val testComponent = ComponentName("com.example", "com.example.TapEventService")
    private val testReferrer = Uri.parse("android-app://com.example.app")

    @Before
    fun setup() {
        whenever(mockUserContext.mainExecutor).thenReturn(Executor { it.run() })
    }

    @Test
    fun awaitNearbyDeviceSelection_nullComponent_cancels() = testScope.runTest {
        val connector = TapEventServiceConnector(null)
        var exception: Throwable? = null
        try {
            connector.awaitNearbyDeviceSelection(testReferrer, mockUserContext)
        } catch (e: Throwable) {
            exception = e
        }

        assertThat(exception).isInstanceOf(CancellationException::class.java)
        assertThat(exception).hasMessageThat().isEqualTo("Tap event service component is not specified")
    }

    @Test
    fun awaitNearbyDeviceSelection_onDeviceTapped_resumes() = testScope.runTest {
        val connector = object : TapEventServiceConnector(testComponent) {
            override fun createClientWrapper(userContext: Context): TapToShareClientWrapper {
                return mockClientWrapper
            }
        }

        val job = launch {
            connector.awaitNearbyDeviceSelection(testReferrer, mockUserContext)
        }

        verify(mockClientWrapper).startSession(
            eq(testComponent), eq(testReferrer), any(), sessionListenerCaptor.capture())

        sessionListenerCaptor.firstValue.onDeviceTapped()

        assertThat(job.isCompleted).isTrue()
        assertThat(job.isCancelled).isFalse()
    }

    @Test
    fun awaitNearbyDeviceSelection_onConnectionFailed_cancels() = testScope.runTest {
        val connector = object : TapEventServiceConnector(testComponent) {
            override fun createClientWrapper(userContext: Context): TapToShareClientWrapper {
                return mockClientWrapper
            }
        }
        var exception: Throwable? = null

        val job = launch {
            try {
                connector.awaitNearbyDeviceSelection(testReferrer, mockUserContext)
            } catch (e: CancellationException) {
                exception = e
            }
        }

        verify(mockClientWrapper).startSession(
            eq(testComponent), eq(testReferrer), any(), sessionListenerCaptor.capture())

        val connectionException = RuntimeException("Failed")
        sessionListenerCaptor.firstValue.onConnectionFailed(connectionException)

        assertThat(job.isCompleted).isTrue()
        assertThat(exception).isInstanceOf(CancellationException::class.java)
        assertThat(exception).hasMessageThat().isEqualTo("Connection failed")
        assertThat(exception?.cause).isEqualTo(connectionException)
    }

    @Test
    fun awaitNearbyDeviceSelection_coroutineCancelled_endsSession() = testScope.runTest {
        val connector = object : TapEventServiceConnector(testComponent) {
            override fun createClientWrapper(userContext: Context): TapToShareClientWrapper {
                return mockClientWrapper
            }
        }

        val job = launch {
            connector.awaitNearbyDeviceSelection(testReferrer, mockUserContext)
        }

        verify(mockClientWrapper).startSession(eq(testComponent), eq(testReferrer), any(), any())

        job.cancel()

        verify(mockClientWrapper).endSession()
    }

    @Test
    fun createClientWrapper_returnsDefaultWrapper() {
        val connector = TapEventServiceConnector(testComponent)

        val clientWrapper = connector.createClientWrapper(mockUserContext)

        assertThat(clientWrapper).isInstanceOf(DefaultTapToShareClientWrapper::class.java)
    }
}
