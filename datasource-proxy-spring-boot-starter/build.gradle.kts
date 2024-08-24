plugins {
    `java-library`
}

dependencies {
    api(project(":datasource-decorator-spring-boot-autoconfigure"))
    api(libs.datasource.proxy)
}
