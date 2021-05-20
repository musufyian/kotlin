/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.AGPVersion
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.Assume
import org.junit.BeforeClass
import kotlin.test.Test

class XCFrameworkIT : BaseGradleIT() {
    companion object {
        @BeforeClass
        @JvmStatic
        fun assumeItsMac() {
            Assume.assumeTrue(HostManager.hostIsMac)
        }
    }

    override val defaultGradleVersion = GradleVersionRequired.FOR_MPP_SUPPORT
    override fun defaultBuildOptions() = super.defaultBuildOptions().copy(
        androidHome = KtTestUtil.findAndroidSdk(),
        androidGradlePluginVersion = AGPVersion.v3_6_0
    )

    @Test
    fun `assemble XCFramework for all available ios and watchos targets`() {
        with(Project("appleXCFramework")) {
            build("assembleSharedReleaseXCFramework") {
                assertSuccessful()
                assertTasksExecuted(":shared:linkReleaseFrameworkIosArm64")
                assertTasksExecuted(":shared:linkReleaseFrameworkIosX64")
                assertTasksExecuted(":shared:linkReleaseFrameworkWatchosArm32")
                assertTasksExecuted(":shared:linkReleaseFrameworkWatchosArm64")
                assertTasksExecuted(":shared:linkReleaseFrameworkWatchosX64")
                assertTasksExecuted(":shared:assembleSharedReleaseWatchosFatFrameworkForXCFramework")
                assertTasksExecuted(":shared:assembleSharedReleaseXCFramework")
                assertFileExists("/shared/build/XCFrameworks/release/shared/shared.xcframework")
                assertFileExists("/shared/build/XCFrameworks/release/shared/watchos.framework")
                assertFileExists("/shared/build/XCFrameworks/release/shared/watchos.framework.dSYM")
            }

            build("assembleSharedReleaseXCFramework") {
                assertSuccessful()
                assertTasksUpToDate(":shared:linkReleaseFrameworkIosArm64")
                assertTasksUpToDate(":shared:linkReleaseFrameworkIosX64")
                assertTasksUpToDate(":shared:linkReleaseFrameworkWatchosArm32")
                assertTasksUpToDate(":shared:linkReleaseFrameworkWatchosArm64")
                assertTasksUpToDate(":shared:linkReleaseFrameworkWatchosX64")
                assertTasksUpToDate(":shared:assembleSharedReleaseWatchosFatFrameworkForXCFramework")
                assertTasksUpToDate(":shared:assembleSharedReleaseXCFramework")
            }
        }
    }

    @Test
    fun `check there aren't XCFramework tasks without declaration in build script`() {
        with(Project("sharedAppleFramework")) {
            build("tasks") {
                assertSuccessful()
                assertTasksNotRegistered(
                    ":shared:assembleSharedDebugXCFramework",
                    ":shared:assembleSharedReleaseXCFramework",
                    ":shared:assembleSharedXCFramework"
                )
            }
        }
    }
}