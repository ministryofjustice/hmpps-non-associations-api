import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import uk.gov.justice.digital.hmpps.gradle.PortForwardRDSTask
import uk.gov.justice.digital.hmpps.gradle.PortForwardRedisTask
import uk.gov.justice.digital.hmpps.gradle.RevealSecretsTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "9.1.4"
  kotlin("plugin.jpa") version "2.2.21"
  kotlin("plugin.spring") version "2.2.21"
  idea
  id("org.springdoc.openapi-gradle-plugin") version "1.9.0"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.8.1")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-validation")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.6.1")
  implementation("uk.gov.justice.service.hmpps:hmpps-digital-prison-reporting-lib:9.7.7")

  implementation("io.opentelemetry:opentelemetry-api:1.56.0")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.21.0")

  implementation("com.vladmihalcea:hibernate-types-60:2.21.1")
  implementation("org.hibernate.orm:hibernate-community-dialects:7.1.7.Final")

  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14")

  implementation("com.zaxxer:HikariCP:7.0.2")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")
  implementation("com.pauldijou:jwt-core_2.11:5.0.0")

  developmentOnly("org.springframework.boot:spring-boot-devtools")

  testImplementation("org.wiremock:wiremock-standalone:3.13.1")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.8.1")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.35")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("org.testcontainers:localstack:1.21.3")
  testImplementation("org.testcontainers:postgresql:1.21.3")
}

kotlin {
  jvmToolchain(25)
}

java {
  sourceCompatibility = JavaVersion.VERSION_24
  targetCompatibility = JavaVersion.VERSION_24
}

tasks {
  register<PortForwardRDSTask>("portForwardRDS") {
    namespacePrefix = "hmpps-non-associations"
  }

  register<PortForwardRedisTask>("portForwardRedis") {
    namespacePrefix = "hmpps-non-associations"
  }

  register<RevealSecretsTask>("revealSecrets") {
    namespacePrefix = "hmpps-non-associations"
  }

  withType<KotlinCompile> {
    compilerOptions.jvmTarget = JvmTarget.JVM_24
  }
}

openApi {
  customBootRun.args.set(listOf("--spring.profiles.active=dev,localstack"))
}
