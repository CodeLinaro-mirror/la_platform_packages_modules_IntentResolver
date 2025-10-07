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

import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.service.chooser.Flags.FLAG_INTERACTIVE_CHOOSER
import com.android.intentresolver.widget.ResolverDrawerLayout.calculateCollapsibleHeight
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class ResolverDrawerLayoutTest {
    @get:Rule val flagRule = SetFlagsRule()

    /**
     * Total uncollapsible height is more than the drawer height and enough space left above the
     * drawer.
     */
    @EnableFlags(FLAG_INTERACTIVE_CHOOSER)
    @Test
    fun test_collapsibleHeightConstraintByDrawerHeight() {
        val collapsibleHeight =
            calculateCollapsibleHeight(
                /* viewHeight = */ 100,
                /* paddingTop = */ 0,
                /* reservedCollapsedTopSpace = */ 0,
                /* drawerHeight = */ 80,
                /* alwaysShowHeight = */ 20,
                /* maxCollapsedHeight = */ 10,
                /* collapsibleHeightReserved = */ 60,
            )

        assertThat(collapsibleHeight).isEqualTo(0)
    }

    /**
     * Total uncollapsible height is less than the drawer height and enough space left above the
     * drawer.
     */
    @EnableFlags(FLAG_INTERACTIVE_CHOOSER)
    @Test
    fun test_collapsibleHeightIsNotConstraint() {
        val collapsibleHeight =
            calculateCollapsibleHeight(
                /* viewHeight = */ 100,
                /* paddingTop = */ 10,
                /* reservedCollapsedTopSpace = */ 30,
                /* drawerHeight = */ 80,
                /* alwaysShowHeight = */ 20,
                /* maxCollapsedHeight = */ 10,
                /* collapsibleHeightReserved = */ 20,
            )

        assertThat(collapsibleHeight).isEqualTo(30)
    }

    /**
     * Total uncollapsible height is less than the drawer height and not enough space left above the
     * drawer. Collapsible height is reduced but no more than the collapsibleHeightReserved value.
     */
    @EnableFlags(FLAG_INTERACTIVE_CHOOSER)
    @Test
    fun test_collapsibleHeightConstraintByTopSpace() {
        val collapsibleHeight =
            calculateCollapsibleHeight(
                /* viewHeight = */ 100,
                /* paddingTop = */ 10,
                /* reservedCollapsedTopSpace = */ 30,
                /* drawerHeight = */ 80,
                /* alwaysShowHeight = */ 20,
                /* maxCollapsedHeight = */ 10,
                /* collapsibleHeightReserved = */ 40,
            )

        assertThat(collapsibleHeight).isEqualTo(20)
    }

    /**
     * The space above the drawer left by the minimum uncollapsed height is less than the reserved
     * top space. Collapsible height is reduced by the collapsibleHeightReserved value.
     */
    @EnableFlags(FLAG_INTERACTIVE_CHOOSER)
    @Test
    fun test_reservedTopSpaceConstraintByMinimumUncollapsedHeight() {
        val collapsibleHeight =
            calculateCollapsibleHeight(
                /* viewHeight = */ 60,
                /* paddingTop = */ 10,
                /* reservedCollapsedTopSpace = */ 20,
                /* drawerHeight = */ 60,
                /* alwaysShowHeight = */ 10,
                /* maxCollapsedHeight = */ 30,
                /* collapsibleHeightReserved = */ 10,
            )

        assertThat(collapsibleHeight).isEqualTo(20)
    }
}
