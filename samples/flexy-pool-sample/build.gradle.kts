plugins {
    id("org.springframework.boot").version("2.2.2.RELEASE")
}

repositories {
    mavenLocal()
}

apply(plugin = "io.spring.dependency-management")

dependencies {
    compile("com.github.gavlyukovskiy:flexy-pool-spring-boot-starter:1.5.8")

    compile("org.springframework.boot:spring-boot")
    compile("org.springframework.boot:spring-boot-starter")
    compile("org.springframework.boot:spring-boot-autoconfigure")
    compile("org.springframework.boot:spring-boot-starter-logging")
    compile("org.springframework.boot:spring-boot-starter-tomcat")
    compile("org.springframework.boot:spring-boot-starter-web")
    compile("org.springframework.boot:spring-boot-starter-jdbc")
    compile("org.springframework.boot:spring-boot-starter-aop")

    compile("com.h2database:h2")
    compile("org.apache.commons:commons-io:1.3.2")

    testCompile("org.springframework.boot:spring-boot-starter-test")
    testCompile("org.junit.jupiter:junit-jupiter-api:5.5.0")
    testCompile("org.junit.jupiter:junit-jupiter-params:5.5.0")
    testCompile("org.junit.jupiter:junit-jupiter-engine:5.5.0")
}

tasks {
    bootRun {
        val args = args!!
        if (project.hasProperty("args")) {
            val userArgs = project.findProperty("args") as String
            userArgs.split(" ").forEach { args.add(it) }
        }
    }

    test {
        useJUnitPlatform()
    }
}