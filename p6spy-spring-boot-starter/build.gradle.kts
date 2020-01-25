plugins {
    `java-library`
}

dependencies {
    api(project(":datasource-decorator-spring-boot-autoconfigure"))
    api("p6spy:p6spy:${project.extra["p6SpyVersion"]}")
}
