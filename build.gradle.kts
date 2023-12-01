plugins {
    java
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin").version("1.3.0")
}

group = "com.github.gavlyukovskiy"
version = "0.1.0-SNAPSHOT"

val sonatypeUser: String? = project.properties["sonatype_user"]?.toString()
    ?: System.getenv("SONATYPE_USER")
val sonatypePassword: String? = project.properties["sonatype_password"]?.toString()
    ?: System.getenv("SONATYPE_PASSWORD")
val gpgKey: String? = project.properties["gpg_key_path"]?.toString()?.let { File(it).readText() }
    ?: System.getenv("GPG_KEY")
val gpgPassphrase: String? = project.properties["gpg_passphrase"] as String?
    ?: System.getenv("GPG_PASSPHRASE")

nexusPublishing {
    repositories {
        sonatype {
            username.set(sonatypeUser)
            password.set(sonatypePassword)
        }
    }
}

subprojects {
    apply(plugin = "java")

    extra["springBootVersion"] = "3.0.2"
    extra["p6SpyVersion"] = "3.9.0"
    extra["datasourceProxyVersion"] = "1.9"
    extra["flexyPoolVersion"] = "2.2.3"

    extra["release"] = listOf(
        "datasource-decorator-spring-boot-autoconfigure",
        "datasource-proxy-spring-boot-starter",
        "flexy-pool-spring-boot-starter",
        "p6spy-spring-boot-starter"
    ).contains(project.name)

    java.sourceCompatibility = JavaVersion.VERSION_17

    group = rootProject.group

    repositories {
        mavenCentral()
    }

    if (extra["release"] as Boolean) {
        apply(plugin = "maven-publish")
        apply(plugin = "signing")

        val sourceJar by tasks.registering(Jar::class) {
            archiveClassifier.set("sources")
            from(sourceSets["main"].allSource)
        }

        val javadocJar by tasks.registering(Jar::class) {
            archiveClassifier.set("javadoc")
            from(tasks["javadoc"])
        }

        tasks.withType<Jar> {
            from(rootProject.projectDir) {
                include("LICENSE.txt")
                into("META-INF")
            }
        }

        publishing {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                    artifact(sourceJar.get())
                    artifact(javadocJar.get())

                    if (version.toString() == "0.1.0-SNAPSHOT") {
                        throw IllegalStateException("'-Pversion' must be set")
                    }

                    pom {
                        name.set(project.name)
                        description.set("Spring Boot integration with p6spy, datasource-proxy and flexy-pool")
                        url.set("https://github.com/gavlyukovskiy/spring-boot-data-source-decorator")
                        licenses {
                            license {
                                name.set("The Apache Software License, Version 2.0")
                                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }
                        developers {
                            developer {
                                id.set("gavlyukovskiy")
                                name.set("Arthur Gavlyukovskiy")
                                email.set("agavlyukovskiy@gmail.com")
                            }
                        }
                        scm {
                            url.set("https://github.com/gavlyukovskiy/spring-boot-data-source-decorator")
                        }
                        issueManagement {
                            url.set("https://github.com/gavlyukovskiy/spring-boot-data-source-decorator/issues")
                        }
                    }
                }
            }
        }

        signing {
            isRequired = gpgKey != null
            useInMemoryPgpKeys(gpgKey, gpgPassphrase)
            sign(*publishing.publications.toTypedArray())
        }
    }

    if (project.name.contains("sample")) {
        tasks.compileJava {
            dependsOn(":datasource-decorator-spring-boot-autoconfigure:publishToMavenLocal")
            dependsOn(":datasource-proxy-spring-boot-starter:publishToMavenLocal")
            dependsOn(":flexy-pool-spring-boot-starter:publishToMavenLocal")
            dependsOn(":p6spy-spring-boot-starter:publishToMavenLocal")
        }
    }
}
