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

package com.android.intentresolver

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.os.Message
import android.os.UserHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.intentresolver.chooser.TargetInfo
import com.android.intentresolver.data.model.ChooserRequest
import com.android.intentresolver.model.AbstractResolverComparator
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class ChooserListControllerTest {

    private val mockPackageManager = mock<PackageManager>()
    private val mockContext =
        mock<Context> {
            on { createContextAsUser(any(), any()) } doReturn mock
            on { packageManager } doReturn mockPackageManager
            var res =
                mock<Resources> {
                    on { configuration } doReturn Configuration().apply { locale = Locale.US }
                }
            on { resources } doReturn res
        }
    private val targetIntent = Intent("TEST_ACTION")
    private val chooserRequest =
        ChooserRequest(
            targetIntent = targetIntent,
            replacementExtras = null,
            launchedFromPackage = "com.example.referrer",
        )
    private val mockSharedPreferences = mock<SharedPreferences>()

    private val userHandle = UserHandle.of(0)
    private val launchedFromUid = 12345
    private val mockComparator = FakeComparator(mockContext, targetIntent, listOf(userHandle))

    private fun createResolvedComponentInfo(
        packageName: String,
        className: String,
    ): ResolvedComponentInfo {
        val componentName = ComponentName(packageName, className)
        val resolveInfo =
            ResolveInfo().apply {
                activityInfo =
                    ActivityInfo().apply {
                        this.packageName = packageName
                        this.name = className
                    }
            }
        return ResolvedComponentInfo(componentName, targetIntent, resolveInfo)
    }

    private fun createController(
        chooserRequest: ChooserRequest,
        rankedGroupSize: Int,
        chooserTitle: CharSequence? = null,
    ): ChooserListController {
        return ChooserListController(
            mockContext,
            mockPackageManager,
            chooserRequest,
            chooserTitle,
            launchedFromUid,
            mockComparator,
            userHandle,
            mockSharedPreferences,
            rankedGroupSize,
        )
    }

    @Test
    fun sort_elementsLessThanRankGroupSize_sortsAll() {
        val rankGroupSize = 5
        val testSubject = createController(chooserRequest, rankGroupSize)

        val components =
            mutableListOf(
                createResolvedComponentInfo("c.package", "ClassC"),
                createResolvedComponentInfo("a.package", "ClassA"),
                createResolvedComponentInfo("b.package", "ClassB"),
            )

        testSubject.sort(components)

        assertThat(components.map { it.name.className })
            .containsExactly("ClassA", "ClassB", "ClassC")
            .inOrder()
    }

    @Test
    fun sort_elementsMoreThanRankGroupSize_partialSortsTopK() {
        val rankGroupSize = 2
        val testSubject = createController(chooserRequest, rankGroupSize)

        val components =
            mutableListOf(
                createResolvedComponentInfo("e.package", "ClassE"),
                createResolvedComponentInfo("a.package", "ClassA"),
                createResolvedComponentInfo("d.package", "ClassD"),
                createResolvedComponentInfo("b.package", "ClassB"),
                createResolvedComponentInfo("c.package", "ClassC"),
            )

        testSubject.sort(components)

        assertThat(components).hasSize(5)
        // Only the top rankGroupSize elements are guaranteed to be sorted at the beginning
        assertThat(components.take(rankGroupSize).map { it.name.className })
            .containsExactly("ClassA", "ClassB")
            .inOrder()

        // The rest of the elements are not guaranteed to be in any specific order
        assertThat(components.drop(rankGroupSize).map { it.name.className })
            .containsExactly("ClassC", "ClassD", "ClassE")
    }

    @Test
    fun sort_withEmptyList_doesNothing() {
        val rankGroupSize = 3
        val testSubject = createController(chooserRequest, rankGroupSize)
        val components = mutableListOf<ResolvedComponentInfo>()

        testSubject.sort(components)

        assertThat(components).isEmpty()
    }

    @Test
    fun sort_rankGroupSizeZero_doesNothing() {
        val rankGroupSize = 0
        val testSubject = createController(chooserRequest, rankGroupSize)

        val components =
            mutableListOf(
                createResolvedComponentInfo("e.package", "ClassE"),
                createResolvedComponentInfo("a.package", "ClassA"),
                createResolvedComponentInfo("d.package", "ClassD"),
                createResolvedComponentInfo("b.package", "ClassB"),
                createResolvedComponentInfo("c.package", "ClassC"),
            )
        val originalOrder = components.toList()

        testSubject.sort(components)

        assertThat(components).containsExactlyElementsIn(originalOrder).inOrder()
    }

    @Test
    fun sort_rankGroupSizeNegative_doesNothing() {
        val rankGroupSize = -1
        val testSubject = createController(chooserRequest, rankGroupSize)

        val components =
            mutableListOf(
                createResolvedComponentInfo("e.package", "ClassE"),
                createResolvedComponentInfo("a.package", "ClassA"),
                createResolvedComponentInfo("d.package", "ClassD"),
                createResolvedComponentInfo("b.package", "ClassB"),
                createResolvedComponentInfo("c.package", "ClassC"),
            )
        val originalOrder = components.toList()

        testSubject.sort(components)

        assertThat(components).containsExactlyElementsIn(originalOrder).inOrder()
    }

    fun getReplacementIntent_noReplacement() {
        val controller = createController(chooserRequest, 4)
        val defIntent = Intent(Intent.ACTION_VIEW)
        val activityInfo =
            ActivityInfo().apply {
                packageName = "com.example"
                name = "TestActivity"
            }

        val resultIntent = controller.getReplacementIntent(activityInfo, defIntent)

        assertThat(resultIntent).isSameInstanceAs(defIntent)
    }

    @Test
    fun getReplacementIntent_withReplacementExtras() {
        val targetIntent = Intent(Intent.ACTION_SEND)
        val replacementExtras =
            Bundle().apply {
                putBundle("com.example", Bundle().apply { putString("extra_key", "extra_value") })
            }
        val chooserRequest =
            ChooserRequest(
                targetIntent = targetIntent,
                replacementExtras = replacementExtras,
                launchedFromPackage = "com.example.referrer",
            )
        val controller = createController(chooserRequest, 4)
        val defIntent = Intent(Intent.ACTION_VIEW)
        val activityInfo =
            ActivityInfo().apply {
                packageName = "com.example"
                name = "TestActivity"
            }

        val resultIntent = controller.getReplacementIntent(activityInfo, defIntent)

        assertThat(resultIntent).isNotSameInstanceAs(defIntent)
        assertThat(resultIntent.getStringExtra("extra_key")).isEqualTo("extra_value")
    }

    @Test
    fun getReplacementIntent_forwardingToParent() {
        val chooserTitle = "Test Chooser Title"
        val controller = createController(chooserRequest, 4, chooserTitle)
        val defIntent = Intent(Intent.ACTION_VIEW)
        val activityInfo =
            ActivityInfo().apply {
                packageName = "com.android.intentresolver"
                name = IntentForwarderActivity.FORWARD_INTENT_TO_PARENT
            }

        val resultIntent = controller.getReplacementIntent(activityInfo, defIntent)

        assertThat(resultIntent.action).isEqualTo(Intent.ACTION_CHOOSER)
        assertThat(resultIntent.getCharSequenceExtra(Intent.EXTRA_TITLE).toString())
            .isEqualTo(chooserTitle)
        assertThat(resultIntent.getBooleanExtra(Intent.EXTRA_AUTO_LAUNCH_SINGLE_CHOICE, true))
            .isFalse()
        val wrappedIntent = resultIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertThat(wrappedIntent).isSameInstanceAs(defIntent)
    }

    @Test
    fun getReplacementIntent_forwardingToManagedProfile() {
        val chooserTitle = "Test Chooser Title"
        val controller = createController(chooserRequest, 4, chooserTitle)
        val defIntent = Intent(Intent.ACTION_VIEW)
        val activityInfo =
            ActivityInfo().apply {
                packageName = "com.android.intentresolver"
                name = IntentForwarderActivity.FORWARD_INTENT_TO_MANAGED_PROFILE
            }

        val resultIntent = controller.getReplacementIntent(activityInfo, defIntent)

        assertThat(resultIntent.action).isEqualTo(Intent.ACTION_CHOOSER)
        assertThat(resultIntent.getCharSequenceExtra(Intent.EXTRA_TITLE).toString())
            .isEqualTo(chooserTitle)
        assertThat(resultIntent.getBooleanExtra(Intent.EXTRA_AUTO_LAUNCH_SINGLE_CHOICE, true))
            .isFalse()
        val wrappedIntent = resultIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertThat(wrappedIntent).isSameInstanceAs(defIntent)
    }
}

private class FakeComparator(context: Context, intent: Intent, userSpaceList: List<UserHandle>) :
    AbstractResolverComparator(context, intent, userSpaceList, null) {
    override fun compare(l: ResolveInfo, r: ResolveInfo): Int =
        l.activityInfo.name.compareTo(r.activityInfo.name)

    override fun doCompute(p0: List<ResolvedComponentInfo?>?) {
        afterCompute()
    }

    override fun getScore(p0: TargetInfo?): Float = 0f

    override fun handleResultMessage(message: Message) = Unit
}
