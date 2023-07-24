package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.support.WithMockUser
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateSyncRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.MigrateRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationRestrictionType
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.UpdateSyncRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service.genNonAssociation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@WithMockUser
class SyncAndMigrateResourceTest : SqsIntegrationTestBase() {

  @Nested
  inner class `Migrate a non-association` {

    private val url = "/migrate"

    @Test
    fun `without a valid token responds 401 Unauthorized`() {
      webTestClient.post()
        .uri(url)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `without the correct role and scope responds 403 Forbidden`() {
      val request = MigrateRequest(
        firstPrisonerNumber = "C7777XX",
        firstPrisonerReason = NonAssociationReason.VICTIM,
        secondPrisonerNumber = "D7777XX",
        secondPrisonerReason = NonAssociationReason.PERPETRATOR,
        restrictionType = NonAssociationRestrictionType.CELL,
        comment = "They keep fighting",
        authorisedBy = "Me",
        active = true,
      )

      // correct role, missing write scope
      webTestClient.post()
        .uri(url)
        .headers(setAuthorisation(roles = listOf("ROLE_DUMMY")))
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `without a valid request body responds 400 Bad Request`() {
      // no request body
      webTestClient.post()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_MIGRATE"),
          ),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isBadRequest

      // unsupported Content-Type
      webTestClient.post()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_MIGRATE"),
          ),
        )
        .header("Content-Type", "text/plain")
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isBadRequest

      // request body missing some fields
      webTestClient.post()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_MIGRATE"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString("firstPrisonerNumber" to "A1234BC"))
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `for a valid request migrates the non-association`() {
      val request = MigrateRequest(
        firstPrisonerNumber = "C7777XX",
        firstPrisonerReason = NonAssociationReason.VICTIM,
        secondPrisonerNumber = "D7777XX",
        secondPrisonerReason = NonAssociationReason.PERPETRATOR,
        restrictionType = NonAssociationRestrictionType.CELL,
        comment = "This is a comment",
        authorisedBy = "Test",
        active = true,
      )

      val expectedResponse =
        // language=json
        """
        {
          "firstPrisonerNumber": "${request.firstPrisonerNumber}",
          "firstPrisonerReason": "${request.firstPrisonerReason}",
          "secondPrisonerNumber": "${request.secondPrisonerNumber}",
          "secondPrisonerReason": "${request.secondPrisonerReason}",
          "restrictionType": "${request.restrictionType}",
          "comment": "${request.comment}",
          "authorisedBy": "${request.authorisedBy}",
          "isClosed": false,
          "closedReason": null,
          "closedBy": null,
          "closedAt": null
        }
        """

      webTestClient.post()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_MIGRATE"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isCreated
        .expectBody().json(expectedResponse, false)
    }
  }

  @Nested
  inner class `Sync a non-association` {

    private val url = "/sync"

    @Test
    fun `without a valid token responds 401 Unauthorized`() {
      webTestClient.post()
        .uri(url)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `without the correct role and scope responds 403 Forbidden`() {
      val request = CreateSyncRequest(
        firstPrisonerNumber = "A7777XX",
        firstPrisonerReason = NonAssociationReason.VICTIM,
        secondPrisonerNumber = "B7777XX",
        secondPrisonerReason = NonAssociationReason.PERPETRATOR,
        restrictionType = NonAssociationRestrictionType.CELL,
        comment = "They keep fighting",
        authorisedBy = "Me",
      )

      // correct role, missing write scope
      webTestClient.post()
        .uri(url)
        .headers(setAuthorisation(roles = listOf("ROLE_DUMMY")))
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `without a valid request body responds 400 Bad Request`() {
      // no request body
      webTestClient.post()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isBadRequest

      // unsupported Content-Type
      webTestClient.post()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "text/plain")
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isBadRequest

      // request body missing some fields
      webTestClient.post()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString("firstPrisonerNumber" to "A1234BC"))
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `creating an non-association`() {
      val request = CreateSyncRequest(
        firstPrisonerNumber = "A7777XX",
        firstPrisonerReason = NonAssociationReason.VICTIM,
        secondPrisonerNumber = "B7777XX",
        secondPrisonerReason = NonAssociationReason.PERPETRATOR,
        restrictionType = NonAssociationRestrictionType.CELL,
        expiryDate = LocalDate.now().minusDays(4),
        active = false,
      )

      val dtFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
      val expectedResponse =
        // language=json
        """
        {
          "firstPrisonerNumber": "${request.firstPrisonerNumber}",
          "firstPrisonerReason": "${request.firstPrisonerReason}",
          "secondPrisonerNumber": "${request.secondPrisonerNumber}",
          "secondPrisonerReason": "${request.secondPrisonerReason}",
          "restrictionType": "${request.restrictionType}",
          "comment": "",
          "authorisedBy": "",
          "isClosed": true,
          "closedReason": "UNDEFINED",
          "closedBy": "$SYSTEM_USERNAME",
          "closedAt": "${request.expiryDate?.atStartOfDay()?.format(dtFormat)}"
        }
        """

      webTestClient.post()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isCreated
        .expectBody().json(expectedResponse, false)
    }

    @Test
    fun `closing a non-association`() {

      val naUpdate = repository.save(
        genNonAssociation(
          firstPrisonerNumber = "C1234AA",
          secondPrisonerNumber = "D1234AA",
          createTime = LocalDateTime.now(),
        ))

      val request = UpdateSyncRequest(
        id = naUpdate.id!!,
        firstPrisonerReason = NonAssociationReason.PERPETRATOR,
        secondPrisonerReason = NonAssociationReason.VICTIM,
        restrictionType = NonAssociationRestrictionType.WING,
        expiryDate = LocalDate.now(),
        active = false,
        comment = "Its ok now",
        authorisedBy = "TEST"
      )

      val dtFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
      val expectedResponse =
        // language=json
        """
        {
          "id": ${request.id},
          "firstPrisonerNumber": "C1234AA",
          "firstPrisonerReason": "${request.firstPrisonerReason}",
          "secondPrisonerNumber": "D1234AA",
          "secondPrisonerReason": "${request.secondPrisonerReason}",
          "restrictionType": "${request.restrictionType}",
          "comment": "${request.comment}",
          "authorisedBy": "${request.authorisedBy}",
          "isClosed": true,
          "closedReason": "UNDEFINED",
          "closedBy": "TEST",
          "closedAt": "${request.expiryDate?.atStartOfDay()?.format(dtFormat)}"
        }
        """

      webTestClient.put()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(expectedResponse, false)

      repository.deleteById(request.id)
    }

    @Test
    fun `re-opening a non-association`() {

      val naUpdate = repository.save(
        genNonAssociation(
          firstPrisonerNumber = "C1234AA",
          secondPrisonerNumber = "D1234AA",
          createTime = LocalDateTime.now(),
          closed = true,
          closedReason = "All fine now",
        ))

      val request = UpdateSyncRequest(
        id = naUpdate.id!!,
        firstPrisonerReason = NonAssociationReason.PERPETRATOR,
        secondPrisonerReason = NonAssociationReason.VICTIM,
        restrictionType = NonAssociationRestrictionType.WING,
        expiryDate = LocalDate.now(),
        active = true,
        comment = "Its kicked off again",
        authorisedBy = "STAFF1"
      )

      val dtFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
      val expectedResponse =
        // language=json
        """
        {
          "id": ${request.id},
          "firstPrisonerNumber": "C1234AA",
          "firstPrisonerReason": "${request.firstPrisonerReason}",
          "secondPrisonerNumber": "D1234AA",
          "secondPrisonerReason": "${request.secondPrisonerReason}",
          "restrictionType": "${request.restrictionType}",
          "comment": "${request.comment}",
          "authorisedBy": "${request.authorisedBy}",
          "isClosed": false,
          "closedReason": null,
          "closedBy": null,
          "closedAt": null
        }
        """

      webTestClient.put()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(expectedResponse, false)

      repository.deleteById(request.id)
    }

    @Test
    fun `deleting a non-association`() {

      val naDelete = repository.save(
        genNonAssociation(
          firstPrisonerNumber = "C1234AA",
          secondPrisonerNumber = "D1234AA",
          createTime = LocalDateTime.now(),
          closedReason = "OK now",
          authBy = "TEST",
        ))


      webTestClient.delete()
        .uri("$url/${naDelete.id}")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC"),
          ),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus().isNoContent

      Assertions.assertThat(repository.findById(naDelete.id!!)).isNotPresent
    }
  }
}
