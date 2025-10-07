plugins {
    `java-library`
}

dependencies {
    api(project(":datasource-decorator-spring-boot-autoconfigure"))
    api(libs.flexy.pool.core)
    api(libs.flexy.pool.hikaricp) {
        exclude(group = "com.vladmihalcea.flexy-pool", module = "flexy-dropwizard-metrics")
    }
    api(libs.flexy.pool.micrometer.metrics)
}
