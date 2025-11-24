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

package com.android.intentresolver.grid

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class AllAppDividerDecorationTest {

    private val paddingVertical = 16
    private val drawableHeight = 2
    private val expectedSpacing = paddingVertical + paddingVertical + drawableHeight
    private val mockDrawable =
        mock<Drawable> {
            on { intrinsicWidth } doReturn 200
            on { intrinsicHeight } doReturn drawableHeight
        }
    private val mockState = mock<RecyclerView.State>()
    private val spanIndex = 1
    private val mockLayoutParams =
        mock<GridLayoutManager.LayoutParams> {
            on { this.spanIndex } doReturn this@AllAppDividerDecorationTest.spanIndex
        }
    private val mockView = mock<View> { on { layoutParams } doReturn mockLayoutParams }
    private val mockAdapter = mock<TestDividerGridAdapter>()
    private val mockRecyclerView =
        mock<RecyclerView> {
            on { adapter } doReturn mockAdapter
            on { getChildAdapterPosition(mockView) } doReturn 1
        }
    private val testSubject = AllAppDividerDecoration(paddingVertical) { mockDrawable }

    @Test
    fun getItemOffsets_noAdapter_noOffset() {
        mockRecyclerView.stub { on { adapter } doReturn null }
        val outRect = Rect()

        testSubject.getItemOffsets(outRect, mockView, mockRecyclerView, mockState)

        assertThat(outRect).isEqualTo(Rect(0, 0, 0, 0))
    }

    @Test
    fun getItemOffsets_notDividerGridAdapter_noOffset() {
        mockRecyclerView.stub { on { adapter } doReturn mock<RecyclerView.Adapter<*>>() }
        val outRect = Rect()

        testSubject.getItemOffsets(outRect, mockView, mockRecyclerView, mockState)

        assertThat(outRect).isEqualTo(Rect(0, 0, 0, 0))
    }

    @Test
    fun getItemOffsets_hasDividerAboveRowFalse_noOffset() {
        val position = 5
        mockRecyclerView.stub { on { getChildAdapterPosition(mockView) } doReturn position }
        mockAdapter.stub { on { hasDividerAboveRow(position - spanIndex) } doReturn false }
        val outRect = Rect()

        testSubject.getItemOffsets(outRect, mockView, mockRecyclerView, mockState)

        assertThat(outRect).isEqualTo(Rect(0, 0, 0, 0))
    }

    @Test
    fun getItemOffsets_hasDividerAboveRowTrue_addsTopOffset() {
        val position = 5
        mockRecyclerView.stub { on { getChildAdapterPosition(mockView) } doReturn position }
        mockAdapter.stub { on { hasDividerAboveRow(position - spanIndex) } doReturn true }
        val outRect = Rect()

        testSubject.getItemOffsets(outRect, mockView, mockRecyclerView, mockState)

        assertThat(outRect).isEqualTo(Rect(0, expectedSpacing, 0, 0))
    }
}

abstract class TestDividerGridAdapter :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), DividerGridAdapter
