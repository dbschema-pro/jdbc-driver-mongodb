plugins {
    alias(libs.plugins.wisecoders.commonGradle.jdbcDriver)
}

group = "com.wisecoders.jdbc-drivers"

jdbcDriver {
    dbId = "MongoDb"
}

dependencies {
    implementation(libs.wisecoders.commonLib.commonSlf4j)
    implementation(libs.wisecoders.commonJdbc.commonJdbcJvm)

    implementation(libs.slf4j.api)
    implementation(libs.mongodbDriverSync)
    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.core)
    implementation(libs.jackson.databind)
    implementation(libs.gson)
    implementation(libs.graal.jsScriptEngine)

    runtimeOnly(libs.logback.classic)
}
