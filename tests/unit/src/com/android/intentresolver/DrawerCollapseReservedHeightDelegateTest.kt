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

package com.android.intentresolver

import android.content.Context
import android.content.res.Resources
import android.graphics.Insets
import android.graphics.Rect
import android.view.LayoutInflater
import androidx.recyclerview.widget.RecyclerView
import com.android.intentresolver.grid.ChooserGridAdapter
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class DrawerCollapseReservedHeightDelegateTest {
    @Test
    fun test_getMinimizedReservedHeightWithNullInsets_insetsIgnored() {
        val testSubject =
            DrawerCollapseReservedHeightDelegate(minimizedReservedHeight = 100) { _, _, _, _ ->
                500
            }

        assertThat(testSubject.getMinimizedReservedHeight(null)).isEqualTo(100)
    }

    @Test
    fun test_getMinimizedReservedHeightInsets_insetsRespected() {
        val insets = Insets.of(Rect(0, 0, 0, 200))
        val testSubject =
            DrawerCollapseReservedHeightDelegate(minimizedReservedHeight = 100) { _, _, _, _ ->
                500
            }

        assertThat(testSubject.getMinimizedReservedHeight(insets)).isEqualTo(300)
    }

    @Test
    fun test_getReservedHeightWhileMinimized_minimizedHeightReturned() {
        val testSubject =
            DrawerCollapseReservedHeightDelegate(minimizedReservedHeight = 100) { _, _, _, _ ->
                500
            }
        testSubject.isMinimized = true

        assertThat(
                testSubject.getReservedHeight(
                    600,
                    mock<RecyclerView>(),
                    createChooserGridAdapter(),
                    null,
                )
            )
            .isEqualTo(100)
    }

    @Test
    fun test_getReservedHeightWhileMaximized_regularHeightReturned() {
        val testSubject =
            DrawerCollapseReservedHeightDelegate(minimizedReservedHeight = 100) { _, _, _, _ ->
                500
            }

        assertThat(testSubject.isMinimized).isFalse()
        assertThat(
                testSubject.getReservedHeight(
                    600,
                    mock<RecyclerView>(),
                    createChooserGridAdapter(),
                    null,
                )
            )
            .isEqualTo(500)
    }

    private fun createChooserGridAdapter(): ChooserGridAdapter {
        val mockResources =
            mock<Resources> {
                on { getDimensionPixelSize(R.dimen.chooser_row_text_option_translate) } doReturn 100
            }
        val context =
            mock<Context> {
                on { resources } doReturn mockResources
                on { getSystemService(Context.LAYOUT_INFLATER_SERVICE) } doReturn
                    mock<LayoutInflater>()
            }
        return ChooserGridAdapter(
            context,
            mock<ChooserGridAdapter.ChooserActivityDelegate>(),
            mock<ChooserListAdapter>(),
            false,
            5,
        )
    }
}
