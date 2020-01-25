dependencies {
    compile(project(":datasource-decorator-spring-boot-autoconfigure"))
    compile("net.ttddyy:datasource-proxy:${project.extra["datasourceProxyVersion"]}")
}
