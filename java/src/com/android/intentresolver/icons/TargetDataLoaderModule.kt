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

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.android.intentresolver.R
import com.android.intentresolver.SimpleIconFactory
import com.android.intentresolver.TargetPresentationGetter
import com.android.intentresolver.inject.Background
import com.android.intentresolver.inject.TargetDataLoading
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Provider
import kotlinx.coroutines.CoroutineDispatcher

private const val MAX_TARGET_DATA_LOADING_THREADS = 4

@Module
@InstallIn(ActivityComponent::class)
object TargetDataLoaderModule {
    /**
     * For target-data loading tasks (e.g. icon loading), provide a dispatcher over a
     * reasonably-sized thread pool so we don't cause unnecessary contention while loading for too
     * many targets at the same time.
     */
    @Provides
    @TargetDataLoading
    fun targetDataLoadingDispatcher(
        @Background dispatcher: CoroutineDispatcher
    ): CoroutineDispatcher = dispatcher.limitedParallelism(MAX_TARGET_DATA_LOADING_THREADS)

    @Provides
    @IconPlaceholder
    fun iconPlaceholder(@ActivityContext context: Context): Drawable =
        requireNotNull(context.getDrawable(R.drawable.resolver_icon_placeholder))

    @Provides
    fun simpleIconFactory(@ActivityContext context: Context): SimpleIconFactory =
        SimpleIconFactory.obtain(context)

    @Provides
    fun presentationGetterFactory(
        iconFactoryProvider: Provider<SimpleIconFactory>,
        packageManager: PackageManager,
        activityManager: ActivityManager,
    ): TargetPresentationGetter.Factory =
        TargetPresentationGetter.Factory(
            iconFactoryProvider,
            packageManager,
            activityManager.launcherLargeIconDensity,
        )

    @Provides
    @ActivityScoped
    @Caching
    fun cachingTargetDataLoader(
        dataLoaderFactory: DefaultTargetDataLoader.Factory
    ): TargetDataLoader =
        // Intended to be used in Chooser only thus the hardcoded isAudioCaptureDevice value.
        CachingTargetDataLoader(dataLoaderFactory.create(isAudioCaptureDevice = false))
}
