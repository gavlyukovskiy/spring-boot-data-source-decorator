dependencies {
    compile(project(":datasource-decorator-spring-boot-autoconfigure"))
    compile("com.vladmihalcea.flexy-pool:flexy-pool-core:${project.extra["flexyPoolVersion"]}")
    compile("com.vladmihalcea.flexy-pool:flexy-hikaricp:${project.extra["flexyPoolVersion"]}") {
        exclude(group = "com.vladmihalcea.flexy-pool", module = "flexy-dropwizard-metrics")
    }
    compile("com.vladmihalcea.flexy-pool:flexy-micrometer-metrics:${project.extra["flexyPoolVersion"]}")
}
