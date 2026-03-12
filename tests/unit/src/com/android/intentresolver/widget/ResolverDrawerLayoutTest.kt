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
import android.os.Handler
import android.os.Message
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.service.chooser.Flags.FLAG_INTERACTIVE_CHOOSER
import android.view.HandlerActionQueue
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.intentresolver.Flags
import com.android.intentresolver.widget.ResolverDrawerLayout.calculateCollapsibleHeight
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ResolverDrawerLayoutTest {
    @get:Rule val flagRule = SetFlagsRule()

    private lateinit var context: Context
    private lateinit var layout: ResolverDrawerLayout

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        layout = ResolverDrawerLayout(context, null)
    }

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

    @Test
    fun onSaveInstanceState_whenExpandedAndCollapsible_savesOpenState() {
        // Make layout collapsible and expanded.
        layout.setCollapsibleHeightForTest(100)
        layout.setCollapseOffsetForTest(0f)

        val state = layout.onSaveInstanceState() as ResolverDrawerLayout.SavedState

        assertThat(state.open).isTrue()
    }

    @Test
    fun onSaveInstanceState_whenCollapsedAndCollapsible_savesNotOpenState() {
        // Make layout collapsible and collapsed.
        layout.setCollapsibleHeightForTest(100)
        layout.setCollapseOffsetForTest(100f)

        val state = layout.onSaveInstanceState() as ResolverDrawerLayout.SavedState

        assertThat(state.open).isFalse()
    }

    @Test
    fun onSaveInstanceState_whenNotCollapsible_savesNotOpenState() {
        // Make layout not collapsible.
        layout.setCollapsibleHeightForTest(0)
        layout.setCollapseOffsetForTest(0f)

        val state = layout.onSaveInstanceState() as ResolverDrawerLayout.SavedState

        assertThat(state.open).isFalse()
    }

    @Test
    fun onRestoreInstanceState_withSavedState_restoresOpenState() {
        val savedState = ResolverDrawerLayout.SavedState(View.BaseSavedState.EMPTY_STATE)
        savedState.open = true

        layout.onRestoreInstanceState(savedState)

        assertThat(layout.getOpenOnLayoutForTest()).isTrue()
    }

    @Test
    fun onRestoreInstanceState_withNonSavedState_doesNotCrash() {
        // This should not crash and not throw.
        layout.onRestoreInstanceState(View.BaseSavedState.EMPTY_STATE)
    }

    @EnableFlags(FLAG_INTERACTIVE_CHOOSER)
    @Test
    fun onLayout_whenRestoredToExpanded_remainsExpanded() {
        // Add a child to make it collapsible.
        val child = View(context)
        val lp =
            ResolverDrawerLayout.LayoutParams(ResolverDrawerLayout.LayoutParams.MATCH_PARENT, 500)
        layout.addView(child, lp)
        // Set max collapsed height so there is collapsible height.
        layout.setMaxCollapsedHeight(100)

        // Simulate restoring an "open" state.
        val savedState = ResolverDrawerLayout.SavedState(View.BaseSavedState.EMPTY_STATE)
        savedState.open = true
        layout.onRestoreInstanceState(savedState)

        // Trigger layout.
        layout.measure(
            View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
        )
        layout.layout(0, 0, 500, 1000)

        // Assert it's expanded.
        assertThat(layout.isExpanded).isTrue()
    }

    @EnableFlags(FLAG_INTERACTIVE_CHOOSER)
    @Test
    fun onLayout_whenRestoredToCollapsed_remainsCollapsed() {
        // Add a child to make it collapsible.
        val child = View(context)
        val lp =
            ResolverDrawerLayout.LayoutParams(ResolverDrawerLayout.LayoutParams.MATCH_PARENT, 500)
        layout.addView(child, lp)
        // Set max collapsed height so there is collapsible height.
        layout.setMaxCollapsedHeight(100)

        // Simulate restoring a "closed" state.
        val savedState = ResolverDrawerLayout.SavedState(View.BaseSavedState.EMPTY_STATE)
        savedState.open = false
        layout.onRestoreInstanceState(savedState)

        // Trigger layout.
        layout.measure(
            View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
        )
        layout.layout(0, 0, 500, 1000)

        // Assert it's collapsed.
        assertThat(layout.isExpanded).isFalse()
        assertThat(layout.getCollapseOffsetForTest()).isGreaterThan(0f)
    }

    /**
     * Verifies that when the desktopUi flag is enabled and height measure spec is AT_MOST, the
     * layout's height wraps its content.
     */
    @Test
    @EnableFlags(Flags.FLAG_DESKTOP_UI)
    fun onMeasure_withDesktopUi_atMostHeight_setsWrappedHeight() {
        val layout = ResolverDrawerLayout(context)
        val child = View(context)
        child.layoutParams = ResolverDrawerLayout.LayoutParams(100, 100)
        layout.addView(child)
        layout.setPadding(10, 20, 10, 30)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.AT_MOST)

        layout.measure(widthSpec, heightSpec)

        // mHeightUsed will be 100 (child height)
        // paddingTop is 20, paddingBottom is 30
        // expected height = 100 + 20 + 30 = 150
        assertThat(layout.measuredHeight).isEqualTo(150)
    }

    /**
     * Verifies that when the desktopUi flag is enabled and height measure spec is AT_MOST, the
     * layout's height is capped by the available size.
     */
    @Test
    @EnableFlags(Flags.FLAG_DESKTOP_UI)
    fun onMeasure_withDesktopUi_atMostHeight_respectsMaxSize() {
        val layout = ResolverDrawerLayout(context)
        val child = View(context)
        child.layoutParams = ResolverDrawerLayout.LayoutParams(100, 100)
        layout.addView(child)
        layout.setPadding(10, 20, 10, 30)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY)
        // Max height is smaller than required height
        val heightSpec = View.MeasureSpec.makeMeasureSpec(120, View.MeasureSpec.AT_MOST)

        layout.measure(widthSpec, heightSpec)

        // mHeightUsed is 100, padding is 50. Total needed is 150.
        // heightSize from spec is 120.
        // measuredHeight = min(150, 120) = 120.
        assertThat(layout.measuredHeight).isEqualTo(120)
    }

    /**
     * Verifies that when the desktopUi flag is disabled, the layout uses the full available height
     * when the measure spec is AT_MOST.
     */
    @Test
    @DisableFlags(Flags.FLAG_DESKTOP_UI)
    fun onMeasure_withoutDesktopUi_atMostHeight_usesFullHeight() {
        val layout = ResolverDrawerLayout(context)
        val child = View(context)
        child.layoutParams = ResolverDrawerLayout.LayoutParams(100, 100)
        layout.addView(child)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.AT_MOST)

        layout.measure(widthSpec, heightSpec)

        // Without desktopUi flag, it should take the height from spec.
        assertThat(layout.measuredHeight).isEqualTo(1000)
    }

    /**
     * Verifies that when the desktopUi flag is enabled but height measure spec is EXACTLY, the
     * layout uses the exact height specified.
     */
    @Test
    @EnableFlags(Flags.FLAG_DESKTOP_UI)
    fun onMeasure_withDesktopUi_exactlyHeight_usesExactHeight() {
        val layout = ResolverDrawerLayout(context)
        val child = View(context)
        child.layoutParams = ResolverDrawerLayout.LayoutParams(100, 100)
        layout.addView(child)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY)

        layout.measure(widthSpec, heightSpec)

        // With EXACTLY, it should take the height from spec, regardless of the flag.
        assertThat(layout.measuredHeight).isEqualTo(1000)
    }

    @Test
    @EnableFlags(Flags.FLAG_FIX_RESOLVER_DRAWER_LAYOUT_DISMISS)
    fun dismiss_noScrollingNeeded_callbackGetsInvoked() {
        val onDismissListener = mock<ResolverDrawerLayout.OnDismissedListener>()
        val layout = ResolverDrawerLayout(context)
        val child = View(context)
        child.layoutParams = ResolverDrawerLayout.LayoutParams(100, 100)
        layout.addView(child)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY)

        layout.measure(widthSpec, heightSpec)
        layout.layout(0, 0, layout.measuredWidth, layout.measuredHeight)
        layout.setCollapseOffsetForTest(100f)
        layout.setOnDismissedListener(onDismissListener)

        layout.dismiss()
        assumeTrue(layout.runPending())

        verify(onDismissListener) { 1 * { onDismissed() } }
    }

    private fun ResolverDrawerLayout.setCollapseOffsetForTest(value: Float) {
        val field = ResolverDrawerLayout::class.java.getDeclaredField("mCollapseOffset")
        field.isAccessible = true
        field.set(this, value)
    }

    private fun ResolverDrawerLayout.setCollapsibleHeightForTest(value: Int) {
        val field = ResolverDrawerLayout::class.java.getDeclaredField("mCollapsibleHeight")
        field.isAccessible = true
        field.set(this, value)
    }

    private fun ResolverDrawerLayout.getOpenOnLayoutForTest(): Boolean {
        val field = ResolverDrawerLayout::class.java.getDeclaredField("mOpenOnLayout")
        field.isAccessible = true
        return field.getBoolean(this)
    }

    private fun ResolverDrawerLayout.getCollapseOffsetForTest(): Float {
        val field = ResolverDrawerLayout::class.java.getDeclaredField("mCollapseOffset")
        field.isAccessible = true
        return field.getFloat(this)
    }

    private fun ResolverDrawerLayout.runPending(): Boolean {
        val field =
            runCatching { View::class.java.getDeclaredField("mRunQueue") }.getOrNull()
                ?: return false
        field.isAccessible = true
        val queue = field.get(this) as? HandlerActionQueue ?: return false
        queue.executeActions(
            object : Handler(context.mainLooper) {
                override fun sendMessageAtTime(msg: Message, uptimeMillis: Long): Boolean {
                    msg.callback?.run() ?: return super.sendMessageAtTime(msg, uptimeMillis)
                    return true
                }
            }
        )
        return true
    }
}
