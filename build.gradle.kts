plugins {
    id("java-library")
}

group = "dev.zyklone"
version = "1.0-SNAPSHOT"

subprojects {
    plugins.apply("java")
    plugins.apply("java-library")

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation(platform("org.junit:junit-bom:5.10.0"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.test {
        useJUnitPlatform()
    }
}

fun buildTask(task: String, os: String, arch: String, distName: String) =
    tasks.register<Exec>(task) {
        group = "server"

        environment("GOOS", os)
        environment("GOARCH", arch)

        workingDir(project.layout.projectDirectory.dir("server"))
        val target = project.layout.buildDirectory
            .file("libs/$distName")
            .get().asFile
        commandLine("go", "build", "-o", target.absolutePath)
    }

buildTask("buildServerWinX64", "windows", "amd64", "NetWatch-windows-amd64.exe")
buildTask("buildServerLinuxX64", "linux", "amd64", "NetWatch-linux-amd64")
tasks.register("buildServer") {
    group = "build"
    dependsOn(tasks["buildServerWinX64"], tasks["buildServerLinuxX64"])
}

tasks.register("dist") {
    group = "build"
    dependsOn(
        tasks["buildServer"],
        tasks.build
    )
}
