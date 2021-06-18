/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.TEST_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.Companion.COMMON_MAIN_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.Companion.COMMON_TEST_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetsContainer
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.KOTLIN_MPP_SUPPORT_MACOS_ARM_HOST_IN_TARGET_SHORTCUTS
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

interface KotlinTargetContainerWithNativeShortcuts : KotlinTargetContainerWithPresetFunctions, KotlinSourceSetContainer {

    private data class DefaultSourceSets(val main: KotlinSourceSet, val test: KotlinSourceSet)

    private fun KotlinNativeTarget.defaultSourceSets(): DefaultSourceSets =
        DefaultSourceSets(
            compilations.getByName(MAIN_COMPILATION_NAME).defaultSourceSet,
            compilations.getByName(TEST_COMPILATION_NAME).defaultSourceSet
        )

    private fun mostCommonSourceSets() = DefaultSourceSets(
        sourceSets.getByName(COMMON_MAIN_SOURCE_SET_NAME),
        sourceSets.getByName(COMMON_TEST_SOURCE_SET_NAME)
    )

    private fun List<KotlinNativeTarget>.defaultSourceSets(): List<DefaultSourceSets> = map { it.defaultSourceSets() }

    private fun createIntermediateSourceSet(
        name: String,
        children: List<KotlinSourceSet>,
        parent: KotlinSourceSet? = null
    ): KotlinSourceSet =
        sourceSets.maybeCreate(name).apply {
            parent?.let { dependsOn(parent) }
            children.forEach {
                it.dependsOn(this)
            }
        }

    private fun createIntermediateSourceSets(
        namePrefix: String,
        children: List<DefaultSourceSets>,
        parent: DefaultSourceSets? = null
    ): DefaultSourceSets {
        val main = createIntermediateSourceSet("${namePrefix}Main", children.map { it.main }, parent?.main)
        val test = createIntermediateSourceSet("${namePrefix}Test", children.map { it.test }, parent?.test)
        return DefaultSourceSets(main, test)
    }

    fun macos(
        namePrefix: String = "macos",
        configure: KotlinNativeTarget.() -> Unit = {}
    ) {
        val targets = listOf(
            macosX64("${namePrefix}X64"),
            macosArm64("${namePrefix}Arm64")
        )

        createIntermediateSourceSets(namePrefix, targets.defaultSourceSets(), mostCommonSourceSets())
        targets.forEach { it.configure() }
    }

    fun macos() = macos("macos") { }
    fun macos(namePrefix: String) = macos(namePrefix) { }
    fun macos(namePrefix: String, configure: Closure<*>) = macos(namePrefix) { ConfigureUtil.configure(configure, this) }
    fun macos(configure: Closure<*>) = macos { ConfigureUtil.configure(configure, this) }

    fun ios(
        namePrefix: String = "ios",
        configure: KotlinNativeTarget.() -> Unit = {}
    ) {
        val targets = listOfNotNull(
            iosArm64("${namePrefix}Arm64"),
            iosX64("${namePrefix}X64"),
            if (isMacosArmHostEnabled("ios")) iosSimulatorArm64("${namePrefix}SimulatorArm64") else null
        )
        createIntermediateSourceSets(namePrefix, targets.defaultSourceSets(), mostCommonSourceSets())
        targets.forEach { it.configure() }
    }

    fun ios() = ios("ios") { }
    fun ios(namePrefix: String) = ios(namePrefix) { }
    fun ios(namePrefix: String, configure: Closure<*>) = ios(namePrefix) { ConfigureUtil.configure(configure, this) }
    fun ios(configure: Closure<*>) = ios { ConfigureUtil.configure(configure, this) }

    fun tvos(
        namePrefix: String = "tvos",
        configure: KotlinNativeTarget.() -> Unit
    ) {
        val targets = listOfNotNull(
            tvosArm64("${namePrefix}Arm64"),
            tvosX64("${namePrefix}X64"),
            if (isMacosArmHostEnabled("tvos")) tvosSimulatorArm64("${namePrefix}SimulatorArm64") else null
        )
        createIntermediateSourceSets(namePrefix, targets.defaultSourceSets(), mostCommonSourceSets())
        targets.forEach { it.configure() }
    }

    fun tvos() = tvos("tvos") { }
    fun tvos(namePrefix: String) = tvos(namePrefix) { }
    fun tvos(namePrefix: String, configure: Closure<*>) = tvos(namePrefix) { ConfigureUtil.configure(configure, this) }
    fun tvos(configure: Closure<*>) = tvos { ConfigureUtil.configure(configure, this) }

    fun watchos(
        namePrefix: String = "watchos",
        configure: KotlinNativeTarget.() -> Unit = {}
    ) {
        val device32 = watchosArm32("${namePrefix}Arm32")
        val device64 = watchosArm64("${namePrefix}Arm64")
        val simulatorX64 = watchosX64("${namePrefix}X64")
        val simulatorArm64 = if (isMacosArmHostEnabled("watchos")) watchosSimulatorArm64("${namePrefix}SimulatorArm64") else null
        val deviceTargets = listOf(device32, device64)

        val deviceSourceSets = createIntermediateSourceSets(
            "${namePrefix}Device",
            deviceTargets.defaultSourceSets()
        )

        createIntermediateSourceSets(
            namePrefix,
            listOfNotNull(deviceSourceSets, simulatorX64.defaultSourceSets(), simulatorArm64?.defaultSourceSets()),
            mostCommonSourceSets()
        )

        listOfNotNull(device32, device64, simulatorX64, simulatorArm64).forEach { it.configure() }
    }

    fun watchos() = watchos("watchos") { }
    fun watchos(namePrefix: String) = watchos(namePrefix) { }
    fun watchos(namePrefix: String, configure: Closure<*>) = watchos(namePrefix) { ConfigureUtil.configure(configure, this) }
    fun watchos(configure: Closure<*>) = watchos { ConfigureUtil.configure(configure, this) }
}

internal val KotlinTargetsContainer.project: Project?
    get() = (this as? KotlinTopLevelExtension)?.project

internal fun KotlinTargetContainerWithNativeShortcuts.isMacosArmHostEnabled(
    shortcutName: String
): Boolean {
    val project = (this as? KotlinTopLevelExtension)?.project ?: return false
    val enabled = PropertiesProvider(project).supportMacosArmHostsInTargetShortcuts
    if (!enabled) {
        project.logger.warn(
            "Target shortcut $shortcutName() does not support Macos Arm Hosts by default\n" +
                    "To migrate, add $KOTLIN_MPP_SUPPORT_MACOS_ARM_HOST_IN_TARGET_SHORTCUTS=true to your gradle.properties"
        )
    }
    return enabled
}