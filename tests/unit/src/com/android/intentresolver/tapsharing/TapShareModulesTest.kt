/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.intentresolver.tapsharing

import android.content.ComponentName
import android.app.Activity
import android.provider.Settings.Secure.TAP_EVENT_SERVICE_COMPONENT
import android.provider.Settings.Secure.TAP_SHARE_FULFILLMENT_ACTIVITY_COMPONENT
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.service.chooser.Flags
import com.android.intentresolver.platform.SecureSettings
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

@RunWith(AndroidJUnit4::class)
class TapShareModulesTest {

    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @get:Rule
    val flagsRule = SetFlagsRule()

    @Mock
    private lateinit var mockActivity: Activity
    @Mock
    private lateinit var mockSettings: SecureSettings

    private val testScope: CoroutineScope = TestScope()
    private val testTapEventService = ComponentName("com.test", "com.test.TapEventService")
    private val testFulfillmentActivity = ComponentName("com.test", "com.test.FulfillmentActivity")

    @Test
    fun provideTapEventServiceConnector_returnsConnector() {
        // Arrange & Act
        val connector = TapEventServiceConnector(testTapEventService)

        // Assert
        assertThat(connector).isInstanceOf(TapEventServiceConnector::class.java)
    }

    @Test
    @DisableFlags(Flags.FLAG_TAP_TO_SHARE)
    fun provideTapShareController_flagDisabled_returnsNoOp() {
        // Arrange
        val connector = TapEventServiceConnector(testTapEventService)

        // Act
        val controller = TapShareModule.provideTapShareController(
            mockActivity,
            testTapEventService,
            testFulfillmentActivity,
            testScope,
            connector,
            Dispatchers.Unconfined
        )

        // Assert
        assertThat(controller).isInstanceOf(NoOpTapShareController::class.java)
    }

    @Test
    @EnableFlags(Flags.FLAG_TAP_TO_SHARE)
    fun provideTapShareController_nullTapEventService_returnsNoOp() {
        // Arrange
        val connector = TapEventServiceConnector(null)

        // Act
        val controller = TapShareModule.provideTapShareController(
            mockActivity,
            null,
            testFulfillmentActivity,
            testScope,
            connector,
            Dispatchers.Unconfined
        )

        // Assert
        assertThat(controller).isInstanceOf(NoOpTapShareController::class.java)
    }

    @Test
    @EnableFlags(Flags.FLAG_TAP_TO_SHARE)
    fun provideTapShareController_nullFulfillmentActivity_returnsNoOp() {
        // Arrange
        val connector = TapEventServiceConnector(testTapEventService)

        // Act
        val controller = TapShareModule.provideTapShareController(
            mockActivity,
            testTapEventService,
            null,
            testScope,
            connector,
            Dispatchers.Unconfined
        )

        // Assert
        assertThat(controller).isInstanceOf(NoOpTapShareController::class.java)
    }

    @Test
    @EnableFlags(Flags.FLAG_TAP_TO_SHARE)
    fun provideTapShareController_allConditionsMet_returnsRealController() {
        // Arrange
        val connector = TapEventServiceConnector(testTapEventService)

        // Act
        val controller = TapShareModule.provideTapShareController(
            mockActivity,
            testTapEventService,
            testFulfillmentActivity,
            testScope,
            connector,
            Dispatchers.Unconfined
        )

        // Assert
        assertThat(controller).isInstanceOf(TapShareController::class.java)
    }

    @Test
    fun provideTapEventServiceComponent_fromSettings() {
        `when`(mockSettings.getStringOrNull(TAP_EVENT_SERVICE_COMPONENT))
            .thenReturn(testTapEventService.flattenToString())

        val componentName = TapTargetModule.provideTapEventServiceComponent(mockSettings)

        assertThat(componentName).isEqualTo(testTapEventService)
    }

    @Test
    fun provideTapEventServiceComponent_fromSettings_emptyReturnsNull() {
        `when`(mockSettings.getStringOrNull(TAP_EVENT_SERVICE_COMPONENT)).thenReturn("")

        val componentName = TapTargetModule.provideTapEventServiceComponent(mockSettings)

        assertThat(componentName).isNull()
    }

    @Test
    fun provideTapEventServiceComponent_fromSettings_nullReturnsNull() {
        `when`(mockSettings.getStringOrNull(TAP_EVENT_SERVICE_COMPONENT)).thenReturn(null)

        val componentName = TapTargetModule.provideTapEventServiceComponent(mockSettings)

        assertThat(componentName).isNull()
    }

    @Test
    fun provideTapShareFulfillmentActivityComponent_fromSettings() {
        `when`(mockSettings.getStringOrNull(TAP_SHARE_FULFILLMENT_ACTIVITY_COMPONENT))
            .thenReturn(testFulfillmentActivity.flattenToString())

        val componentName =
            TapTargetModule.provideTapShareFulfillmentActivityComponent(mockSettings)

        assertThat(componentName).isEqualTo(testFulfillmentActivity)
    }

    @Test
    fun provideTapShareFulfillmentActivityComponent_fromSettings_emptyReturnsNull() {
        `when`(mockSettings.getStringOrNull(TAP_SHARE_FULFILLMENT_ACTIVITY_COMPONENT)).thenReturn("")

        val componentName =
            TapTargetModule.provideTapShareFulfillmentActivityComponent(mockSettings)

        assertThat(componentName).isNull()
    }

    @Test
    fun provideTapShareFulfillmentActivityComponent_fromSettings_nullReturnsNull() {
        `when`(mockSettings.getStringOrNull(TAP_SHARE_FULFILLMENT_ACTIVITY_COMPONENT)).thenReturn(null)

        val componentName =
            TapTargetModule.provideTapShareFulfillmentActivityComponent(mockSettings)

        assertThat(componentName).isNull()
    }
}
