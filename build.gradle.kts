plugins {
    id("java")
    id("io.quarkus") version "3.31.2"
    id("nu.studer.jooq") version "10.1.1"
    id("com.diffplug.spotless") version "8.2.1"
}

apply(from = "jooq.gradle")

group = "com.k12-school"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {

    implementation(platform("io.quarkus:quarkus-bom:3.31.2"))

    // Quarkus dependencies
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-resteasy")
    implementation("io.quarkus:quarkus-resteasy-jackson")
    implementation("io.quarkus:quarkus-jackson")
    implementation("io.quarkus:quarkus-smallrye-jwt")
    implementation("io.quarkus:quarkus-smallrye-jwt-build")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-opentelemetry")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.45.0")
    // OpenTelemetry Logs SDK for log export
    implementation("io.opentelemetry:opentelemetry-sdk-logs:1.45.0")
    // JBoss Log Manager for custom handler
    implementation("org.jboss.logmanager:jboss-logmanager:3.2.0.Final")
    implementation("io.quarkus:quarkus-agroal")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")

    // jOOQ
    implementation("org.jooq:jooq:3.20.11")
    jooqGenerator("org.jooq:jooq-meta")
    jooqGenerator("org.postgresql:postgresql:42.7.9")

    // Password hashing
    implementation("at.favre.lib:bcrypt:0.10.2")

    // JWT (JJWT)
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    implementation("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    // Kryo serialization
    implementation("com.esotericsoftware:kryo:5.6.0")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.2")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.mockito:mockito-core:5.21.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.21.0")
    testImplementation("com.tngtech.archunit:archunit:1.4.1")
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured:5.5.1")
    testCompileOnly("org.projectlombok:lombok:1.18.42")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.42")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

spotless {
    java {
        palantirJavaFormat()
        formatAnnotations()
        importOrder()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
        targetExclude("**/generated/**")
    }
}

// Delete system schema jOOQ code after generation (causes compilation errors)
afterEvaluate {
    tasks.matching { it.name.startsWith("generate") && it.name.contains("Jooq") }.configureEach {
        doLast {
            // Delete system schema directories
            delete(fileTree("build/generated/jooq") {
                include("**/information_schema/**")
                include("**/pg_catalog/**")
            })
            // Fix DefaultCatalog.java to remove system schema references
            val defaultCatalog = file("build/generated/jooq/com/k12/backend/infrastructure/jooq/DefaultCatalog.java")
            if (defaultCatalog.exists()) {
                val lines = defaultCatalog.readLines()
                val filtered = lines.filterNot {
                    it.contains("import com.k12.backend.infrastructure.jooq.information_schema") ||
                    it.contains("import com.k12.backend.infrastructure.jooq.pg_catalog") ||
                    (it.trim().startsWith("InformationSchema.") && !it.contains("//")) ||
                    (it.trim().startsWith("PgCatalog") && !it.contains("//")) ||
                    (it.contains("InformationSchema.INFORMATION_SCHEMA") || it.contains("PgCatalog.PG_CATALOG"))
                }
                val filteredContent = filtered.joinToString("\n")
                // Remove system schemas from getSchemas() method
                val fixedContent = filteredContent.replace(
                    "return Arrays.asList(\n            Public.PUBLIC,\n            InformationSchema.INFORMATION_SCHEMA,\n            PgCatalog.PG_CATALOG\n        );",
                    "return Arrays.asList(\n            Public.PUBLIC\n        );"
                )
                defaultCatalog.writeText(fixedContent)
            }
        }
    }
}

tasks.named("compileJava") {
    dependsOn("spotlessApply")
}

tasks.named("compileTestJava") {
    dependsOn("spotlessApply")
}

// Run spotlessCheck during build
tasks.named("check") {
    dependsOn("spotlessCheck")
}