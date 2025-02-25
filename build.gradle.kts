import java.time.LocalDate
import java.time.format.DateTimeFormatter

group = "com.wisecoders.dbschema"
version = "4.8.3"

plugins {
    `java-library`
    distribution
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.mongodbDriverSync)
    implementation(libs.graal.js)
    implementation(libs.graal.jsScriptEngine)
    implementation(libs.gson)

    testRuntimeOnly(libs.junit.platformLauncher)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = sourceCompatibility
    withJavadocJar()
    withSourcesJar()
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
}

tasks.jar {
    archiveFileName.set("mongojdbc${project.version}.jar")
    manifest {
        attributes(
            "Main-Class" to "com.wisecoders.dbschema.mongodb.JdbcDriver",
            "Class-Path" to configurations.runtimeClasspath.get().files.joinToString(separator = " ") { it.name },
            "Specification-Version" to project.version,
            "Specification-Vendor" to "Wise Coders",
            "Implementation-Vendor-Id" to "dbschema.com",
            "Implementation-Vendor" to "Wise Coders",
            "Implementation-Version" to LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")),
        )
    }
}

distributions {
    main {
        contents {
            from(tasks.jar)
            from(configurations.runtimeClasspath)
            from(project.layout.projectDirectory.dir("lib"))

            into("/")
        }
    }
}

tasks.distZip {
    archiveFileName = "MongoDbJdbcDriver.zip"
}

tasks.distTar {
    enabled = false
}

artifacts {
    archives(tasks.javadoc)
    archives(tasks.named("sourcesJar"))
}

val deleteFromUserHome: TaskProvider<Delete> = tasks.register<Delete>("deleteFromUserHome") {
    delete {
        delete("${System.getProperty("user.home")}/.DbSchema/drivers/MongoDb/")
    }
}

tasks.register<Copy>("copyToUserHome") {
    dependsOn(deleteFromUserHome)
    from(tasks.distZip.get().inputs.files)
    into("${System.getProperty("user.home")}/.DbSchema/drivers/MongoDb/")
}
