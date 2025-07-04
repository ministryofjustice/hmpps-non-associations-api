package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration

import io.swagger.v3.parser.OpenAPIV3Parser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@DisplayName("OpenApi docs")
class OpenApiDocsTest : SqsIntegrationTestBase() {
  @Test
  fun `open api docs are available`() {
    webTestClient.get()
      .uri("/swagger-ui/index.html?configUrl=/v3/api-docs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `open api docs redirect to correct page`() {
    webTestClient.get()
      .uri("/swagger-ui.html")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().is3xxRedirection
      .expectHeader().value("Location") {
        assertThat(it).contains("/swagger-ui/index.html")
      }
  }

  @Test
  fun `the swagger json is valid`() {
    webTestClient.get()
      .uri("/v3/api-docs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody().consumeWith {
        val contents = it.responseBody!!.decodeToString()
        val result = OpenAPIV3Parser().readContents(contents)
        assertThat(
          result.messages.filter { message ->
            // TODO: DPR endpoints generate three seemingly-valid "x-no-data-warning" response header definitions,
            //       but swagger parser now fails with this error
            !message.contains("responses.default.headers.x-no-data-warning.style is not of type `simple`")
          },
        ).isEmpty()
        assertThat(result.openAPI.paths).isNotEmpty
      }
  }

  @Test
  fun `the swagger json contains the version number`() {
    webTestClient.get()
      .uri("/v3/api-docs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("info.version").isEqualTo(DateTimeFormatter.ISO_DATE.format(LocalDate.now()))
  }

  @Test
  fun `the security scheme is setup for bearer tokens`() {
    webTestClient.get()
      .uri("/v3/api-docs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.components.securitySchemes.bearer-jwt.type").isEqualTo("http")
      .jsonPath("$.components.securitySchemes.bearer-jwt.scheme").isEqualTo("bearer")
      .jsonPath("$.components.securitySchemes.bearer-jwt.bearerFormat").isEqualTo("JWT")
      .jsonPath("$.security[0].bearer-jwt").isEqualTo(listOf("read", "write"))
  }

  @Test
  fun `the swagger json doesn't include LocalTime`() {
    webTestClient.get()
      .uri("/v3/api-docs")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("components.schemas.LocalTime").doesNotExist()
  }
}
