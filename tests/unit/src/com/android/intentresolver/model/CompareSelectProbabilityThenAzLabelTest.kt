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

package com.android.intentresolver.model

import android.content.pm.ResolveInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import java.text.Collator
import java.util.Locale
import kotlin.math.sign
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class CompareSelectProbabilityThenAzLabelTest {
    @Test
    fun testSelectProbabilityIsMoreImportant() {
        val targetApple2 = mock<ResolveInfo>()
        val targetBanana1 = mock<ResolveInfo>()
        val targetCheese3 = mock<ResolveInfo>()

        val comparator =
            makeSelectProbabilityThenAzLabelComparator(
                {
                    when (it) {
                        targetApple2 -> 0.2f
                        targetBanana1 -> 0.1f
                        targetCheese3 -> 0.3f
                        else -> 0.0f
                    }
                },
                {
                    when (it) {
                        targetApple2 -> "Apple"
                        targetBanana1 -> "Banana"
                        targetCheese3 -> "Cheese"
                        else -> ""
                    }
                },
                Collator.getInstance(Locale.US),
            )

        verifyComparator(comparator, listOf(targetCheese3, targetApple2, targetBanana1))
    }

    @Test
    fun testLabelCanBreakTiesInSelectionProbability() {
        val targetApple2 = mock<ResolveInfo>()
        val targetBanana2 = mock<ResolveInfo>()
        val targetCheese2 = mock<ResolveInfo>()

        val comparator =
            makeSelectProbabilityThenAzLabelComparator(
                { 0.2f },
                {
                    when (it) {
                        targetApple2 -> "Apple"
                        targetBanana2 -> "Banana"
                        targetCheese2 -> "Cheese"
                        else -> ""
                    }
                },
                Collator.getInstance(Locale.US),
            )

        verifyComparator(comparator, listOf(targetApple2, targetBanana2, targetCheese2))
    }

    @Test
    fun testLabelCanProvideOrderForNullSelectionProbabilities() {
        val targetApple = mock<ResolveInfo>()
        val targetBanana = mock<ResolveInfo>()
        val targetCheese = mock<ResolveInfo>()

        val comparator =
            makeSelectProbabilityThenAzLabelComparator(
                { null },
                {
                    when (it) {
                        targetApple -> "Apple"
                        targetBanana -> "Banana"
                        targetCheese -> "Cheese"
                        else -> ""
                    }
                },
                Collator.getInstance(Locale.US),
            )

        verifyComparator(comparator, listOf(targetApple, targetBanana, targetCheese))
    }

    @Test
    fun testSortingWithNullSelectionProbabilityMixedIn() {
        val targetApple2 = mock<ResolveInfo>()
        val targetBanana = mock<ResolveInfo>()
        val targetCheese3 = mock<ResolveInfo>()

        val comparator =
            makeSelectProbabilityThenAzLabelComparator(
                {
                    when (it) {
                        targetApple2 -> 0.2f
                        targetCheese3 -> 0.3f
                        else -> null
                    }
                },
                {
                    when (it) {
                        targetApple2 -> "Apple"
                        targetBanana -> "Banana"
                        targetCheese3 -> "Cheese"
                        else -> ""
                    }
                },
                Collator.getInstance(Locale.US),
            )

        // Despite having a label after "Apple" and before "Cheese" the `targetBanana` needs to be
        // partitioned separately, since we already know that `targetCheese` comes before
        // `targetApple2` according to the (higher-priority) selection probability.
        verifyComparator(comparator, listOf(targetCheese3, targetApple2, targetBanana))
    }

    /**
     * Verify that the given [comparator] is consistent with an expected ordering given as
     * [sortedList], and that no pairwise comparisons among elements of that list would violate the
     * contract documented for [Comparator.compare()].
     *
     * Warning: this is a O(n^3) check that could be expensive for large lists (or if the individual
     * comparison operations are expensive).
     */
    fun <T> verifyComparator(comparator: Comparator<T>, sortedList: List<T>) {
        for (i in 0..<sortedList.size - 1) {
            assertWithMessage(
                    "Element @$i(='${sortedList[i]}') <= @${i + 1}(='${sortedList[i + 1]}') in list expected to be sorted"
                )
                .that(comparator.compare(sortedList[i], sortedList[i + 1]).sign)
                .isAtMost(0)
        }
        for (x in 0..<sortedList.size) {
            for (y in 0..<sortedList.size) {
                val compareXy = comparator.compare(sortedList[x], sortedList[y]).sign
                val compareYx = comparator.compare(sortedList[y], sortedList[x]).sign
                var label = "With x@$x(='${sortedList[x]}'), y@$y(='${sortedList[y]}')"
                assertWithMessage("$label: sgn(compare(x, y)) == -sgn(compare, y, x)")
                    .that(compareXy)
                    .isEqualTo(-compareYx)
                for (z in 0..<sortedList.size) {
                    label = "$label, z@$z(='${sortedList[z]}')"
                    val compareXz = comparator.compare(sortedList[x], sortedList[z]).sign
                    val compareYz = comparator.compare(sortedList[y], sortedList[z]).sign
                    when {
                        compareXy > 0 && compareYz > 0 ->
                            assertWithMessage(
                                    "$label: ((compare(x, y)>0) && (compare(y, z)>0)) => compare(x, z)>0"
                                )
                                .that(compareXz)
                                .isGreaterThan(0)
                        compareXy == 0 ->
                            assertWithMessage(
                                    "$label: compare(x,y)==0 => sgn(compare(x, z))=sgn(compare(y, z))"
                                )
                                .that(compareXz)
                                .isEqualTo(compareYz)
                    }
                }
            }
        }
    }
}
