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

/**
 * Captures a contract between [ResolverDrawerLayout] and a nested scrolling child that allows
 * [ResolverDrawerLayout] to determine if the target list is at top at the time of a nested fling
 * event.
 */
interface TargetListScrollStateQuery {
    /** Invoked from [ResolverDrawerLayout.onNestedPreFling] */
    val isAtTop: Boolean
}
