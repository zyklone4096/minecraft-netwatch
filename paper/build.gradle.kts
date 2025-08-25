plugins {
    // Apply the plugin
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("de.eldoria.plugin-yml.paper") version "0.7.1"
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

    include(project(":core"))
}

paper {
    name = "NetWatch"
    authors = listOf("Zyklone")
    main = "dev.zyklone.netwatch.paper.NetWatchPlugin"
    description = "NetWatch cloud ban system"
    apiVersion = "1.21"
}

tasks.runServer {
    minecraftVersion(project.property("minecraft") as String)
}

tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}

tasks.register<Exec>("downloadVanillaServerFromMirror") {
    group = "run paper"
    val version = project.property("minecraft")

    val dir = File(tasks.runServer.get().runDirectory.get().asFile, "cache")
    doFirst {
        if (!dir.exists())
            dir.mkdirs()
    }

    workingDir(dir)
    commandLine("curl", "-L", "https://bmclapi2.bangbang93.com/version/$version/server", "-o", "mojang_$version.jar")
}

tasks.shadowJar {
    dependencies {
        exclude(dependency("com.google.code.gson:gson"))
    }
}
