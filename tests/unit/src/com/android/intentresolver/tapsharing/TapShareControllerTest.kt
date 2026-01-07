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

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.UserHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.intentresolver.data.model.ChooserRequest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class TapShareControllerTest {

    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock
    private lateinit var mockActivity: Activity
    @Mock
    private lateinit var mockUserContext: Context
    @Mock
    private lateinit var mockPackageManager: PackageManager
    @Mock
    private lateinit var mockDelegate: TapShareDelegate
    @Mock
    private lateinit var mockConnector: TapEventServiceConnector

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val tapShareFulfillmentActivity =
        ComponentName("com.example", "com.example.FulfillmentActivity")
    private val tapEventServiceComponent =
        ComponentName("com.example", "com.example.TapEventService")
    private val targetIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Hello")
    }
    private val referrer = Uri.parse("android-app://com.example.app")
    private val testRequest = ChooserRequest(
        targetIntent = targetIntent,
        refinementIntentSender = null,
        launchedFromPackage = "com.example.app"
    )
    private val testUser = UserHandle.of(0)
    private val resolveInfo = ResolveInfo().apply {
        activityInfo = ActivityInfo().apply {
            applicationInfo = ApplicationInfo().apply {
                packageName = tapShareFulfillmentActivity.packageName
            }
            packageName = tapShareFulfillmentActivity.packageName
            name = tapShareFulfillmentActivity.className
        }
    }
    private val serviceInfo = ServiceInfo().apply {
        enabled = true
        applicationInfo = ApplicationInfo()
    }

    private lateinit var controller: TapShareController

    @Before
    fun setup() {
        whenever(mockDelegate.request) doReturn testRequest
        whenever(mockDelegate.referrer) doReturn referrer
        whenever(mockActivity.createPackageContextAsUser(any(), eq(0), eq(testUser))) doReturn mockUserContext
        whenever(mockActivity.packageName) doReturn tapShareFulfillmentActivity.packageName
        whenever(mockUserContext.packageManager) doReturn mockPackageManager
        whenever(mockPackageManager.getServiceInfo(eq(tapEventServiceComponent), any<PackageManager.ComponentInfoFlags>())) doReturn serviceInfo

        controller = TapShareController(
            mockActivity,
            tapShareFulfillmentActivity,
            tapEventServiceComponent,
            mockConnector,
            testScope,
            testDispatcher
        )
    }

    @Test
    fun setup_componentNotEnabled_doesNothing() = testScope.runTest {
        // Arrange
        serviceInfo.enabled = false

        // Act
        controller.setup(mockDelegate, testUser)

        // Assert
        verify(mockConnector, never()).awaitNearbyDeviceSelection(any(), any())
    }

    @Test
    fun setup_getServiceInfoFails_doesNothing() = testScope.runTest {
        // Arrange
        whenever(mockPackageManager.getServiceInfo(
            eq(tapEventServiceComponent),
            any<PackageManager.ComponentInfoFlags>()
        )) doThrow PackageManager.NameNotFoundException()

        // Act
        controller.setup(mockDelegate, testUser)

        // Assert
        verify(mockConnector, never()).awaitNearbyDeviceSelection(any(), any())
    }

    @Test
    fun setup_fulfillmentActivityNotResolved_doesNothing() = testScope.runTest {
        // Arrange
        whenever(mockPackageManager.resolveActivity(argThat { intent ->
            intent != null && intent.component == tapShareFulfillmentActivity && !intent.hasCategory(Intent.CATEGORY_DEFAULT)
        }, any<Int>())) doReturn null

        // Act
        controller.setup(mockDelegate, testUser)

        // Assert
        val intentCaptor = argumentCaptor<Intent>()
        verify(mockPackageManager).resolveActivity(intentCaptor.capture(), eq(0))
        val capturedIntent = intentCaptor.firstValue
        assertThat(capturedIntent.component).isEqualTo(tapShareFulfillmentActivity)
        assertThat(capturedIntent.hasCategory(Intent.CATEGORY_DEFAULT)).isFalse()

        verify(mockConnector, never()).awaitNearbyDeviceSelection(any(), any())
        verify(mockDelegate, never()).onNearbyDeviceSelected(any(), any())
    }

    @Test
    fun setup_createContextFails_doesNothing() = testScope.runTest {
        // Arrange
        doThrow(PackageManager.NameNotFoundException())
            .whenever(mockActivity).createPackageContextAsUser(any(), eq(0), eq(testUser))

        // Act
        controller.setup(mockDelegate, testUser)

        // Assert
        verify(mockConnector, never()).awaitNearbyDeviceSelection(any(), any())
    }

    @Test
    fun setup_fulfillmentActivityResolved_awaitsDeviceSelection() = testScope.runTest {
        // Arrange
        whenever(mockConnector.awaitNearbyDeviceSelection(any(), any())) doReturn Unit
        whenever(mockPackageManager.resolveActivity(argThat { intent ->
            intent != null && intent.component == tapShareFulfillmentActivity
        }, eq(0))) doReturn resolveInfo

        // Act
        controller.setup(mockDelegate, testUser)

        // Assert
        verify(mockConnector).awaitNearbyDeviceSelection(eq(referrer), eq(mockUserContext))
    }

    @Test
    fun setup_onNearbyDeviceSelected_finalIntentResolved_callbacks() = testScope.runTest {
        // Arrange
        whenever(mockConnector.awaitNearbyDeviceSelection(any(), any())) doReturn Unit
        whenever(mockPackageManager.resolveActivity(argThat { intent ->
            intent != null && intent.component == tapShareFulfillmentActivity
        }, any<Int>())) doReturn resolveInfo

        // Act
        controller.setup(mockDelegate, testUser)

        // Assert
        val intentCaptor = argumentCaptor<Intent>()
        verify(mockDelegate).onNearbyDeviceSelected(eq(resolveInfo), intentCaptor.capture())
        val capturedIntent = intentCaptor.firstValue
        assertThat(capturedIntent.component).isEqualTo(tapShareFulfillmentActivity)
        assertThat(capturedIntent.action).isEqualTo(Intent.ACTION_SEND)
        assertThat(capturedIntent.type).isEqualTo("text/plain")
        assertThat(capturedIntent.getStringExtra(Intent.EXTRA_TEXT)).isEqualTo("Hello")
    }
}
