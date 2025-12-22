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

import android.app.Activity
import android.content.ComponentName
import android.content.res.Resources
import android.service.chooser.Flags.tapToShare
import com.android.intentresolver.R
import com.android.intentresolver.inject.ActivityOwned
import com.android.intentresolver.inject.ApplicationOwned
import com.android.intentresolver.inject.Background
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/** Dagger qualifier for identifying the tap event service ComponentName. */
@Qualifier @MustBeDocumented @Retention(AnnotationRetention.RUNTIME)
annotation class TapEventService

/** Dagger qualifier for identifying the fulfillment activity ComponentName. */
@Qualifier @MustBeDocumented @Retention(AnnotationRetention.RUNTIME)
annotation class TapShareFulfillmentActivity

@Module
@InstallIn(SingletonComponent::class)
abstract class TapTargetModule {

    companion object {
        @Provides
        @Singleton
        @TapEventService
        // TODO(b/461778971): Provide the real component name in a follow-up CL.
        fun provideTapEventServiceComponent(
            @ApplicationOwned resources: Resources
        ): ComponentName? = null

        @Provides
        @Singleton
        @TapShareFulfillmentActivity
        // TODO(b/461778971): Provide the real component name in a follow-up CL.
        fun provideTapShareFulfillmentActivityComponent(
            @ApplicationOwned resources: Resources
        ): ComponentName? = null
    }
}

@Module
@InstallIn(ActivityComponent::class)
object TapShareModule {

    @Provides
    @ActivityScoped
    fun provideTapEventServiceConnector(
        @TapEventService tapEventService: ComponentName?
    ): TapEventServiceConnector = TapEventServiceConnector(tapEventService)

    @Provides
    @ActivityScoped
    fun provideTapShareController(
        activity: Activity,
        @TapEventService tapEventService: ComponentName?,
        @TapShareFulfillmentActivity tapShareFulfillmentActivity: ComponentName?,
        @ActivityOwned activityScope: CoroutineScope,
        connector: TapEventServiceConnector,
        @Background ioDispatcher: CoroutineDispatcher
    ): ITapShareController =
        if (!tapToShare() || tapEventService == null || tapShareFulfillmentActivity == null) {
            NoOpTapShareController
        } else {
            TapShareController(
                activity,
                tapShareFulfillmentActivity,
                tapEventService,
                connector,
                activityScope,
                ioDispatcher
            )
        }
}
