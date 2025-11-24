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

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

/**
 * An [RecyclerView.ItemDecoration] that draws a horizontal divider above rows in a grid layout,
 * based on the [DividerGridAdapter] and a provided [Drawable].
 *
 * @property paddingVertical Vertical padding to apply around the divider.
 * @property drawableProvider A lambda function that provides the [Drawable] to be used as a
 *   divider.
 */
class AllAppDividerDecoration(
    private val paddingVertical: Int,
    private val drawableProvider: () -> Drawable?,
) : RecyclerView.ItemDecoration() {
    private val dividerDrawable by lazy { drawableProvider() }

    /** Controls whether the divider should be drawn. Defaults to `false`. */
    var showDivider: Boolean = false

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        outRect.set(0, 0, 0, 0)
        val adapter = parent.adapter as? DividerGridAdapter ?: return
        val lp = view.layoutParams as? GridLayoutManager.LayoutParams ?: return
        val divider = dividerDrawable ?: return

        val position = parent.getChildAdapterPosition(view)
        if (adapter.hasDividerAboveRow(position - lp.spanIndex)) {
            outRect.top = paddingVertical + paddingVertical + divider.intrinsicHeight
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (!showDivider) return
        val divider = dividerDrawable ?: return
        val adapter = parent.adapter as? DividerGridAdapter ?: return
        for (i in 0 until parent.childCount) {
            val view = parent.getChildAt(i)
            val lp = view.layoutParams as? GridLayoutManager.LayoutParams ?: continue
            if (lp.spanIndex != 0) continue
            val position = parent.getChildAdapterPosition(view)
            if (position == 0) continue
            if (adapter.hasDividerAboveRow(position)) {
                if (view.top > paddingVertical) {
                    val left = ((parent.width - divider.intrinsicWidth) / 2f).roundToInt()
                    val top =
                        view.top - (divider.intrinsicHeight / 2f).roundToInt() - paddingVertical
                    divider.setBounds(
                        left,
                        top,
                        left + divider.intrinsicWidth,
                        top + divider.intrinsicHeight,
                    )
                    divider.draw(c)
                }
                break
            }
        }
    }
}
