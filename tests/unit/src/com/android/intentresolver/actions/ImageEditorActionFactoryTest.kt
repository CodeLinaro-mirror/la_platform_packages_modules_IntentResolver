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
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.Optional
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.argForWhich
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class ImageEditorActionFactoryTest {
    val scheduler = TestCoroutineScheduler()
    val testDispatcher = UnconfinedTestDispatcher(scheduler)
    val preferredEditorComponent = ComponentName("preferred.package", "preferred.class")
    val fallbackEditorComponent = ComponentName("fallback.pkg", "fallback.cls")

    val contentResolver: ContentResolver = mock()

    fun createPackageManager(
        hasPreferredEditor: Boolean,
        hasFallbackEditor: Boolean,
    ): PackageManager = mock {
        if (hasPreferredEditor) {
            on {
                    resolveActivity(
                        argForWhich { preferredEditorComponent.equals(component) },
                        anyInt(),
                    )
                }
                .doReturn(resolveInfoForComponent(preferredEditorComponent))
        }
        if (hasFallbackEditor) {
            on {
                    resolveActivity(
                        argForWhich { fallbackEditorComponent.equals(component) },
                        anyInt(),
                    )
                }
                .doReturn(resolveInfoForComponent(fallbackEditorComponent))
        }
    }

    fun resolveInfoForComponent(component: ComponentName): ResolveInfo =
        ResolveInfo().apply {
            activityInfo =
                ActivityInfo().apply {
                    name = component.className
                    applicationInfo =
                        ApplicationInfo().apply { packageName = component.packageName }
                }
        }

    fun createFactory(
        hasPreferredEditor: Boolean = true,
        hasFallbackEditor: Boolean = true,
        preferredEditor: ComponentName? = preferredEditorComponent,
        fallbackEditor: ComponentName? = fallbackEditorComponent,
    ) =
        ImageEditorActionFactory(
            InstrumentationRegistry.getInstrumentation().getContext(),
            testDispatcher,
            Optional.ofNullable(preferredEditor),
            Optional.ofNullable(fallbackEditor),
            createPackageManager(hasPreferredEditor, hasFallbackEditor),
            contentResolver,
        )

    @Test
    fun test_getImageEditorTargetInfo() = runTest {
        val target = createFactory().getImageEditorTargetInfo(Intent(Intent.ACTION_SEND))
        assertThat(target).isNotNull()
        assertThat(target?.resolvedIntent?.component).isEqualTo(preferredEditorComponent)
        assertThat(target?.resolvedIntent?.action).isEqualTo(Intent.ACTION_EDIT)
    }

    @Test
    fun test_getImageEditorTargetInfo_preferredNotProvided() = runTest {
        val target =
            createFactory(preferredEditor = null)
                .getImageEditorTargetInfo(Intent(Intent.ACTION_SEND))
        assertThat(target).isNotNull()
        assertThat(target?.resolvedIntent?.component).isEqualTo(fallbackEditorComponent)
        assertThat(target?.resolvedIntent?.action).isEqualTo(Intent.ACTION_EDIT)
    }

    @Test
    fun test_getImageEditorTargetInfo_noComponentProvided() = runTest {
        val target =
            createFactory(preferredEditor = null, fallbackEditor = null)
                .getImageEditorTargetInfo(Intent(Intent.ACTION_SEND))
        assertThat(target).isNull()
    }

    @Test
    fun test_getImageEditorTargetInfo_nonSendAction() = runTest {
        val target = createFactory().getImageEditorTargetInfo(Intent(Intent.ACTION_VIEW))
        assertThat(target).isNull()
    }

    @Test
    fun test_getImageEditorTargetInfo_preferredNotAvailable() = runTest {
        val target =
            createFactory(hasPreferredEditor = false)
                .getImageEditorTargetInfo(Intent(Intent.ACTION_SEND))
        assertThat(target?.resolvedIntent?.component).isEqualTo(fallbackEditorComponent)
    }

    @Test
    fun test_getImageEditorTargetInfo_bothNotAvailable() = runTest {
        val target =
            createFactory(hasPreferredEditor = false, hasFallbackEditor = false)
                .getImageEditorTargetInfo(Intent(Intent.ACTION_SEND))
        assertThat(target).isNull()
    }
}
