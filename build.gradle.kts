buildscript {
    repositories {
        jcenter()
    }
}

plugins {
    java
    `maven-publish`
    id("com.jfrog.bintray").version("1.8.4")
    id("pl.allegro.tech.build.axion-release").version("1.10.3")
}

scmVersion {
    with(tag) {
        prefix = ""
        versionSeparator = ""
    }
    if (System.getenv("GITHUB_ACTIONS") == "true") {
        with(repository) {
            customPassword = System.getenv("GITHUB_TOKEN");
        }
    }
}

group = "com.github.gavlyukovskiy"
version = scmVersion.version

subprojects {
    apply(plugin = "java")

    extra["springBootVersion"] = "2.3.1.RELEASE"
    extra["p6SpyVersion"] = "3.9.0"
    extra["datasourceProxyVersion"] = "1.7"
    extra["flexyPoolVersion"] = "2.2.1"
    extra["sleuthVersion"] = "2.2.1.RELEASE"

    extra["release"] = listOf(
            "datasource-decorator-spring-boot-autoconfigure",
            "datasource-proxy-spring-boot-starter",
            "flexy-pool-spring-boot-starter",
            "p6spy-spring-boot-starter"
    ).contains(project.name)

    java.sourceCompatibility = JavaVersion.VERSION_1_8

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }

    if (extra["release"] as Boolean) {
        apply(plugin = "maven-publish")
        apply(plugin = "com.jfrog.bintray")

        tasks {
            val bintrayUploadCheck by registering {
                doLast {
                    val errors = ArrayList<String>()
                    if ((project.version as String).contains("SNAPSHOT")) {
                        errors.add("Cannot release SNAPSHOT version")
                    }
                    if (System.getenv("BINTRAY_USER") == null && !project.hasProperty("release.bintray_user")) {
                        errors.add("'BINTRAY_USER' or '-Prelease.bintray_user' must be set")
                    }
                    if (System.getenv("BINTRAY_KEY") == null && !project.hasProperty("release.bintray_key")) {
                        errors.add("'BINTRAY_KEY' or '-Prelease.bintray_key' must be set")
                    }
                    if (System.getenv("GPG_PASSPHRASE") == null && !project.hasProperty("release.gpg_passphrase")) {
                        errors.add("'GPG_PASSPHRASE' or '-Prelease.gpg_passphrase' must be set")
                    }
                    if (System.getenv("SONATYPE_USER") == null && !project.hasProperty("release.sonatype_user")) {
                        errors.add("'SONATYPE_USER' or '-Prelease.sonatype_user' must be set")
                    }
                    if (System.getenv("SONATYPE_PASSWORD") == null && !project.hasProperty("release.sonatype_password")) {
                        errors.add("'SONATYPE_PASSWORD' or '-Prelease.sonatype_password' must be set")
                    }
                    if (errors.isNotEmpty()) {
                        throw IllegalStateException(errors.joinToString("\n"))
                    }
                }
            }

            bintrayUpload {
                dependsOn(bintrayUploadCheck)
            }

            withType<Jar> {
                from(rootProject.projectDir) {
                    include("LICENSE.txt")
                    into("META-INF")
                }
            }
        }

        val sourceJar by tasks.registering(Jar::class) {
            archiveClassifier.set("sources")
            from(sourceSets["main"].allSource)
        }

        val javadocJar by tasks.registering(Jar::class) {
            archiveClassifier.set("javadoc")
            from(tasks["javadoc"])
        }

        publishing {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                    artifact(sourceJar.get())
                    artifact(javadocJar.get())

                    pom {
                        name.set("spring-boot-data-source-decorator")
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
                    }
                }
            }
        }

        bintray {
            user = (project.properties["release.bintray_user"] ?: System.getenv("BINTRAY_USER"))?.toString()
            key = (project.properties["release.bintray_key"] ?: System.getenv("BINTRAY_KEY"))?.toString()
            setPublications("mavenJava")
            publish = true
            with(pkg) {
                repo = project.group.toString()
                name = project.name
                setLicenses("Apache-2.0")
                publicDownloadNumbers = true
                with(version) {
                    name = project.version.toString()
                    vcsTag = project.version.toString()
                    with(gpg) {
                        sign = true
                        passphrase = (project.properties["release.gpg_passphrase"] ?: System.getenv("GPG_PASSPHRASE"))?.toString()
                    }
                    with(mavenCentralSync) {
                        sync = true
                        user = (project.properties["release.sonatype_user"] ?: System.getenv("SONATYPE_USER"))?.toString()
                        password = (project.properties["release.sonatype_password"] ?: System.getenv("SONATYPE_PASSWORD"))?.toString()
                        close = "1"
                    }
                }
            }
        }
    }

    if (project.name.contains("sample")) {
        tasks.build {
            dependsOn(":datasource-decorator-spring-boot-autoconfigure:publishToMavenLocal")
            dependsOn(":datasource-proxy-spring-boot-starter:publishToMavenLocal")
            dependsOn(":flexy-pool-spring-boot-starter:publishToMavenLocal")
            dependsOn(":p6spy-spring-boot-starter:publishToMavenLocal")
        }
    }
}

tasks {
    val releaseCheck by registering {
        doLast {
            val errors = ArrayList<String>()
            if (!project.hasProperty("release.version")) {
                errors.add("'-Prelease.version' must be set")
            }
            if (System.getenv("GITHUB_ACTIONS") != "true") {
                if (!project.hasProperty("release.customUsername")) {
                    errors.add("'-Prelease.customUsername' must be set")
                }
                if (!project.hasProperty("release.customPassword")) {
                    errors.add("'-Prelease.customPassword' must be set")
                }
            }
            if (errors.isNotEmpty()) {
                throw IllegalStateException(errors.joinToString("\n"))
            }
        }
    }

    verifyRelease {
        dependsOn(releaseCheck)
        subprojects.forEach {
            dependsOn(it.tasks.build)
        }
    }

    bintrayUpload {
        enabled = false
    }
}
