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

package com.android.intentresolver.icons

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.platform.test.flag.junit.SetFlagsRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.filters.SmallTest
import com.android.intentresolver.ResolverDataProvider
import com.android.intentresolver.SimpleIconFactory
import com.android.intentresolver.TargetPresentationGetter
import com.android.intentresolver.chooser.SelectableTargetInfo
import com.android.intentresolver.createChooserTarget
import com.android.intentresolver.createShortcutInfo
import com.google.common.truth.Truth
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
class DefaultTargetDataLoaderTest {
    @get:Rule val flagRule = SetFlagsRule()
    val testDispatcher = UnconfinedTestDispatcher()
    private val appIcon = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    private val badgedBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    private val placeholderDrawable =
        BitmapDrawable(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))
    private val activityInfo = ActivityInfo()
    private val pm =
        mock<PackageManager> {
            on { getActivityInfo(any<ComponentName>(), anyInt()) } doReturn activityInfo
        }
    private val userContext =
        mock<Context> {
            on { getSystemService(any<String>()) } doReturn null
            on { packageManager } doReturn pm
        }
    private val context =
        mock<Context> { on { createContextAsUser(any(), any()) } doReturn userContext }
    private val presentationGetter =
        mock<TargetPresentationGetter> { on { getIconBitmap(anyOrNull()) } doReturn appIcon }
    private val presentationFactory =
        mock<TargetPresentationGetter.Factory> {
            on { makePresentationGetter(any<ActivityInfo>()) } doReturn presentationGetter
        }
    private val iconFactory =
        mock<SimpleIconFactory> {
            on { createAppBadgedIconBitmap(any<Drawable>(), eq(appIcon)) } doReturn badgedBitmap
        }
    private val lifecycleOwner = TestLifecycleOwner()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun test_ShortcutIconFailedToLoad_placeholderIsBadged() = runTest {
        lifecycleOwner.currentState = Lifecycle.State.RESUMED
        val testSubject =
            DefaultTargetDataLoader(
                context,
                lifecycleOwner.lifecycle,
                Provider { iconFactory },
                presentationFactory,
                testDispatcher,
                { placeholderDrawable },
                false,
            )
        val chooserTarget =
            createChooserTarget(
                "title",
                0.3f,
                ResolverDataProvider.createComponentName(1),
                "test_shortcut_id",
            )
        val shortcutInfo = createShortcutInfo("id", ResolverDataProvider.createComponentName(2), 3)
        val targetInfo =
            SelectableTargetInfo.newSelectableTargetInfo(
                null,
                null,
                mock(),
                chooserTarget,
                0.1f,
                shortcutInfo,
                null,
                mock(),
            ) as SelectableTargetInfo

        val resultRef = AtomicReference<Drawable?>()
        testSubject.getOrLoadDirectShareIcon(targetInfo, UserHandle.of(10)) { resultRef.set(it) }

        Truth.assertThat(resultRef.get()).isNotNull()
        verify(iconFactory) { 1 * { createAppBadgedIconBitmap(any<Drawable>(), eq(appIcon)) } }
    }
}
