dependencies {
    implementation(project(":datasource-decorator-spring-boot-autoconfigure"))
    implementation("net.ttddyy:datasource-proxy:${project.extra["datasourceProxyVersion"]}")
}
