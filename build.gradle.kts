plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "com.idit"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Target 2024.3 Community as the base
        intellijIdeaCommunity("2024.3")

        instrumentationTools()

        // Include the Terminal plugin dependencies
        bundledPlugin("org.jetbrains.plugins.terminal")

        pluginVerifier()
    }
}

intellijPlatform {
    pluginConfiguration {
        id.set("com.idit.intellij.terminal.bridge")
        name.set("IDIT Terminal Bridge")

        ideaVersion {
            sinceBuild.set("243")
            untilBuild.set("251.*")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}