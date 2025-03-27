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

package com.android.intentresolver.actions

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.android.intentresolver.ChooserActionFactory
import com.android.intentresolver.R
import com.android.intentresolver.chooser.DisplayResolveInfo
import com.android.intentresolver.chooser.TargetInfo
import com.android.intentresolver.inject.Background
import com.android.intentresolver.platform.ImageEditor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Optional
import java.util.function.Consumer
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/** Creates target info to launch the image editor when appropriate. */
class ImageEditorActionFactory
@Inject
constructor(
    @ApplicationContext private val context: Context,
    @Background private val backgroundDispatcher: CoroutineDispatcher,
    @ImageEditor private val imageEditor: Optional<ComponentName>,
    private val packageManager: PackageManager,
    private val contentResolver: ContentResolver,
) {
    /**
     * Get a TargetInfo for the image editor for the given targetIntent. If none is available, call
     * back with null.
     */
    fun getImageEditorTargetInfoAsync(
        clientScope: CoroutineScope,
        targetIntent: Intent,
        editorAvailable: Consumer<TargetInfo?>,
    ) {
        clientScope.async { editorAvailable.accept(getImageEditorTargetInfo(targetIntent)) }
    }

    /**
     * Get a TargetInfo for the image editor for the given targetIntent. If none is available,
     * return null.
     */
    suspend fun getImageEditorTargetInfo(targetIntent: Intent): TargetInfo? {
        if (Intent.ACTION_SEND != targetIntent.action) {
            return null
        }

        return withContext(backgroundDispatcher) {
            val resolveIntent = Intent(targetIntent)

            // Retain only URI permission grant flags if present. Other flags may prevent the scene
            // transition animation from running (i.e FLAG_ACTIVITY_NO_ANIMATION,
            // FLAG_ACTIVITY_NEW_TASK, FLAG_ACTIVITY_NEW_DOCUMENT) but also not needed.
            resolveIntent.setFlags(
                targetIntent.flags and ChooserActionFactory.URI_PERMISSION_INTENT_FLAGS
            )

            imageEditor.ifPresent { resolveIntent.setComponent(it) }

            resolveIntent.setAction(Intent.ACTION_EDIT)
            resolveIntent.putExtra(
                ChooserActionFactory.EDIT_SOURCE,
                ChooserActionFactory.EDIT_SOURCE_SHARESHEET,
            )

            if (resolveIntent.data == null) {
                val uri = resolveIntent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                if (uri != null) {
                    val mimeType = contentResolver.getType(uri)
                    resolveIntent.setDataAndType(uri, mimeType)
                }
            }

            displayResolveInfoForIntent(targetIntent, resolveIntent)
        }
    }

    private fun displayResolveInfoForIntent(
        targetIntent: Intent,
        resolveIntent: Intent,
    ): DisplayResolveInfo? {
        val resolveInfo =
            packageManager.resolveActivity(resolveIntent, PackageManager.GET_META_DATA)
        if (resolveInfo?.activityInfo == null) {
            Log.e(TAG, "Device-specified editor ($imageEditor) not available")
            return null
        }

        val displayResolveInfo =
            DisplayResolveInfo.newDisplayResolveInfo(
                targetIntent,
                resolveInfo,
                context.getString(R.string.screenshot_edit),
                "",
                resolveIntent,
            )
        displayResolveInfo.displayIconHolder.displayIcon =
            context.getDrawable(com.android.internal.R.drawable.ic_screenshot_edit)
        return displayResolveInfo
    }

    companion object {
        const val TAG = "EditActionController"
    }
}
