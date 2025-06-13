/*
 * Copyright 2024 The Android Open Source Project
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

@file:JvmName("ResolverDrawerLayoutExt")

package com.android.intentresolver.widget

import android.graphics.Rect
import android.view.View
import androidx.core.view.isGone

private val defaultViewBoundsInParentProvider: View.(Rect) -> Unit = { rect ->
    rect.set(left, top, right, bottom)
}

@JvmOverloads
fun ResolverDrawerLayout.getVisibleBoundsInWindow(
    outRect: Rect,
    viewBoundsInParentProvider: View.(Rect) -> Unit = defaultViewBoundsInParentProvider,
) {
    outRect.set(0, 0, 0, 0)
    if (!isLaidOut) {
        return
    }
    var minL = Int.MAX_VALUE
    var minT = Int.MAX_VALUE
    var maxR = Int.MIN_VALUE
    var maxB = Int.MIN_VALUE
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        if (child.isGone) continue
        // get each child's position relative to the parent instead of calling `getBoundsInWindow`
        // (as it traverses the view hierarchy up).
        child.viewBoundsInParentProvider(outRect)
        minL = minOf(minL, outRect.left)
        minT = minOf(minT, outRect.top)
        maxR = maxOf(maxR, outRect.right)
        maxB = maxOf(maxB, outRect.bottom)
    }
    getBoundsInWindow(outRect, true)
    outRect.set(
        maxOf(outRect.left, minL + outRect.left),
        maxOf(outRect.top, minT + outRect.top),
        minOf(outRect.right, maxR + outRect.left),
        minOf(outRect.bottom, maxB + outRect.top),
    )
}
