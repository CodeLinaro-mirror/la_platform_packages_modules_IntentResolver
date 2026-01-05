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

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.android.intentresolver.Flags.useRecyclerViewDecorations
import com.android.intentresolver.R
import com.android.intentresolver.grid.AllAppDividerDecoration
import kotlin.math.roundToInt

class ChooserTargetView : RecyclerView, TargetListScrollStateQuery {
    private val dividerDecoration: AllAppDividerDecoration =
        AllAppDividerDecoration(
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics)
                .roundToInt()
        ) {
            context.getDrawable(R.drawable.chooser_row_layer_list)
        }
    private val footerDecoration: FooterDecoration = FooterDecoration()

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
    ) : super(context, attrs, defStyleAttr)

    init {
        if (useRecyclerViewDecorations()) {
            addItemDecoration(dividerDecoration)
            addItemDecoration(footerDecoration)
        }
    }

    override val isAtTop: Boolean
        get() = !canScrollVertically(-1)

    var showAppDivider: Boolean
        get() = dividerDecoration.showDivider
        set(value) {
            if (dividerDecoration.showDivider != value) {
                dividerDecoration.showDivider = value
                invalidate()
            }
        }

    var footerHeight: Int
        get() = footerDecoration.footerHeight
        set(value) {
            if (footerDecoration.footerHeight != value) {
                footerDecoration.footerHeight = value
                requestLayout()
            }
        }
}

private class FooterDecoration() : RecyclerView.ItemDecoration() {
    var footerHeight: Int = 0

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        outRect.set(0, 0, 0, 0)
        val adapter = parent.adapter ?: return
        val position = parent.getChildAdapterPosition(view)
        if (position == adapter.itemCount - 1) {
            outRect.set(0, 0, 0, footerHeight)
        }
    }
}
