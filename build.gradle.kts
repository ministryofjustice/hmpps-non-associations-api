plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.4.1"
  kotlin("plugin.spring") version "1.9.10"
  kotlin("plugin.jpa") version "1.9.10"
  idea
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-validation")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:2.0.1")
  implementation("io.opentelemetry:opentelemetry-api:1.30.0")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:1.29.0")

  implementation("org.flywaydb:flyway-core")
  implementation("com.vladmihalcea:hibernate-types-60:2.21.1")
  implementation("org.hibernate.orm:hibernate-community-dialects:6.3.0.Final")

  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")

  implementation("com.zaxxer:HikariCP:5.0.1")
  runtimeOnly("org.postgresql:postgresql:42.6.0")
  implementation("com.pauldijou:jwt-core_2.11:5.0.0")

  developmentOnly("org.springframework.boot:spring-boot-devtools")

  testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:3.0.1")
  testImplementation("org.testcontainers:postgresql:1.19.0")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.11.5")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.11.5")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.16")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("org.testcontainers:localstack:1.19.0")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(20))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "20"
    }
  }
}
