plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    android()

    val sharedXCFramework = XCFramework()

    ios {
        binaries {
            framework {
                baseName = "shared"
                sharedXCFramework.add(this)
            }
        }
    }

    watchos {
        binaries {
            framework {
                baseName = "shared"
                sharedXCFramework.add(this)
            }
        }
    }
}

android {
    compileSdkVersion(30)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)
    }
}
