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

import android.graphics.Insets
import androidx.recyclerview.widget.RecyclerView
import com.android.intentresolver.grid.ChooserGridAdapter

/**
 * Calculates the collapsible reserved height for a drawer layout, adjusting the calculation based
 * on whether the drawer is in a minimized state.
 *
 * @see com.android.intentresolver.widget.ResolverDrawerLayout.setCollapsibleHeightReserved
 */
class DrawerCollapseReservedHeightDelegate(
    private val minimizedReservedHeight: Int,
    // TODO: move the actual offset calculation logic into this class and unit-test it
    private val reservedHeightCalcLogic: (Int, RecyclerView, ChooserGridAdapter, Insets?) -> Int,
) {
    /** A flag indicating if the drawer is currently in the minimized state. */
    var isMinimized = false

    /** Calculates the reserved height for the minimized state. */
    fun getMinimizedReservedHeight(systemWindowInsets: Insets?): Int {
        return minimizedReservedHeight + (systemWindowInsets?.bottom ?: 0)
    }

    /** Gets the currently applicable reserved height based on the drawer's state. */
    fun getReservedHeight(
        viewHeight: Int,
        recyclerView: RecyclerView,
        gridAdapter: ChooserGridAdapter,
        systemWindowInsets: Insets?,
    ): Int =
        if (isMinimized) getMinimizedReservedHeight(systemWindowInsets)
        else reservedHeightCalcLogic(viewHeight, recyclerView, gridAdapter, systemWindowInsets)
}
