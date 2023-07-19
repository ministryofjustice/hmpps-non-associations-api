package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.support.WithMockUser
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationRestrictionType
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PrisonerNonAssociations
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.offendersearch.OffenderSearchPrisoner
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.createNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.offenderSearchPrisoners
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation as NonAssociationJPA

@WithMockUser
class NonAssociationsResourceTest : SqsIntegrationTestBase() {

  @Nested
  inner class `Create a non-association` {

    private val url = "/non-associations"

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
      val request = createNonAssociationRequest(
        firstPrisonerNumber = "A1234BC",
        firstPrisonerReason = NonAssociationReason.VICTIM,
        secondPrisonerNumber = "D5678EF",
        secondPrisonerReason = NonAssociationReason.PERPETRATOR,
        restrictionType = NonAssociationRestrictionType.CELL,
        comment = "They keep fighting",
      )

      // correct role, missing write scope
      webTestClient.post()
        .uri(url)
        .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS")))
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus()
        .isForbidden

      // correct role, missing write scope
      webTestClient.post()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS"),
            scopes = listOf("read"),
          ),
        )
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
            roles = listOf("ROLE_NON_ASSOCIATIONS"),
            scopes = listOf("write"),
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
            roles = listOf("ROLE_NON_ASSOCIATIONS"),
            scopes = listOf("write"),
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
            roles = listOf("ROLE_NON_ASSOCIATIONS"),
            scopes = listOf("write"),
          ),
        )
        .header("Content-Type", "text/plain")
        .bodyValue(jsonString("firstPrisonerNumber" to "A1234BC"))
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `when any of the prisoners can't be found in OffenderSearch responds 404 Not Found`() {
      val foundPrisoner = offenderSearchPrisoners["A1234BC"]!!
      val notFoundPrisonerNumber = "NOT-FOUND-PRISONER"
      val prisonerNumbers = listOf(
        foundPrisoner.prisonerNumber,
        notFoundPrisonerNumber,
      )
      offenderSearchMockServer.stubSearchByPrisonerNumbers(
        prisonerNumbers,
        listOf(foundPrisoner),
      )

      val request: CreateNonAssociationRequest = createNonAssociationRequest(
        firstPrisonerNumber = foundPrisoner.prisonerNumber,
        firstPrisonerReason = NonAssociationReason.VICTIM,
        secondPrisonerNumber = notFoundPrisonerNumber,
        secondPrisonerReason = NonAssociationReason.PERPETRATOR,
        restrictionType = NonAssociationRestrictionType.CELL,
        comment = "They keep fighting",
      )

      webTestClient.post()
        .uri(url)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS"),
            scopes = listOf("write", "read"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").isEqualTo(
          "Could not find the following prisoners: [NOT-FOUND-PRISONER]",
        )
    }

    @Test
    fun `for a valid request creates the non-association`() {
      val firstPrisoner = offenderSearchPrisoners["A1234BC"]!!
      val secondPrisoner = offenderSearchPrisoners["D5678EF"]!!
      val prisonerNumbers = listOf(
        firstPrisoner.prisonerNumber,
        secondPrisoner.prisonerNumber,
      )
      val prisoners = listOf(firstPrisoner, secondPrisoner)
      offenderSearchMockServer.stubSearchByPrisonerNumbers(
        prisonerNumbers,
        prisoners,
      )

      val request: CreateNonAssociationRequest = createNonAssociationRequest(
        firstPrisonerNumber = firstPrisoner.prisonerNumber,
        firstPrisonerReason = NonAssociationReason.VICTIM,
        secondPrisonerNumber = secondPrisoner.prisonerNumber,
        secondPrisonerReason = NonAssociationReason.PERPETRATOR,
        restrictionType = NonAssociationRestrictionType.CELL,
        comment = "They keep fighting",
      )

      val expectedUsername = "A_TEST_USER"
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
          "authorisedBy": "$expectedUsername",
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
            user = expectedUsername,
            roles = listOf("ROLE_NON_ASSOCIATIONS"),
            scopes = listOf("write", "read"),
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
  inner class `Get a non-association` {

    @Test
    fun `without a valid token responds 401 Unauthorized`() {
      webTestClient.get()
        .uri("/non-associations/42")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `without the correct role responds 403 Forbidden`() {
      val existingNonAssociation = createNonAssociation()

      // wrong role
      webTestClient.get()
        .uri("/non-associations/${existingNonAssociation.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_SOMETHING_ELSE")))
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `when the non-association doesn't exist responds 404 Not Found`() {
      webTestClient.get()
        .uri("/non-associations/42")
        .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS")))
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `when the non-association exists returns it`() {
      val existingNonAssociation = createNonAssociation()

      webTestClient.get()
        .uri("/non-associations/${existingNonAssociation.id}")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS"),
          ),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("id").isEqualTo(existingNonAssociation.id)
        .jsonPath("firstPrisonerNumber").isEqualTo(existingNonAssociation.firstPrisonerNumber)
        .jsonPath("firstPrisonerReason").isEqualTo(existingNonAssociation.firstPrisonerReason.toString())
        .jsonPath("secondPrisonerNumber").isEqualTo(existingNonAssociation.secondPrisonerNumber)
        .jsonPath("secondPrisonerReason").isEqualTo(existingNonAssociation.secondPrisonerReason.toString())
        .jsonPath("restrictionType").isEqualTo(existingNonAssociation.restrictionType.toString())
        .jsonPath("comment").isEqualTo(existingNonAssociation.comment)
    }
  }

  @Nested
  inner class `GET non associations for a prisoner` {

    val prisonerNumber = "A1234BC"

    @Test
    fun `without a valid token responds 401 Unauthorized`() {
      ""
      webTestClient.get()
        .uri("/prisoner/$prisonerNumber/non-associations")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `without the correct role responds 403 Forbidden`() {
      webTestClient.get()
        .uri("/prisoner/$prisonerNumber/non-associations")
        .headers(setAuthorisation(roles = listOf("WRONG_ROLE")))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `when any of the prisoners can't be found in OffenderSearch responds 404 Not Found`() {
      val nonAssociation = createNonAssociation()
      offenderSearchMockServer.stubSearchByPrisonerNumbers(
        listOf(nonAssociation.firstPrisonerNumber, nonAssociation.secondPrisonerNumber),
        emptyList(),
      )

      webTestClient.get()
        .uri("/prisoner/$prisonerNumber/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").isEqualTo("Could not find the following prisoners: [${nonAssociation.firstPrisonerNumber}, ${nonAssociation.secondPrisonerNumber}]")
    }

    @Test
    fun `when there a no non-associations for the given prison, returns the prisoner details`() {
      val prisoner = offenderSearchPrisoners[prisonerNumber]!!
      offenderSearchMockServer.stubSearchByPrisonerNumbers(
        listOf(prisonerNumber),
        listOf(prisoner),
      )

      val expectedResponse = jsonString(
        PrisonerNonAssociations(
          prisonerNumber = prisoner.prisonerNumber,
          firstName = prisoner.firstName,
          lastName = prisoner.lastName,
          prisonId = prisoner.prisonId,
          prisonName = prisoner.prisonName,
          cellLocation = prisoner.cellLocation,
          nonAssociations = emptyList(),
        ),
      )

      webTestClient.get()
        .uri("/prisoner/$prisonerNumber/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          expectedResponse,
          true,
        )
    }

    @Test
    fun `by default returns only open non-associations in same prison the given prisoner is in`() {
      // prisoners in MDI
      val firstPrisoner = offenderSearchPrisoners["A1234BC"]!!
      val secondPrisoner = offenderSearchPrisoners["D5678EF"]!!
      val thirdPrisoner = offenderSearchPrisoners["G9012HI"]!!
      // prisoner in another prison
      val fourthPrisoner = offenderSearchPrisoners["L3456MN"]!!

      // open non-association, same prison
      val openNonAssociation = createNonAssociation(
        firstPrisoner.prisonerNumber,
        secondPrisoner.prisonerNumber,
        isClosed = false,
      )

      // closed non-association, same prison, not returned
      val closedNonAssociation = createNonAssociation(
        firstPrisonerNumber = secondPrisoner.prisonerNumber,
        secondPrisonerNumber = thirdPrisoner.prisonerNumber,
        isClosed = true,
      )

      // non-association with someone in a different prison, not returned
      val otherPrisonNonAssociation = createNonAssociation(
        firstPrisonerNumber = fourthPrisoner.prisonerNumber,
        secondPrisonerNumber = secondPrisoner.prisonerNumber,
      )

      val prisoners = listOf(firstPrisoner, secondPrisoner, thirdPrisoner, fourthPrisoner)
      offenderSearchMockServer.stubSearchByPrisonerNumbers(
        prisonerNumbers = prisoners.map(OffenderSearchPrisoner::prisonerNumber),
        prisoners,
      )

      // NOTE: Non-associations for the 2nd prisoner
      val url = "/prisoner/${secondPrisoner.prisonerNumber}/non-associations"
      webTestClient.get()
        .uri(url)
        .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          // language=json
          """
            {
              "prisonerNumber": "${secondPrisoner.prisonerNumber}",
              "firstName": "${secondPrisoner.firstName}",
              "lastName": "${secondPrisoner.lastName}",
              "prisonId": "${secondPrisoner.prisonId}",
              "prisonName": "${secondPrisoner.prisonName}",
              "cellLocation": "${secondPrisoner.cellLocation}",
              "nonAssociations": [
                {
                  "reasonCode": "${openNonAssociation.secondPrisonerReason}",
                  "reasonDescription": "${openNonAssociation.secondPrisonerReason.description}",
                  "restrictionTypeCode": "${openNonAssociation.restrictionType}",
                  "restrictionTypeDescription": "${openNonAssociation.restrictionType.description}",
                  "comment": "${openNonAssociation.comment}",
                  "authorisedBy": "${openNonAssociation.authorisedBy}",
                  "isClosed": false,
                  "closedReason": null,
                  "closedBy": null,
                  "closedAt": null,
                  "otherPrisonerDetails": {
                    "prisonerNumber": "${firstPrisoner.prisonerNumber}",
                    "reasonCode": "${openNonAssociation.firstPrisonerReason}",
                    "reasonDescription": "${openNonAssociation.firstPrisonerReason.description}",
                    "firstName": "${firstPrisoner.firstName}",
                    "lastName": "${firstPrisoner.lastName}",
                    "prisonId": "${firstPrisoner.prisonId}",
                    "prisonName": "${firstPrisoner.prisonName}",
                    "cellLocation": "${firstPrisoner.cellLocation}"
                  }
                }
              ]
            }
          """,
          false,
        )
    }

    @Test
    fun `optionally returns closed non-associations`() {
      // prisoners in MDI
      val firstPrisoner = offenderSearchPrisoners["A1234BC"]!!
      val secondPrisoner = offenderSearchPrisoners["D5678EF"]!!
      val thirdPrisoner = offenderSearchPrisoners["G9012HI"]!!
      // prisoner in another prison
      val fourthPrisoner = offenderSearchPrisoners["L3456MN"]!!

      // open non-association, same prison
      val openNonAssociation = createNonAssociation(
        firstPrisoner.prisonerNumber,
        secondPrisoner.prisonerNumber,
        isClosed = false,
      )

      // closed non-association, same prison, not returned
      val closedNonAssociation = createNonAssociation(
        firstPrisonerNumber = secondPrisoner.prisonerNumber,
        secondPrisonerNumber = thirdPrisoner.prisonerNumber,
        isClosed = true,
      )

      // non-association with someone in a different prison, not returned
      val otherPrisonNonAssociation = createNonAssociation(
        firstPrisonerNumber = fourthPrisoner.prisonerNumber,
        secondPrisonerNumber = secondPrisoner.prisonerNumber,
      )

      val prisoners = listOf(firstPrisoner, secondPrisoner, thirdPrisoner, fourthPrisoner)
      offenderSearchMockServer.stubSearchByPrisonerNumbers(
        prisonerNumbers = prisoners.map(OffenderSearchPrisoner::prisonerNumber),
        prisoners,
      )

      // NOTE: Non-associations for the 2nd prisoner
      val url = "/prisoner/${secondPrisoner.prisonerNumber}/non-associations?includeClosed=true"
      webTestClient.get()
        .uri(url)
        .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          // language=json
          """
            {
              "prisonerNumber": "${secondPrisoner.prisonerNumber}",
              "firstName": "${secondPrisoner.firstName}",
              "lastName": "${secondPrisoner.lastName}",
              "prisonId": "${secondPrisoner.prisonId}",
              "prisonName": "${secondPrisoner.prisonName}",
              "cellLocation": "${secondPrisoner.cellLocation}",
              "nonAssociations": [
                {
                  "reasonCode": "${openNonAssociation.secondPrisonerReason}",
                  "reasonDescription": "${openNonAssociation.secondPrisonerReason.description}",
                  "restrictionTypeCode": "${openNonAssociation.restrictionType}",
                  "restrictionTypeDescription": "${openNonAssociation.restrictionType.description}",
                  "comment": "${openNonAssociation.comment}",
                  "authorisedBy": "${openNonAssociation.authorisedBy}",
                  "isClosed": false,
                  "closedReason": null,
                  "closedBy": null,
                  "closedAt": null,
                  "otherPrisonerDetails": {
                    "prisonerNumber": "${firstPrisoner.prisonerNumber}",
                    "reasonCode": "${openNonAssociation.firstPrisonerReason}",
                    "reasonDescription": "${openNonAssociation.firstPrisonerReason.description}",
                    "firstName": "${firstPrisoner.firstName}",
                    "lastName": "${firstPrisoner.lastName}",
                    "prisonId": "${firstPrisoner.prisonId}",
                    "prisonName": "${firstPrisoner.prisonName}",
                    "cellLocation": "${firstPrisoner.cellLocation}"
                  }
                },
                {
                  "reasonCode": "${closedNonAssociation.firstPrisonerReason}",
                  "reasonDescription": "${closedNonAssociation.firstPrisonerReason.description}",
                  "restrictionTypeCode": "${closedNonAssociation.restrictionType}",
                  "restrictionTypeDescription": "${closedNonAssociation.restrictionType.description}",
                  "comment": "${closedNonAssociation.comment}",
                  "authorisedBy": "${closedNonAssociation.authorisedBy}",
                  "isClosed": true,
                  "closedReason": "They're friends now",
                  "closedBy": "CLOSE_USER",
                  "otherPrisonerDetails": {
                    "prisonerNumber": "${thirdPrisoner.prisonerNumber}",
                    "reasonCode": "${closedNonAssociation.secondPrisonerReason}",
                    "reasonDescription": "${closedNonAssociation.secondPrisonerReason.description}",
                    "firstName": "${thirdPrisoner.firstName}",
                    "lastName": "${thirdPrisoner.lastName}",
                    "prisonId": "${thirdPrisoner.prisonId}",
                    "prisonName": "${thirdPrisoner.prisonName}",
                    "cellLocation": "${thirdPrisoner.cellLocation}"
                  }
                }
              ]
            }
          """,
          false,
        )
    }
  }

  private fun createNonAssociation(
    firstPrisonerNumber: String = "A1234BC",
    secondPrisonerNumber: String = "D5678EF",
    isClosed: Boolean = false,
  ): NonAssociationJPA {
    val nonna = NonAssociationJPA(
      firstPrisonerNumber = firstPrisonerNumber,
      firstPrisonerReason = NonAssociationReason.VICTIM,
      secondPrisonerNumber = secondPrisonerNumber,
      secondPrisonerReason = NonAssociationReason.PERPETRATOR,
      restrictionType = NonAssociationRestrictionType.CELL,
      comment = "They keep fighting",
      authorisedBy = "USER_1",
    )

    if (isClosed) {
      nonna.isClosed = true
      nonna.closedReason = "They're friends now"
      nonna.closedBy = "CLOSE_USER"
      nonna.closedAt = LocalDateTime.now()
    }

    return repository.save(nonna)
  }
}
