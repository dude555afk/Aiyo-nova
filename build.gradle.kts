plugins {
    id("com.android.application") version "8.11.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
    id("com.google.dagger.hilt.android") version "2.57.1" apply false
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1" apply false
    id("com.nomanr.plugin.lumo") version "1.2.5" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register("installPreCommitHook") {
    doLast {
        val hookFile = file(".git/hooks/pre-commit")
        val scriptFile = file("scripts/git-hooks/pre-commit")
        hookFile.parentFile.mkdirs()
        hookFile.writeText(scriptFile.readText())
        hookFile.setExecutable(true)
    }
}

tasks.named("preBuild") {
    dependsOn(tasks.named("installPreCommitHook"))
}

subprojects {
    plugins.withId("org.jlleitschuh.gradle.ktlint") {
        ktlint {
            reporter("json", "checkstyle")
            disabledRules.set(setOf(
                "no-wildcard-imports",
                "filename",
                "enum-entry-name-case"
            ))
            ignoreFailures.set(false)
        }
    }
}