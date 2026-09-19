plugins {
    id("java")
}

group = "pl.twojserwer"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Nowy schemat wersjonowania Paper dla MC 26.x: zakres [26.2.build,) sam
    // lapie najnowszy dostepny build.
    compileOnly("io.papermc.paper:paper-api:[26.2.build,)")
}

tasks {
    jar {
        archiveBaseName.set("WoolMixUp")
        archiveVersion.set(project.version.toString())
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    processResources {
        filteringCharset = "UTF-8"
    }
}
