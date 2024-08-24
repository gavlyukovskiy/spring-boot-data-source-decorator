plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.test.logger)
}

repositories {
    mavenLocal()
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    testImplementation(platform(libs.spring.boot.dependencies))

    // get latest version from https://github.com/gavlyukovskiy/spring-boot-data-source-decorator/releases
    implementation("com.github.gavlyukovskiy:datasource-proxy-spring-boot-starter:${project.version}")

    implementation(libs.spring.boot)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.jdbc)

    implementation(libs.h2)
    implementation(libs.commons.io)
    implementation(libs.sql.formatter)

    testImplementation(libs.spring.boot.starter.test)
}

tasks {
    test {
        useJUnitPlatform()
    }
}
