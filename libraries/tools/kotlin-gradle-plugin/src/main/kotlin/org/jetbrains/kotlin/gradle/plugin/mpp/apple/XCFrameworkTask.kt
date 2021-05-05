/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

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
     * Creates new XCFrameworkConfig and register related tasks for packing.
     * Use XCFrameworkConfig.add(framework) for adding frameworks to result bundle.
     */
    fun Project.XCFramework(
        xcFrameworkName: String = "shared"
    ): XCFrameworkConfig {
        val config = XCFrameworkConfig(xcFrameworkName, mutableSetOf())
        registerPackXCFrameworkTask(config)
        return config
    }
}

private fun Project.registerPackXCFrameworkTask(
    config: XCFrameworkConfig
) {
    NativeBuildType.values().forEach { buildType ->
        registerPackXCFrameworkTask(config, buildType)
    }
}

private fun Project.packXCFrameworkTask(xcFrameworkName: String): TaskProvider<Task> =
    locateOrRegisterTask(lowerCamelCaseName("pack", xcFrameworkName, "XCFramework")) {
        it.group = "build"
        it.description = "Pack all types of '$xcFrameworkName' XCFramework"
    }

private fun Project.registerPackXCFrameworkTask(
    config: XCFrameworkConfig,
    buildType: NativeBuildType
) {
    val xcFrameworkName = config.name
    val buildTypeName = buildType.name.toLowerCaseAsciiOnly()
    val taskName = lowerCamelCaseName("pack", xcFrameworkName, buildTypeName, "XCFramework")

    val outputXCFrameworkFile =
        buildDir
            .resolve("XCFrameworks")
            .resolve(buildTypeName)
            .resolve("$xcFrameworkName.xcframework")

    val deleteTask = registerTask<Delete>(
        lowerCamelCaseName("delete", xcFrameworkName, buildTypeName, "XCFramework")
    ) {
        it.delete.add(outputXCFrameworkFile)
    }

    packXCFrameworkTask(xcFrameworkName).dependsOn(
        registerTask<Task>(taskName) { task ->
            task.group = "build"
            task.description = "Pack $buildTypeName '$xcFrameworkName' XCFramework"

            val typedParts = config.parts.filter { it.buildType == buildType }

            task.onlyIf { typedParts.isNotEmpty() }
            task.dependsOn(deleteTask)
            task.inputs.apply {
                typedParts.forEach { framework ->
                    task.dependsOn(framework.linkTaskName)
                    task.inputs.dir(framework.outputFile)
                }
                property("frameworkName", xcFrameworkName)
                property("buildTypeName", buildTypeName)
            }
            task.outputs.file(outputXCFrameworkFile)

            task.doLast {
                val cmdArgs = mutableListOf("xcodebuild", "-create-xcframework")
                typedParts.forEach { framework ->
                    cmdArgs.add("-framework")
                    cmdArgs.add(framework.outputFile.path)
                }
                cmdArgs.add("-output")
                cmdArgs.add(outputXCFrameworkFile.path)
                exec { it.commandLine(cmdArgs) }
            }
        }
    )
}