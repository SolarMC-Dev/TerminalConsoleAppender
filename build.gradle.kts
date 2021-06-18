import nu.studer.gradle.credentials.domain.CredentialsContainer
import java.net.URI

plugins {
    `java-library`
    `maven-publish`
    signing
    id("nu.studer.credentials") version "2.1"
    id("org.cadixdev.licenser") version "0.6.0"
    id("pl.allegro.tech.build.axion-release") version "1.13.2"
}

scmVersion.tag.prefix = ""

project.version = scmVersion.version

val artifactId = project.name.toLowerCase()
base.archivesBaseName = artifactId

sourceSets.create("java11") {
    java.srcDir("src/main/java11")
    java.srcDir("src/main/java")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.apache.logging.log4j:log4j-core:2.14.1")
    annotationProcessor("org.apache.logging.log4j:log4j-core:2.14.1")

    api("org.jline:jline-reader:3.20.0")

    compileOnly("org.checkerframework:checker-qual:3.13.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "net.minecrell.terminalconsole")
        attributes("Multi-Release" to "true")
    }
    into("META-INF/versions/11") {
        from(sourceSets["java11"].output)
        include("module-info.class")
    }
}

tasks.named<JavaCompile>("compileJava") {
    options.release.set(8)
}

tasks.named<JavaCompile>("compileJava11Java") {
    options.release.set(11)
    options.javaModuleVersion.set(project.version as String)
    classpath = classpath.plus(sourceSets["main"].compileClasspath)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val isSnapshot = version.toString().endsWith("-SNAPSHOT")

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = base.archivesBaseName

            pom {
                val url: String by project
                name(project.name)
                description(project.description!!)
                url(url)

                scm {
                    url(url)
                    connection("scm:git:$url.git")
                    developerConnection.set(connection)
                }

                issueManagement {
                    system("GitHub Issues")
                    url("$url/issues")
                }

                developers {
                    developer {
                        id("minecrell")
                        name("Minecrell")
                        email("minecrell@minecrell.net")
                    }
                }

                licenses {
                    license {
                        name("MIT License")
                        url("https://opensource.org/licenses/MIT")
                        distribution("repo")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            credentials {
                val credentialsContainer = properties["credentials"] as CredentialsContainer
                val repoUser = credentialsContainer.propertyMissing("solarRepoUser") as String
                val repoPass = credentialsContainer.propertyMissing("solarRepoPassword") as String
                setUsername(repoUser)
                setPassword(repoPass)
            }

            name = "solar-repo"
            val base = "https://mvn-repo.solarmc.gg"
            val releasesRepoUrl = "$base/releases"
            val snapshotsRepoUrl = "$base/snapshots"
            val urlString = if (isSnapshot) { snapshotsRepoUrl; } else { releasesRepoUrl; }
            url = URI(urlString)
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

tasks.withType<Sign> {
    onlyIf { false }
}

operator fun Property<String>.invoke(v: String) = set(v)
