dependencies {
    implementation(project(":datasource-decorator-spring-boot-autoconfigure"))
    implementation("p6spy:p6spy:${project.extra["p6SpyVersion"]}")
}
