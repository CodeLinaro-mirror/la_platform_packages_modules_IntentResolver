/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.intentresolver.icons

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.AsyncTask
import android.os.UserHandle
import android.util.Log
import android.util.SparseArray
import androidx.annotation.GuardedBy
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import com.android.intentresolver.Flags.badgeShortcutIconPlaceholders
import com.android.intentresolver.Flags.targetHoverAndKeyboardFocusStates
import com.android.intentresolver.SimpleIconFactory
import com.android.intentresolver.TargetPresentationGetter
import com.android.intentresolver.chooser.DisplayResolveInfo
import com.android.intentresolver.chooser.SelectableTargetInfo
import com.android.intentresolver.inject.ActivityOwned
import com.android.intentresolver.inject.Background
import com.android.intentresolver.util.hasValidIcon
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ActivityContext
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import javax.inject.Provider
import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "DefaultTargetDataLoader"

@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
annotation class IconPlaceholder

/** An actual [TargetDataLoader] implementation. */
// TODO: replace async tasks with coroutines.
class DefaultTargetDataLoader
@AssistedInject
constructor(
    @ActivityContext private val context: Context,
    @ActivityOwned private val lifecycle: Lifecycle,
    private val iconFactoryProvider: Provider<SimpleIconFactory>,
    private val presentationFactory: TargetPresentationGetter.Factory,
    @Background private val bgDispatcher: CoroutineDispatcher,
    @IconPlaceholder private val iconPlacehoderProvider: Provider<Drawable>,
    @Assisted private val isAudioCaptureDevice: Boolean,
) : TargetDataLoader {
    private val nextTaskId = AtomicInteger(0)
    @GuardedBy("self") private val activeTasks = SparseArray<AsyncTask<*, *, *>>()
    private val executor = bgDispatcher.asExecutor()

    init {
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    lifecycle.removeObserver(this)
                    destroy()
                }
            }
        )
    }

    override fun getOrLoadAppTargetIcon(
        info: DisplayResolveInfo,
        defaultUserHandle: UserHandle,
        callback: Consumer<Drawable>,
    ): Drawable? {
        val taskId = nextTaskId.getAndIncrement()
        LoadIconTask(context, info, presentationFactory) { bitmap ->
                removeTask(taskId)
                callback.accept(bitmap?.toDrawable() ?: loadIconPlaceholder())
            }
            .also { addTask(taskId, it) }
            .executeOnExecutor(executor)
        return null
    }

    override fun getOrLoadDirectShareIcon(
        info: SelectableTargetInfo,
        userHandle: UserHandle,
        callback: Consumer<Drawable>,
    ): Drawable? {
        if (badgeShortcutIconPlaceholders()) {
            loadDirectShareIcon(context.createContextAsUser(userHandle, 0), info, callback)
        } else {
            val taskId = nextTaskId.getAndIncrement()
            LoadDirectShareIconTask(
                    context.createContextAsUser(userHandle, 0),
                    info,
                    presentationFactory,
                    iconFactoryProvider,
                ) { bitmap ->
                    removeTask(taskId)
                    callback.accept(bitmap?.toDrawable() ?: loadIconPlaceholder())
                }
                .also { addTask(taskId, it) }
                .executeOnExecutor(executor)
        }
        return null
    }

    override fun loadLabel(info: DisplayResolveInfo, callback: Consumer<LabelInfo>) {
        val taskId = nextTaskId.getAndIncrement()
        LoadLabelTask(context, info, isAudioCaptureDevice, presentationFactory) { result ->
                removeTask(taskId)
                callback.accept(result)
            }
            .also { addTask(taskId, it) }
            .executeOnExecutor(executor)
    }

    override fun getOrLoadLabel(info: DisplayResolveInfo) {
        if (!info.hasDisplayLabel()) {
            val result =
                LoadLabelTask.loadLabel(context, info, isAudioCaptureDevice, presentationFactory)
            info.displayLabel = result.label
            info.extendedInfo = result.subLabel
        }
    }

    private fun addTask(id: Int, task: AsyncTask<*, *, *>) {
        synchronized(activeTasks) { activeTasks.put(id, task) }
    }

    private fun removeTask(id: Int) {
        synchronized(activeTasks) { activeTasks.remove(id) }
    }

    private fun loadIconPlaceholder(): Drawable = iconPlacehoderProvider.get()

    private fun destroy() {
        synchronized(activeTasks) {
            for (i in 0 until activeTasks.size()) {
                activeTasks.valueAt(i).cancel(false)
            }
            activeTasks.clear()
        }
    }

    private fun Bitmap.toDrawable(): Drawable {
        return if (targetHoverAndKeyboardFocusStates()) {
            HoverBitmapDrawable(this)
        } else {
            BitmapDrawable(context.resources, this)
        }
    }

    private fun loadDirectShareIcon(
        userContext: Context,
        info: SelectableTargetInfo,
        consumer: Consumer<Drawable>,
    ) {
        lifecycle.coroutineScope.launch {
            val iconDrawable =
                withContext(bgDispatcher) {
                    val icon = info.chooserTargetIcon?.takeIf { hasValidIcon(it) }
                    val iconDrawable =
                        getDirectTargetIconDrawable(userContext, icon, info.directShareShortcutInfo)
                    val appIcon =
                        info.chooserTargetComponentName?.let { getAppIcon(userContext, it) }
                    if (appIcon != null) {
                        iconFactoryProvider.get().use { sif ->
                            sif.createAppBadgedIconBitmap(iconDrawable, appIcon).toDrawable()
                        }
                    } else {
                        iconDrawable
                    }
                }
            if (isActive) {
                consumer.accept(iconDrawable)
            }
        }
    }

    private fun getDirectTargetIconDrawable(
        userContext: Context,
        icon: Icon?,
        shortcutInfo: ShortcutInfo?,
    ): Drawable {
        return (if (icon != null) {
            icon.loadDrawable(userContext)
        } else if (shortcutInfo != null) {
            userContext
                .getSystemService<LauncherApps?>(LauncherApps::class.java)
                ?.getShortcutIconDrawable(shortcutInfo, 0)
        } else {
            null
        }) ?: return loadIconPlaceholder()
    }

    private fun getAppIcon(userContext: Context, targetComponentName: ComponentName): Bitmap? =
        runCatching {
                val info = userContext.getPackageManager().getActivityInfo(targetComponentName, 0)
                presentationFactory.makePresentationGetter(info).getIconBitmap(null)
            }
            .onFailure { Log.e(TAG, "Could not find activity associated with ChooserTarget") }
            .getOrNull()

    @AssistedFactory
    interface Factory {
        fun create(isAudioCaptureDevice: Boolean): DefaultTargetDataLoader
    }
}
