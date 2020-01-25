plugins {
    `java-library`
}

dependencies {
    api(project(":datasource-decorator-spring-boot-autoconfigure"))
    api("net.ttddyy:datasource-proxy:${project.extra["datasourceProxyVersion"]}")
}
