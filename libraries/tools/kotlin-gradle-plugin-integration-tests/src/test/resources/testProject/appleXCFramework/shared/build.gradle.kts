plugins {
    kotlin("multiplatform")
}

kotlin {
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
