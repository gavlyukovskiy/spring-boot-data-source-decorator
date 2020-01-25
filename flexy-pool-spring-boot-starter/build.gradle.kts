dependencies {
    implementation(project(":datasource-decorator-spring-boot-autoconfigure"))
    implementation("com.vladmihalcea.flexy-pool:flexy-pool-core:${project.extra["flexyPoolVersion"]}")
    implementation("com.vladmihalcea.flexy-pool:flexy-hikaricp:${project.extra["flexyPoolVersion"]}") {
        exclude(group = "com.vladmihalcea.flexy-pool", module = "flexy-dropwizard-metrics")
    }
    implementation("com.vladmihalcea.flexy-pool:flexy-micrometer-metrics:${project.extra["flexyPoolVersion"]}")
}
