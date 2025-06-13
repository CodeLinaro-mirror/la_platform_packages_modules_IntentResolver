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

package com.android.intentresolver.widget

import android.graphics.Rect
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import com.google.common.truth.Truth
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

class ResolverDrawerLayoutExtTest {
    @Test
    fun test_getVisibleBoundsTest() {
        val viewBounds = HashMap<View, Rect>()
        val views =
            arrayOf(
                mock<View> { on { visibility } doReturn VISIBLE }
                    .also { viewBounds[it] = Rect(110, 100, 190, 200) },
                mock<View> { on { visibility } doReturn INVISIBLE }
                    .also { viewBounds[it] = Rect(100, 200, 200, 300) },
                mock<View> { on { visibility } doReturn VISIBLE }
                    .also { viewBounds[it] = Rect(100, 300, 200, 500) },
                mock<View> { on { visibility } doReturn GONE }
                    .also { viewBounds[it] = Rect(50, 50, 250, 600) },
            )
        val drawer =
            mock<ResolverDrawerLayout> {
                on { childCount } doReturn views.size
                on { getChildAt(any()) } doAnswer
                    { invocation ->
                        views[invocation.arguments[0] as Int]
                    }
                on { getBoundsInWindow(any(), eq(true)) } doAnswer
                    { invocation ->
                        val rect = invocation.arguments[0] as Rect
                        rect.set(50, 50, 350, 450)
                    }
                on { isLaidOut } doReturn true
            }

        val rect = Rect()
        drawer.getVisibleBoundsInWindow(rect) { bounds ->
            bounds.set(requireNotNull(viewBounds[this]))
        }

        Truth.assertThat(rect).isEqualTo(Rect(150, 150, 250, 450))
    }
}
