import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    id("java")
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
}

group = "com.hjk321.bigspender"
version = "1.1-SNAPSHOT"
description = "Allows players to run commands with shorthand money values as arguments."
project.ext["author"] = "hjk321"
project.ext["url"] = "https://hangar.papermc.io/hjk321/BigSpender"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("org.bukkit:bukkit:1.8-R0.1-SNAPSHOT")
}

bukkit {
    main = "com.hjk321.bigspender.BigSpender"
    apiVersion = "1.13"

    permissions {
        register("bigspender.use") {
            description = "BigSpender will process this user's commands."
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
    }
}