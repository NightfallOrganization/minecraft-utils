plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.shadow)
}

group = "eu.darkcube.system"
version = "1.0.0"

dependencies {
    api(project("data"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            shadow.component(this)
        }
    }
}

allprojects {
    pluginManager.withPlugin("maven-publish") {
        this@allprojects.extensions.getByType<PublishingExtension>().run {
            repositories {
                maven("https://nexus.darkcube.eu/repository/dasbabypixel/") {
                    name = "DasBabyPixel"
                    credentials(PasswordCredentials::class)
                }
            }
        }
    }
}