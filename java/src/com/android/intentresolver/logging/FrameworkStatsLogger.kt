/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.intentresolver.logging

import com.android.internal.util.FrameworkStatsLog

/** A documenting annotation for FrameworkStatsLog methods and their associated UiEvents. */
internal annotation class ForUiEvent(vararg val uiEventId: Int)

/** Isolates the specific method signatures to use for each of the logged UiEvents. */
interface FrameworkStatsLogger {

    @ForUiEvent(FrameworkStatsLog.SHARESHEET_STARTED)
    fun write(
        frameworkEventId: Int,
        appEventId: Int,
        packageName: String?,
        instanceId: Int,
        mimeType: String?,
        numAppProvidedDirectTargets: Int,
        numAppProvidedAppTargets: Int,
        isWorkProfile: Boolean,
        previewType: Int,
        intentType: Int,
        numCustomActions: Int,
        modifyShareActionProvided: Boolean,
        isInteractiveMode: Boolean,
        isTextToggleShown: Boolean,
    ) {
        FrameworkStatsLog.write(
            frameworkEventId,
            appEventId, /* event_id = 1 */
            packageName, /* package_name = 2 */
            instanceId, /* instance_id = 3 */
            mimeType, /* mime_type = 4 */
            numAppProvidedDirectTargets, /* num_app_provided_direct_targets */
            numAppProvidedAppTargets, /* num_app_provided_app_targets */
            isWorkProfile, /* is_workprofile */
            previewType, /* previewType = 8 */
            intentType, /* intentType = 9 */
            numCustomActions, /* num_provided_custom_actions = 10 */
            modifyShareActionProvided, /* modify_share_action_provided = 11 */
            isInteractiveMode, /* is_interactive_mode = 12 */
            isTextToggleShown, /* is_text_toggle_shown = 13 */
        )
    }

    @ForUiEvent(FrameworkStatsLog.RANKING_SELECTED)
    fun write(
        frameworkEventId: Int,
        appEventId: Int,
        packageName: String?,
        instanceId: Int,
        positionPicked: Int,
        isPinned: Boolean,
        includedExtraTextWhenSharingFile: Boolean,
    ) {
        FrameworkStatsLog.write(
            frameworkEventId,
            appEventId, /* event_id = 1 */
            packageName, /* package_name = 2 */
            instanceId, /* instance_id = 3 */
            positionPicked, /* position_picked = 4 */
            isPinned, /* is_pinned = 5 */
            includedExtraTextWhenSharingFile, /* included_extra_text_when_sharing_file = 6 */
        )
    }
}
