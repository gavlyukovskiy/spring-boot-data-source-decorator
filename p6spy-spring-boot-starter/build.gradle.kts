dependencies {
    compile(project(":datasource-decorator-spring-boot-autoconfigure"))
    compile("p6spy:p6spy:${project.extra["p6SpyVersion"]}")
}
