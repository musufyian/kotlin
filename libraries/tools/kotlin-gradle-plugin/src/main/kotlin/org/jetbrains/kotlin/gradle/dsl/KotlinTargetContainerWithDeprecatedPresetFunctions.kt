/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.dsl

import groovy.lang.Closure
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetsContainerWithPresets
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTestsPreset
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName

interface KotlinTargetContainerWithDeprecatedPresetFunctions : KotlinTargetsContainerWithPresets {
    @Deprecated("Use iosSimulatorX64 instead", replaceWith = ReplaceWith("iosSimulatorX64(name, configure)"))
    fun iosX64(
        name: String = "iosX64",
        configure: KotlinNativeTargetWithSimulatorTests.() -> Unit = { }
    ): KotlinNativeTargetWithSimulatorTests =
        configureOrCreate(
            name,
            presets.getByName(KonanTarget.IOS_X64.presetName) as KotlinNativeTargetWithSimulatorTestsPreset,
            configure
        )

    @Deprecated("Use iosSimulatorX64 instead", replaceWith = ReplaceWith("iosSimulatorX64()"))
    fun iosX64() = iosX64("iosX64") { }

    @Deprecated("Use iosSimulatorX64 instead", replaceWith = ReplaceWith("iosSimulatorX64(name)"))
    fun iosX64(name: String) = iosX64(name) { }

    @Deprecated("Use iosSimulatorX64 instead", replaceWith = ReplaceWith("iosSimulatorX64(name, configure)"))
    fun iosX64(name: String, configure: Closure<*>) = iosX64(name) { ConfigureUtil.configure(configure, this) }

    @Deprecated("Use iosSimulatorX64 instead", replaceWith = ReplaceWith("iosSimulatorX64(configure)"))
    fun iosX64(configure: Closure<*>) = iosX64 { ConfigureUtil.configure(configure, this) }

    @Deprecated("Use watchosSimulatorX64 instead", replaceWith = ReplaceWith("watchosSimulatorX64(name, configure)"))
    fun watchosX64(
        name: String = "watchosX64",
        configure: KotlinNativeTargetWithSimulatorTests.() -> Unit = { }
    ): KotlinNativeTargetWithSimulatorTests =
        configureOrCreate(
            name,
            presets.getByName("watchosX64") as KotlinNativeTargetWithSimulatorTestsPreset,
            configure
        )

    @Deprecated("Use watchosSimulatorX64 instead", replaceWith = ReplaceWith("watchosSimulatorX64()"))
    fun watchosX64() = watchosX64("watchosX64") { }

    @Deprecated("Use watchosSimulatorX64 instead", replaceWith = ReplaceWith("watchosSimulatorX64(name)"))
    fun watchosX64(name: String) = watchosX64(name) { }

    @Deprecated("Use watchosSimulatorX64 instead", replaceWith = ReplaceWith("watchosSimulatorX64(name, configure)"))
    fun watchosX64(name: String, configure: Closure<*>) = watchosX64(name) { ConfigureUtil.configure(configure, this) }

    @Deprecated("Use watchosSimulatorX64 instead", replaceWith = ReplaceWith("watchosSimulatorX64(configure)"))
    fun watchosX64(configure: Closure<*>) = watchosX64 { ConfigureUtil.configure(configure, this) }


    @Deprecated("Use tvosSimulatorX64 instead", replaceWith = ReplaceWith("tvosSimulatorX64(name, configure)"))
    fun tvosX64(
        name: String = "tvosX64",
        configure: KotlinNativeTargetWithSimulatorTests.() -> Unit = { }
    ): KotlinNativeTargetWithSimulatorTests =
        configureOrCreate(
            name,
            presets.getByName("tvosX64") as KotlinNativeTargetWithSimulatorTestsPreset,
            configure
        )

    @Deprecated("Use tvosSimulatorX64 instead", replaceWith = ReplaceWith("tvosSimulatorX64()"))
    fun tvosX64() = tvosX64("tvosX64") { }

    @Deprecated("Use tvosSimulatorX64 instead", replaceWith = ReplaceWith("tvosSimulatorX64(name)"))
    fun tvosX64(name: String) = tvosX64(name) { }

    @Deprecated("Use tvosSimulatorX64 instead", replaceWith = ReplaceWith("tvosSimulatorX64(name, configure)"))
    fun tvosX64(name: String, configure: Closure<*>) = tvosX64(name) { ConfigureUtil.configure(configure, this) }

    @Deprecated("Use tvosSimulatorX64 instead", replaceWith = ReplaceWith("tvosSimulatorX64(configure)"))
    fun tvosX64(configure: Closure<*>) = tvosX64 { ConfigureUtil.configure(configure, this) }
}