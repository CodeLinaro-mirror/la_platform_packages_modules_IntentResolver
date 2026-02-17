/*
 * Copyright (C) 2022 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE")

package com.android.intentresolver

import kotlin.DeprecationLevel.ERROR
import kotlin.DeprecationLevel.WARNING
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatcher
import org.mockito.MockSettings
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import org.mockito.stubbing.OngoingStubbing
import org.mockito.stubbing.Stubber

/*
 * Kotlin versions of popular mockito methods that can return null in situations when Kotlin expects
 * a non-null value. Kotlin will throw an IllegalStateException when this takes place ("x must not
 * be null"). To fix this, we can use methods that modify the return type to be nullable. This
 * causes Kotlin to skip the null checks. Cloned from
 * frameworks/base/packages/SystemUI/tests/utils/src/com/android/systemui/util/mockito/KotlinMockitoHelpers.kt
 */

/**
 * Returns Mockito.eq() as nullable type to avoid java.lang.IllegalStateException when null is
 * returned.
 *
 * Generic T is nullable because implicitly bounded by Any?.
 */
@Deprecated(
    "Replace with mockito-kotlin. See http://go/mockito-kotlin",
    ReplaceWith(expression = "eq", imports = ["org.mockito.kotlin.eq"]),
    level = ERROR,
)
inline fun <T> eq(obj: T): T = Mockito.eq<T>(obj) ?: obj

/**
 * Returns Mockito.same() as nullable type to avoid java.lang.IllegalStateException when null is
 * returned.
 *
 * Generic T is nullable because implicitly bounded by Any?.
 */
@Deprecated(
    "Replace with mockito-kotlin. See http://go/mockito-kotlin",
    ReplaceWith(expression = "same(obj)", imports = ["org.mockito.kotlin.same"]),
    level = ERROR,
)
inline fun <T> same(obj: T): T = Mockito.same<T>(obj) ?: obj

/**
 * Returns Mockito.any() as nullable type to avoid java.lang.IllegalStateException when null is
 * returned.
 *
 * Generic T is nullable because implicitly bounded by Any?.
 */
@Deprecated(
    "Replace with mockito-kotlin. See http://go/mockito-kotlin",
    ReplaceWith(expression = "any(type)", imports = ["org.mockito.kotlin.any"]),
    level = WARNING,
)
inline fun <T> any(type: Class<T>): T = Mockito.any<T>(type)

@Deprecated(
    "Replace with mockito-kotlin. See http://go/mockito-kotlin",
    ReplaceWith(expression = "any()", imports = ["org.mockito.kotlin.any"]),
    level = WARNING,
)
inline fun <reified T> any(): T = any(T::class.java)

/**
 * Returns Mockito.argThat() as nullable type to avoid java.lang.IllegalStateException when null is
 * returned.
 *
 * Generic T is nullable because implicitly bounded by Any?.
 */
@Deprecated(
    "Replace with mockito-kotlin. See http://go/mockito-kotlin",
    ReplaceWith(expression = "argThat(matcher)", imports = ["org.mockito.kotlin.argThat"]),
    level = WARNING,
)
inline fun <T> argThat(matcher: ArgumentMatcher<T>): T = Mockito.argThat(matcher)

/**
 * Kotlin type-inferred version of Mockito.nullable()
 *
 * @see org.mockito.kotlin.anyOrNull
 */
@Deprecated(
    "Replace with mockito-kotlin. See http://go/mockito-kotlin",
    ReplaceWith(expression = "anyOrNull()", imports = ["org.mockito.kotlin.anyOrNull"]),
    level = ERROR,
)
inline fun <reified T> nullable(): T? = Mockito.nullable(T::class.java)

/**
 * Returns ArgumentCaptor.capture() as nullable type to avoid java.lang.IllegalStateException when
 * null is returned.
 *
 * Generic T is nullable because implicitly bounded by Any?.
 *
 * @see org.mockito.kotlin.capture
 */
@Deprecated(
    "Replace with mockito-kotlin. See http://go/mockito-kotlin",
    ReplaceWith(expression = "capture(argumentCaptor)", imports = ["org.mockito.kotlin.capture"]),
    level = ERROR,
)
inline fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()

/**
 * Helper function for creating an argumentCaptor in kotlin.
 *
 * Generic T is nullable because implicitly bounded by Any?.
 *
 * @see org.mockito.kotlin.argumentCaptor
 */
@Deprecated(
    "Replace with mockito-kotlin. See http://go/mockito-kotlin",
    ReplaceWith(expression = "argumentCaptor()", imports = ["org.mockito.kotlin.argumentCaptor"]),
    level = ERROR,
)
inline fun <reified T : Any> argumentCaptor(): ArgumentCaptor<T> =
    ArgumentCaptor.forClass(T::class.java)

/**
 * Helper function for creating new mocks, without the need to pass in a [Class] instance.
 *
 * Generic T is nullable because implicitly bounded by Any?.
 *
 * Updated kotlin-mockito usage:
 * ```
 * val value: Widget = mock<> {
 *    on { status } doReturn "OK"
 *    on { buttonPress } doNothing
 *    on { destroy } doAnswer error("Boom!")
 * }
 * ```
 *
 * __Deprecation note__
 *
 * Automatic replacement is not possible due to a change in lambda receiver type to KStubbing<T>
 *
 * @see org.mockito.kotlin.mock
 * @see org.mockito.kotlin.KStubbing.on
 */
@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("Replace with mockito-kotlin. See http://go/mockito-kotlin", level = WARNING)
inline fun <reified T : Any> mock(
    mockSettings: MockSettings = Mockito.withSettings(),
    apply: T.() -> Unit = {},
): T = Mockito.mock(T::class.java, mockSettings).apply(apply)

/**
 * Helper function for stubbing methods without the need to use backticks.
 *
 * Avoid. It is preferable to provide stubbing at creation time using the [mock] lambda argument.
 *
 * @see org.mockito.kotlin.whenever
 */
@Deprecated(
    "Replace with mockito-kotlin. See http://go/mockito-kotlin",
    ReplaceWith(expression = "whenever(methodCall)", imports = ["org.mockito.kotlin.whenever"]),
    level = ERROR,
)
inline fun <T> whenever(methodCall: T): OngoingStubbing<T> = Mockito.`when`(methodCall)

/**
 * Helper function for stubbing methods without the need to use backticks.
 *
 * Avoid. It is preferable to provide stubbing at creation time using the [mock] lambda argument.
 *
 * __Deprecation note__
 *
 * Replace with KStubber<T>.on within [org.mockito.kotlin.mock] { stubbing }
 *
 * @see org.mockito.kotlin.mock
 * @see org.mockito.kotlin.KStubbing.on
 */
@Deprecated(
    "Replace with mockito-kotlin. See http://go/mockito-kotlin",
    ReplaceWith(expression = "whenever(mock)", imports = ["org.mockito.kotlin.whenever"]),
    level = ERROR,
)
inline fun <T> Stubber.whenever(mock: T): T = `when`(mock)

/**
 * @see org.mockito.kotlin.mock
 * @see org.mockito.kotlin.doThrow
 */
@Deprecated("Replace with mockito-kotlin. See http://go/mockito-kotlin", level = ERROR)
val THROWS_EXCEPTION = Answer { error("Unstubbed behavior was accessed.") }
