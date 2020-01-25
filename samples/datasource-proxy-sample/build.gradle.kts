plugins {
    id("org.springframework.boot").version("2.2.2.RELEASE")
}

repositories {
    mavenLocal()
}

apply(plugin = "io.spring.dependency-management")

dependencies {
    implementation("com.github.gavlyukovskiy:datasource-proxy-spring-boot-starter:1.5.8")

    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework.boot:spring-boot-starter-tomcat")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    implementation("org.springframework.cloud:spring-cloud-sleuth-core:2.1.2.RELEASE")
    implementation("org.springframework.cloud:spring-cloud-sleuth-zipkin:2.1.2.RELEASE")

    implementation("com.h2database:h2")
    implementation("org.apache.commons:commons-io:1.3.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.5.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.5.0")
}

tasks {
    bootRun {
        val args = args!!
        if (project.hasProperty("zipkin")) {
            args.add("--spring.zipkin.enabled=true")
            args.add("--spring.sleuth.enabled=true")
        }
        if (project.hasProperty("args")) {
            val userArgs = project.findProperty("args") as String
            userArgs.split(" ").forEach { args.add(it) }
        }
    }

    test {
        useJUnitPlatform()
    }
}


