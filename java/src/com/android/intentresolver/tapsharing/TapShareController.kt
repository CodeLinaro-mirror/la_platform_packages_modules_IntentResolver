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
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.Context
import android.net.Uri
import android.os.UserHandle
import android.app.Activity
import android.util.Log
import com.android.intentresolver.data.model.ChooserRequest
import com.android.intentresolver.inject.ActivityOwned
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import com.android.intentresolver.inject.Background
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Delegate for the controller to communicate back to the hosting activity.
 */
interface TapShareDelegate {
    val referrer: Uri?
    val request: ChooserRequest
    fun onNearbyDeviceSelected(
        resolveInfo: ResolveInfo,
        sendIntent: Intent
    )
}

/**
 * Controller for tap-to-share logic.
 */
interface ITapShareController {
    fun setup(delegate: TapShareDelegate, currentUser: UserHandle)
}

/**
 * A no-op implementation of [ITapShareController] for when the feature is disabled.
 */
object NoOpTapShareController : ITapShareController {
    override fun setup(delegate: TapShareDelegate, currentUser: UserHandle) {}
}

/**
 * Implementation of the tap-to-share controller logic, extracted from ChooserActivity.
 */
class TapShareController(
    private val activity: Activity,
    @param:TapShareFulfillmentActivity private val tapShareFulfillmentActivity: ComponentName,
    @param:TapEventService private val tapEventServiceComponent: ComponentName,
    private val connector: TapEventServiceConnector,
    @param:ActivityOwned private val activityScope: CoroutineScope,
    @param:Background private val ioDispatcher: CoroutineDispatcher
) : ITapShareController {

    override fun setup(delegate: TapShareDelegate, currentUser: UserHandle) {
        // Tap share fulfillment activity intentionally omits the DEFAULT category in its
        // intent-filter. To check if it supports the current share intent, we must use a
        // "probe" intent that is identical to the original, but without the DEFAULT category.
        val probeIntent = Intent(delegate.request.targetIntent).apply {
            component = tapShareFulfillmentActivity
            removeCategory(Intent.CATEGORY_DEFAULT)
        }

        activityScope.launch {
            val userContext = getValidatedUserContext(currentUser, probeIntent) ?: return@launch

            connector.awaitNearbyDeviceSelection(delegate.referrer, userContext)
            onNearbyDeviceSelected(userContext.packageManager, delegate)
        }
    }

    private suspend fun getValidatedUserContext(
        currentUser: UserHandle,
        probeIntent: Intent
    ): Context? =
        withContext(ioDispatcher) {
            val context = createUserContext(currentUser) ?: return@withContext null

            if (!isComponentEnabled(context.packageManager)) {
                Log.d(TAG, "Tap share component not enabled, skipping.")
                return@withContext null
            }

            context.packageManager.resolveActivity(probeIntent, 0) ?: return@withContext null
            context
        }

    private suspend fun onNearbyDeviceSelected(packageManager: PackageManager, delegate: TapShareDelegate) {
        // Prepare the final send intent based on the original request.
        val sendIntent = Intent(delegate.request.targetIntent)
        sendIntent.component = tapShareFulfillmentActivity

        val ri = withContext(ioDispatcher) {
            packageManager.resolveActivity(sendIntent, /*flags=*/ 0)
        } ?: return

        delegate.onNearbyDeviceSelected(ri, sendIntent)
    }

    private fun createUserContext(currentUser: UserHandle): Context? =
        runCatching {
            activity.createPackageContextAsUser(activity.packageName, 0, currentUser)
        }.getOrNull()

    private fun isComponentEnabled(packageManager: PackageManager): Boolean =
        runCatching {
            when (packageManager.getComponentEnabledSetting(tapEventServiceComponent)) {
                COMPONENT_ENABLED_STATE_ENABLED -> true
                COMPONENT_ENABLED_STATE_DISABLED -> false
                COMPONENT_ENABLED_STATE_DEFAULT ->
                    // If the state is DEFAULT, fall back to the manifest value.
                    packageManager.getServiceInfo(
                        tapEventServiceComponent,
                        PackageManager.ComponentInfoFlags.of(0L)
                    ).isEnabled
                else -> false
            }
        }.getOrDefault(false)

    companion object {
        private const val TAG = "TapShareController"
    }
}
