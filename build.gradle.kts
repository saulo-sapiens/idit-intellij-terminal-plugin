plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Target the unified 2025.2 distribution
        intellijIdea("2025.2")

        // This is the "magic line" that adds TerminalToolWindowManager to javac
        bundledPlugin("org.jetbrains.plugins.terminal")

        pluginVerifier()
    }
}

intellijPlatform {
    pluginConfiguration {
        id.set("com.idit.intellij.terminal.bridge")
        name.set("IDIT Terminal Bridge")
        ideaVersion {
            sinceBuild.set("252") // Explicitly target 2025.2+
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}