plugins {
    id("xyz.jpenilla.run-velocity") version "2.3.1"
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")

    include(project(":core"))
}

tasks.runVelocity {
    velocityVersion("3.4.0-SNAPSHOT")
}

tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}

tasks.shadowJar {
    relocate("com.google.gson", "dev.zyklone.netwatch.libs.com.google.gson")
}
