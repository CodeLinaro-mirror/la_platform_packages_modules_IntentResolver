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
import android.util.Log
import androidx.annotation.OpenForTesting
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * A wrapper for [TapToShareClient] to facilitate testing.
 *
 * [TapToShareClient] is a final class, which makes it difficult to mock in unit tests. This
 * wrapper interface can be easily mocked.
 */
interface TapToShareClientWrapper {
    fun startSession(
        component: ComponentName,
        referrer: Uri?,
        executor: Executor,
        listener: TapToShareClient.SessionListener
    )
    fun endSession()
}

/**
 * The default implementation of [TapToShareClientWrapper] which passes calls to a real
 * [TapToShareClient] instance.
 */
class DefaultTapToShareClientWrapper(private val context: Context) : TapToShareClientWrapper {
    private val client: TapToShareClient = TapToShareClient(context)

    override fun startSession(
        component: ComponentName,
        referrer: Uri?,
        executor: Executor,
        listener: TapToShareClient.SessionListener
    ) {
        client.startSession(component, referrer, executor, listener)
    }

    override fun endSession() {
        client.endSession()
    }
}

/**
 * Manages the connection to the tap event service.
 */
@OpenForTesting
open class TapEventServiceConnector @Inject constructor(
    @param:TapEventService private val component: ComponentName?
) {
    @OpenForTesting
    open suspend fun awaitNearbyDeviceSelection(
        referrer: Uri?,
        userContext: Context
    ): Unit = suspendCancellableCoroutine { continuation ->
        val component = component ?: run {
            continuation.cancel(
                CancellationException("Tap event service component is not specified")
            )
            return@suspendCancellableCoroutine
        }

        val client = createClientWrapper(userContext)

        val listener = object : TapToShareClient.SessionListener {
            override fun onDeviceTapped() {
                Log.d(TAG, "onDeviceTapped called")
                continuation.takeIf { it.isActive }?.resume(Unit)
            }

            override fun onConnectionFailed(e: Exception) {
                Log.e(TAG, "Tap event service connection failed", e)
                continuation.takeIf { it.isActive }
                    ?.cancel(CancellationException("Connection failed", e))
            }
        }

        client.startSession(component, referrer, userContext.mainExecutor, listener)

        continuation.invokeOnCancellation {
            Log.d(TAG, "Cancelling tap event service connection due to cancellation")
            client.endSession()
        }
    }

    @OpenForTesting
    open fun createClientWrapper(userContext: Context): TapToShareClientWrapper {
        return DefaultTapToShareClientWrapper(userContext)
    }

    companion object {
        private const val TAG = "TapEventServiceConnector"
    }
}
