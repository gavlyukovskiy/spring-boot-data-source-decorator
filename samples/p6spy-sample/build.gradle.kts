plugins {
    id("org.springframework.boot").version("2.4.3")
}

repositories {
    mavenLocal()
}

apply(plugin = "io.spring.dependency-management")

dependencies {
    // get latest version from https://github.com/gavlyukovskiy/spring-boot-data-source-decorator/releases
    implementation("com.github.gavlyukovskiy:p6spy-spring-boot-starter:${project.version}")

    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework.boot:spring-boot-starter-tomcat")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    implementation("org.springframework.cloud:spring-cloud-starter-sleuth:3.0.1")
    implementation("org.springframework.cloud:spring-cloud-sleuth-zipkin:3.0.1")

    implementation("com.h2database:h2")
    implementation("org.apache.commons:commons-io:1.3.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks {
    bootRun {
        doFirst {
            val args = args!!
            if (project.hasProperty("zipkin")) {
                args.add("--spring.zipkin.enabled=true")
                args.add("--spring.sleuth.enabled=true")
            }
            if (project.hasProperty("args")) {
                val userArgs = project.findProperty("args") as String
                userArgs.split(" ").forEach { args.add(it) }
            }
            setArgs(args)
        }
    }

    test {
        useJUnitPlatform()
    }
}
