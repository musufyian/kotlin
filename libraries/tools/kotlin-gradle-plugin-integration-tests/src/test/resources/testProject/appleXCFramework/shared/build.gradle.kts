import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.add

plugins {
    kotlin("multiplatform")
}

kotlin {
    val sdkXCFramework = XCFramework("sdk", NativeBuildType.DEBUG)
    val otherXCFramework = XCFramework()

    ios {
        binaries {
            framework {
                baseName = "shared"
                sdkXCFramework.add(this)
                otherXCFramework.add(this)
            }
        }
    }

    watchos {
        binaries {
            framework {
                baseName = "shared"
                sdkXCFramework.add(this)
            }
        }
    }
}
