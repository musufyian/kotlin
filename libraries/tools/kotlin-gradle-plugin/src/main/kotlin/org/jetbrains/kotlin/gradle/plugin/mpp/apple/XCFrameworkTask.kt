/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.io.File

class XCFrameworkConfig internal constructor(
    val name: String,
    internal val parts: MutableSet<Framework>
) {
    fun add(framework: Framework) {
        require(framework.konanTarget.family.isAppleFamily) {
            "XCFramework supports Apple frameworks only"
        }
        parts.add(framework)
    }
}

interface XCFrameworkExtension {

    /**
     * Creates new XCFrameworkConfig and register related tasks for assembling.
     * Use XCFrameworkConfig.add(framework) for adding frameworks to result bundle.
     */
    fun Project.XCFramework(
        xcFrameworkName: String = "shared"
    ): XCFrameworkConfig {
        val config = XCFrameworkConfig(xcFrameworkName, mutableSetOf())
        registerAssembleXCFrameworkTask(config)
        return config
    }
}

private fun Project.registerAssembleXCFrameworkTask(
    config: XCFrameworkConfig
) {
    NativeBuildType.values().forEach { buildType ->
        registerAssembleXCFrameworkTask(config, buildType)
    }
}

private fun Project.assembleXCFrameworkTask(xcFrameworkName: String): TaskProvider<Task> =
    locateOrRegisterTask(lowerCamelCaseName("assemble", xcFrameworkName, "XCFramework")) {
        it.group = "build"
        it.description = "Assemble all types of '$xcFrameworkName' XCFramework"
    }

private fun Project.registerAssembleXCFrameworkTask(
    config: XCFrameworkConfig,
    buildType: NativeBuildType
) {
    val xcFrameworkName = config.name
    val buildTypeName = buildType.name.toLowerCaseAsciiOnly()
    val taskName = lowerCamelCaseName("assemble", xcFrameworkName, buildTypeName, "XCFramework")

    val outputDir = buildDir.resolve("XCFrameworks").resolve(buildTypeName).resolve(xcFrameworkName)
    val outputXCFrameworkFile = outputDir.resolve("$xcFrameworkName.xcframework")

    val fatFrameworkTasks = registerFatFrameworkTasks(config, buildType, outputDir)

    assembleXCFrameworkTask(xcFrameworkName).dependsOn(
        registerTask<Task>(taskName) { task ->
            task.group = "build"
            task.description = "Assemble $buildTypeName '$xcFrameworkName' XCFramework"

            val typedParts = config.parts.filter { it.buildType == buildType }

            task.onlyIf { typedParts.isNotEmpty() }
            task.inputs.apply {
                typedParts.forEach { framework ->
                    task.dependsOn(framework.linkTaskName)
                    task.inputs.dir(framework.outputFile)
                }
                property("frameworkName", xcFrameworkName)
                property("buildTypeName", buildTypeName)
            }
            task.outputs.dir(outputXCFrameworkFile)
            task.dependsOn(fatFrameworkTasks)

            task.doLast {
                val preparedFrameworks = selectFatOrRegularFrameworks(typedParts, outputDir)
                createXCFramework(preparedFrameworks, outputXCFrameworkFile)
            }
        }
    )
}

private enum class AppleTarget(
    val targetName: String,
    val targets: List<KonanTarget>
) {
    MACOS_DEVICE("macos", listOf(KonanTarget.MACOS_X64, KonanTarget.MACOS_ARM64)),
    IPHONE_DEVICE("ios", listOf(KonanTarget.IOS_ARM32, KonanTarget.IOS_ARM64)),
    IPHONE_SIMULATOR("iosSimulator", listOf(KonanTarget.IOS_X64, KonanTarget.IOS_SIMULATOR_ARM64)),
    WATCHOS_DEVICE("watchos", listOf(KonanTarget.WATCHOS_ARM32, KonanTarget.WATCHOS_ARM64)),
    WATCHOS_SIMULATOR("watchosSimulator", listOf(KonanTarget.WATCHOS_X64, KonanTarget.WATCHOS_SIMULATOR_ARM64)),
    TVOS_DEVICE("tvos", listOf(KonanTarget.TVOS_ARM64)),
    TVOS_SIMULATOR("tvosSimulator", listOf(KonanTarget.TVOS_X64, KonanTarget.TVOS_SIMULATOR_ARM64))
}

//see: https://developer.apple.com/forums/thread/666335
private fun Project.registerFatFrameworkTasks(
    config: XCFrameworkConfig,
    buildType: NativeBuildType,
    workDir: File
): List<TaskProvider<FatFrameworkTask>> {
    val xcFrameworkName = config.name
    val buildTypeName = buildType.name.toLowerCaseAsciiOnly()

    return AppleTarget.values().map { appleTarget ->
        val taskName = lowerCamelCaseName(
            "assemble",
            xcFrameworkName,
            buildTypeName,
            appleTarget.targetName,
            "FatFrameworkForXCFramework"
        )
        registerTask<FatFrameworkTask>(taskName) { task ->
            val frameworks = config.parts.filter { it.buildType == buildType && it.konanTarget in appleTarget.targets }
            if (frameworks.size < 2) return@registerTask
            task.from(frameworks)
            task.destinationDir = workDir
            task.baseName = appleTarget.targetName
        }
    }
}

private fun selectFatOrRegularFrameworks(frameworks: List<Framework>, workDir: File): List<File> =
    AppleTarget.values().mapNotNull { appleTarget ->
        val group = frameworks.filter { it.konanTarget in appleTarget.targets }
        when {
            group.size == 1 -> group.first().outputFile
            group.size > 1 -> workDir.resolve(appleTarget.targetName + ".framework")
            else -> null
        }
    }

private fun Project.createXCFramework(frameworks: List<File>, output: File) {
    if (output.exists()) output.deleteRecursively()

    val cmdArgs = mutableListOf("xcodebuild", "-create-xcframework")
    frameworks.forEach { framework ->
        cmdArgs.add("-framework")
        cmdArgs.add(framework.path)
    }
    cmdArgs.add("-output")
    cmdArgs.add(output.path)
    exec { it.commandLine(cmdArgs) }
}