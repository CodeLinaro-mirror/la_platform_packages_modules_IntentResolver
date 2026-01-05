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

package com.android.intentresolver.ui

import android.app.Activity
import android.graphics.Insets
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.android.intentresolver.R
import com.android.intentresolver.profiles.ChooserMultiProfilePagerAdapter
import com.android.intentresolver.widget.ResolverDrawerLayout
import com.android.internal.widget.RecyclerView.LayoutParams
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class DrawerUiModeController @Inject constructor(private val activity: Activity) {
    private val bottomsheetMode: BottomsheetMode = BottomsheetMode()
    private val dialogMode: DialogMode = DialogMode()
    private var mode: UiMode? = null
    private val drawer by lazy {
        activity.requireViewById<ResolverDrawerLayout>(com.android.internal.R.id.contentPanel)
    }

    fun switchToBottomsheet() {
        if (mode !== bottomsheetMode) {
            mode = bottomsheetMode
            bottomsheetMode.configure(drawer)
        }
    }

    fun switchToDialog() {
        if (mode !== dialogMode) {
            mode = dialogMode
            dialogMode.configure(drawer)
        }
    }

    fun applyInsets(pagerAdapter: ChooserMultiProfilePagerAdapter, insets: Insets) {
        mode?.appyInsets(drawer, pagerAdapter, insets)
    }

    private sealed class UiMode {
        abstract fun configure(drawer: ResolverDrawerLayout)

        abstract fun appyInsets(
            drawer: ResolverDrawerLayout,
            pagerAdapter: ChooserMultiProfilePagerAdapter,
            insets: Insets,
        )

        protected fun ResolverDrawerLayout.setViewsVisibility(
            dialogBottomDecor: Boolean,
            dragHandle: Boolean,
            exitButton: Boolean,
        ) {
            findViewById<View>(R.id.dialog_bottom_decor)?.let { it.isVisible = dialogBottomDecor }
            findViewById<View>(R.id.drag)?.let { it.isVisible = dragHandle }
            findViewById<View>(R.id.exit_button)?.let { it.isVisible = exitButton }
        }
    }

    private class BottomsheetMode : UiMode() {
        override fun configure(drawer: ResolverDrawerLayout) {
            drawer.showAtTop = false
            drawer.setViewsVisibility(
                dialogBottomDecor = false,
                dragHandle = true,
                exitButton =
                    drawer.context
                        ?.resources
                        ?.getInteger(R.integer.preview_close_button_visibility) == View.VISIBLE,
            )
            (drawer.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
                lp.height = LayoutParams.MATCH_PARENT
                lp.gravity = Gravity.NO_GRAVITY
                drawer.layoutParams = lp
            }
        }

        override fun appyInsets(
            drawer: ResolverDrawerLayout,
            pagerAdapter: ChooserMultiProfilePagerAdapter,
            insets: Insets,
        ) {
            drawer.setPadding(insets.left, insets.top, insets.right, 0)
            // Need extra padding so the list can fully scroll up
            // To accommodate for window insets
            pagerAdapter.setFooterHeightInEveryAdapter(insets.bottom)
            (drawer.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
                lp.topMargin = 0
                lp.bottomMargin = 0
            }
        }
    }

    private class DialogMode : UiMode() {
        override fun configure(drawer: ResolverDrawerLayout) {
            drawer.showAtTop = true
            drawer.setViewsVisibility(
                dialogBottomDecor = true,
                dragHandle = false,
                exitButton = true,
            )
            (drawer.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
                lp.height = LayoutParams.WRAP_CONTENT
                lp.gravity = Gravity.CENTER
                drawer.layoutParams = lp
            }
        }

        override fun appyInsets(
            drawer: ResolverDrawerLayout,
            pagerAdapter: ChooserMultiProfilePagerAdapter,
            insets: Insets,
        ) {
            drawer.setPadding(0, 0, 0, 0)
            // Need extra padding so the list can fully scroll up
            // To accommodate for window insets
            pagerAdapter.setFooterHeightInEveryAdapter(0)
            (drawer.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
                lp.topMargin = insets.top
                lp.bottomMargin = insets.bottom
            }
        }
    }
}
