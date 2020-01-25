plugins {
    `java-library`
}

dependencies {
    api(project(":datasource-decorator-spring-boot-autoconfigure"))
    api("com.vladmihalcea.flexy-pool:flexy-pool-core:${project.extra["flexyPoolVersion"]}")
    api("com.vladmihalcea.flexy-pool:flexy-hikaricp:${project.extra["flexyPoolVersion"]}") {
        exclude(group = "com.vladmihalcea.flexy-pool", module = "flexy-dropwizard-metrics")
    }
    api("com.vladmihalcea.flexy-pool:flexy-micrometer-metrics:${project.extra["flexyPoolVersion"]}")
}
