plugins {
    // add toolchain resolver
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "NetWatch"

include(":core")
include(":paper")
