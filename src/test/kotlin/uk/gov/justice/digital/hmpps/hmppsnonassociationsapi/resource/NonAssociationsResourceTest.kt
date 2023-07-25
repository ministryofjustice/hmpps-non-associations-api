package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PatchNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PrisonerNonAssociations
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.Reason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.RestrictionType
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.Role
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
        firstPrisonerRole = Role.VICTIM,
        secondPrisonerNumber = "D5678EF",
        secondPrisonerRole = Role.PERPETRATOR,
        reason = Reason.VIOLENCE,
        restrictionType = RestrictionType.CELL,
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

      val request = createNonAssociationRequest(
        firstPrisonerNumber = foundPrisoner.prisonerNumber,
        firstPrisonerRole = Role.VICTIM,
        secondPrisonerNumber = notFoundPrisonerNumber,
        secondPrisonerRole = Role.PERPETRATOR,
        reason = Reason.VIOLENCE,
        restrictionType = RestrictionType.CELL,
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

      val request = createNonAssociationRequest(
        firstPrisonerNumber = firstPrisoner.prisonerNumber,
        firstPrisonerRole = Role.VICTIM,
        secondPrisonerNumber = secondPrisoner.prisonerNumber,
        secondPrisonerRole = Role.PERPETRATOR,
        reason = Reason.VIOLENCE,
        restrictionType = RestrictionType.CELL,
        comment = "They keep fighting",
      )

      // language=text
      val expectedUsername = "A_TEST_USER"
      val expectedResponse =
        // language=json
        """
        {
          "firstPrisonerNumber": "${request.firstPrisonerNumber}",
          "firstPrisonerRole": "${request.firstPrisonerRole}",
          "secondPrisonerNumber": "${request.secondPrisonerNumber}",
          "secondPrisonerRole": "${request.secondPrisonerRole}",
          "reason": "${request.reason}",
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
  inner class `PATCH a non-association` {

    private lateinit var nonAssociation: NonAssociationJPA
    private lateinit var url: String

    @BeforeEach
    fun setUp() {
      nonAssociation = createNonAssociation()
      url = "/non-associations/${nonAssociation.id}"
    }

    @Test
    fun `without a valid token responds 401 Unauthorized`() {
      webTestClient.patch()
        .uri(url)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `without the correct role and scope responds 403 Forbidden`() {
      val request = PatchNonAssociationRequest(
        restrictionType = RestrictionType.LANDING,
      )

      // correct role, missing write scope
      webTestClient.patch()
        .uri(url)
        .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS")))
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus()
        .isForbidden

      // correct role, missing write scope
      webTestClient.patch()
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
      // TODO: How do we check the request body is not empty if all fields optional?
      // no request body
      webTestClient.patch()
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
      webTestClient.patch()
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

      // request body has invalid fields
      webTestClient.patch()
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
    fun `for a valid request updates the non-association`() {
      // language=text
      val updatedComment = "UPDATED comment"
      val request = mapOf(
        "comment" to updatedComment,
      )

      // language=text
      val expectedUsername = "A_TEST_USER"
      val expectedResponse =
        // language=json
        """
        {
          "firstPrisonerNumber": "${nonAssociation.firstPrisonerNumber}",
          "firstPrisonerRole": "${nonAssociation.firstPrisonerRole}",
          "secondPrisonerNumber": "${nonAssociation.secondPrisonerNumber}",
          "secondPrisonerRole": "${nonAssociation.secondPrisonerRole}",
          "reason": "${nonAssociation.reason}",
          "restrictionType": "${nonAssociation.restrictionType}",
          "comment": "$updatedComment",
          "authorisedBy": "${nonAssociation.authorisedBy}",
          "isClosed": false,
          "closedReason": null,
          "closedBy": null,
          "closedAt": null
        }
        """

      webTestClient.patch()
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
        .expectStatus().isOk
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
        .jsonPath("id").isEqualTo(existingNonAssociation.id!!)
        .jsonPath("firstPrisonerNumber").isEqualTo(existingNonAssociation.firstPrisonerNumber)
        .jsonPath("firstPrisonerRole").isEqualTo(existingNonAssociation.firstPrisonerRole.toString())
        .jsonPath("secondPrisonerNumber").isEqualTo(existingNonAssociation.secondPrisonerNumber)
        .jsonPath("secondPrisonerRole").isEqualTo(existingNonAssociation.secondPrisonerRole.toString())
        .jsonPath("restrictionType").isEqualTo(existingNonAssociation.restrictionType.toString())
        .jsonPath("comment").isEqualTo(existingNonAssociation.comment)
    }
  }

  @Nested
  inner class `GET non associations for a prisoner` {

    private val prisonerNumber = "A1234BC"

    @Test
    fun `without a valid token responds 401 Unauthorized`() {
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
        .jsonPath("userMessage")
        .isEqualTo("Could not find the following prisoners: [${nonAssociation.firstPrisonerNumber}, ${nonAssociation.secondPrisonerNumber}]")
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
      val prisonerJohn = offenderSearchPrisoners["A1234BC"]!!
      val prisonerMerlin = offenderSearchPrisoners["D5678EF"]!!
      val prisonerJosh = offenderSearchPrisoners["G9012HI"]!!
      // prisoner in another prison
      val prisonerEdward = offenderSearchPrisoners["L3456MN"]!!

      // open non-association, same prison
      val openNonAssociation = createNonAssociation(
        prisonerJohn.prisonerNumber,
        prisonerMerlin.prisonerNumber,
        isClosed = false,
      )

      // closed non-association, same prison, not returned
      createNonAssociation(
        firstPrisonerNumber = prisonerMerlin.prisonerNumber,
        secondPrisonerNumber = prisonerJosh.prisonerNumber,
        isClosed = true,
      )

      // non-association with someone in a different prison, not returned
      createNonAssociation(
        firstPrisonerNumber = prisonerEdward.prisonerNumber,
        secondPrisonerNumber = prisonerMerlin.prisonerNumber,
      )

      val prisoners = listOf(prisonerJohn, prisonerMerlin, prisonerJosh, prisonerEdward)
      offenderSearchMockServer.stubSearchByPrisonerNumbers(
        prisonerNumbers = prisoners.map(OffenderSearchPrisoner::prisonerNumber),
        prisoners,
      )

      // NOTE: Non-associations for Merlin
      val url = "/prisoner/${prisonerMerlin.prisonerNumber}/non-associations"
      webTestClient.get()
        .uri(url)
        .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          // language=json
          """
            {
              "prisonerNumber": "${prisonerMerlin.prisonerNumber}",
              "firstName": "${prisonerMerlin.firstName}",
              "lastName": "${prisonerMerlin.lastName}",
              "prisonId": "${prisonerMerlin.prisonId}",
              "prisonName": "${prisonerMerlin.prisonName}",
              "cellLocation": "${prisonerMerlin.cellLocation}",
              "nonAssociations": [
                {
                  "id": ${openNonAssociation.id},
                  "roleCode": "${openNonAssociation.secondPrisonerRole}",
                  "roleDescription": "${openNonAssociation.secondPrisonerRole.description}",
                  "reasonCode": "${openNonAssociation.reason}",
                  "reasonDescription": "${openNonAssociation.reason.description}",
                  "restrictionTypeCode": "${openNonAssociation.restrictionType}",
                  "restrictionTypeDescription": "${openNonAssociation.restrictionType.description}",
                  "comment": "${openNonAssociation.comment}",
                  "authorisedBy": "${openNonAssociation.authorisedBy}",
                  "isClosed": false,
                  "closedReason": null,
                  "closedBy": null,
                  "closedAt": null,
                  "otherPrisonerDetails": {
                    "prisonerNumber": "${prisonerJohn.prisonerNumber}",
                    "roleCode": "${openNonAssociation.firstPrisonerRole}",
                    "roleDescription": "${openNonAssociation.firstPrisonerRole.description}",
                    "firstName": "${prisonerJohn.firstName}",
                    "lastName": "${prisonerJohn.lastName}",
                    "prisonId": "${prisonerJohn.prisonId}",
                    "prisonName": "${prisonerJohn.prisonName}",
                    "cellLocation": "${prisonerJohn.cellLocation}"
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
      val prisonerJohn = offenderSearchPrisoners["A1234BC"]!!
      val prisonerMerlin = offenderSearchPrisoners["D5678EF"]!!
      val prisonerJosh = offenderSearchPrisoners["G9012HI"]!!
      // prisoner in another prison
      val prisonerEdward = offenderSearchPrisoners["L3456MN"]!!

      // open non-association, same prison, returned
      val openNonAssociation = createNonAssociation(
        prisonerJohn.prisonerNumber,
        prisonerMerlin.prisonerNumber,
        isClosed = false,
      )

      // closed non-association, same prison, returned
      val closedNonAssociation = createNonAssociation(
        firstPrisonerNumber = prisonerMerlin.prisonerNumber,
        secondPrisonerNumber = prisonerJosh.prisonerNumber,
        isClosed = true,
      )

      // non-association with someone in a different prison, not returned
      createNonAssociation(
        firstPrisonerNumber = prisonerEdward.prisonerNumber,
        secondPrisonerNumber = prisonerMerlin.prisonerNumber,
      )

      val prisoners = listOf(prisonerJohn, prisonerMerlin, prisonerJosh, prisonerEdward)
      offenderSearchMockServer.stubSearchByPrisonerNumbers(
        prisonerNumbers = prisoners.map(OffenderSearchPrisoner::prisonerNumber),
        prisoners,
      )

      // NOTE: Non-associations for Merlin
      val url = "/prisoner/${prisonerMerlin.prisonerNumber}/non-associations?includeClosed=true"
      webTestClient.get()
        .uri(url)
        .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          // language=json
          """
            {
              "prisonerNumber": "${prisonerMerlin.prisonerNumber}",
              "firstName": "${prisonerMerlin.firstName}",
              "lastName": "${prisonerMerlin.lastName}",
              "prisonId": "${prisonerMerlin.prisonId}",
              "prisonName": "${prisonerMerlin.prisonName}",
              "cellLocation": "${prisonerMerlin.cellLocation}",
              "nonAssociations": [
                {
                  "id": ${openNonAssociation.id},
                  "roleCode": "${openNonAssociation.secondPrisonerRole}",
                  "roleDescription": "${openNonAssociation.secondPrisonerRole.description}",
                  "reasonCode": "${openNonAssociation.reason}",
                  "reasonDescription": "${openNonAssociation.reason.description}",
                  "restrictionTypeCode": "${openNonAssociation.restrictionType}",
                  "restrictionTypeDescription": "${openNonAssociation.restrictionType.description}",
                  "comment": "${openNonAssociation.comment}",
                  "authorisedBy": "${openNonAssociation.authorisedBy}",
                  "isClosed": false,
                  "closedReason": null,
                  "closedBy": null,
                  "closedAt": null,
                  "otherPrisonerDetails": {
                    "prisonerNumber": "${prisonerJohn.prisonerNumber}",
                    "roleCode": "${openNonAssociation.firstPrisonerRole}",
                    "roleDescription": "${openNonAssociation.firstPrisonerRole.description}",
                    "firstName": "${prisonerJohn.firstName}",
                    "lastName": "${prisonerJohn.lastName}",
                    "prisonId": "${prisonerJohn.prisonId}",
                    "prisonName": "${prisonerJohn.prisonName}",
                    "cellLocation": "${prisonerJohn.cellLocation}"
                  }
                },
                {
                  "id": ${closedNonAssociation.id},
                  "roleCode": "${closedNonAssociation.firstPrisonerRole}",
                  "roleDescription": "${closedNonAssociation.firstPrisonerRole.description}",
                  "reasonCode": "${closedNonAssociation.reason}",
                  "reasonDescription": "${closedNonAssociation.reason.description}",
                  "restrictionTypeCode": "${closedNonAssociation.restrictionType}",
                  "restrictionTypeDescription": "${closedNonAssociation.restrictionType.description}",
                  "comment": "${closedNonAssociation.comment}",
                  "authorisedBy": "${closedNonAssociation.authorisedBy}",
                  "isClosed": true,
                  "closedReason": "They're friends now",
                  "closedBy": "CLOSE_USER",
                  "otherPrisonerDetails": {
                    "prisonerNumber": "${prisonerJosh.prisonerNumber}",
                    "roleCode": "${closedNonAssociation.secondPrisonerRole}",
                    "roleDescription": "${closedNonAssociation.secondPrisonerRole.description}",
                    "firstName": "${prisonerJosh.firstName}",
                    "lastName": "${prisonerJosh.lastName}",
                    "prisonId": "${prisonerJosh.prisonId}",
                    "prisonName": "${prisonerJosh.prisonName}",
                    "cellLocation": "${prisonerJosh.cellLocation}"
                  }
                }
              ]
            }
          """,
          false,
        )
    }

    @Test
    fun `optionally returns non-associations in other prisons`() {
      // prisoners in MDI
      val prisonerJohn = offenderSearchPrisoners["A1234BC"]!!
      val prisonerMerlin = offenderSearchPrisoners["D5678EF"]!!
      val prisonerJosh = offenderSearchPrisoners["G9012HI"]!!
      // prisoner in another prison
      val prisonerEdward = offenderSearchPrisoners["L3456MN"]!!

      // open non-association, same prison
      val openNonAssociation = createNonAssociation(
        prisonerJohn.prisonerNumber,
        prisonerMerlin.prisonerNumber,
        isClosed = false,
      )

      // closed non-association, same prison, not returned
      createNonAssociation(
        firstPrisonerNumber = prisonerMerlin.prisonerNumber,
        secondPrisonerNumber = prisonerJosh.prisonerNumber,
        isClosed = true,
      )

      // non-association with someone in a different prison
      val otherPrisonNonAssociation = createNonAssociation(
        firstPrisonerNumber = prisonerEdward.prisonerNumber,
        secondPrisonerNumber = prisonerMerlin.prisonerNumber,
      )

      val prisoners = listOf(prisonerJohn, prisonerMerlin, prisonerJosh, prisonerEdward)
      offenderSearchMockServer.stubSearchByPrisonerNumbers(
        prisonerNumbers = prisoners.map(OffenderSearchPrisoner::prisonerNumber),
        prisoners,
      )

      // NOTE: Non-associations for Merlin
      val url = "/prisoner/${prisonerMerlin.prisonerNumber}/non-associations?includeOtherPrisons=true"
      webTestClient.get()
        .uri(url)
        .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          // language=json
          """
            {
              "prisonerNumber": "${prisonerMerlin.prisonerNumber}",
              "firstName": "${prisonerMerlin.firstName}",
              "lastName": "${prisonerMerlin.lastName}",
              "prisonId": "${prisonerMerlin.prisonId}",
              "prisonName": "${prisonerMerlin.prisonName}",
              "cellLocation": "${prisonerMerlin.cellLocation}",
              "nonAssociations": [
                {
                  "id": ${openNonAssociation.id},
                  "roleCode": "${openNonAssociation.secondPrisonerRole}",
                  "roleDescription": "${openNonAssociation.secondPrisonerRole.description}",
                  "reasonCode": "${openNonAssociation.reason}",
                  "reasonDescription": "${openNonAssociation.reason.description}",
                  "restrictionTypeCode": "${openNonAssociation.restrictionType}",
                  "restrictionTypeDescription": "${openNonAssociation.restrictionType.description}",
                  "comment": "${openNonAssociation.comment}",
                  "authorisedBy": "${openNonAssociation.authorisedBy}",
                  "isClosed": false,
                  "closedReason": null,
                  "closedBy": null,
                  "closedAt": null,
                  "otherPrisonerDetails": {
                    "prisonerNumber": "${prisonerJohn.prisonerNumber}",
                    "roleCode": "${openNonAssociation.firstPrisonerRole}",
                    "roleDescription": "${openNonAssociation.firstPrisonerRole.description}",
                    "firstName": "${prisonerJohn.firstName}",
                    "lastName": "${prisonerJohn.lastName}",
                    "prisonId": "${prisonerJohn.prisonId}",
                    "prisonName": "${prisonerJohn.prisonName}",
                    "cellLocation": "${prisonerJohn.cellLocation}"
                  }
                },
                {
                  "id": ${otherPrisonNonAssociation.id},
                  "roleCode": "${otherPrisonNonAssociation.secondPrisonerRole}",
                  "roleDescription": "${otherPrisonNonAssociation.secondPrisonerRole.description}",
                  "reasonCode": "${otherPrisonNonAssociation.reason}",
                  "reasonDescription": "${otherPrisonNonAssociation.reason.description}",
                  "restrictionTypeCode": "${otherPrisonNonAssociation.restrictionType}",
                  "restrictionTypeDescription": "${otherPrisonNonAssociation.restrictionType.description}",
                  "comment": "${otherPrisonNonAssociation.comment}",
                  "authorisedBy": "${otherPrisonNonAssociation.authorisedBy}",
                  "isClosed": false,
                  "closedReason": null,
                  "closedBy": null,
                  "closedAt": null,
                  "otherPrisonerDetails": {
                    "prisonerNumber": "${prisonerEdward.prisonerNumber}",
                    "roleCode": "${otherPrisonNonAssociation.firstPrisonerRole}",
                    "roleDescription": "${otherPrisonNonAssociation.firstPrisonerRole.description}",
                    "firstName": "${prisonerEdward.firstName}",
                    "lastName": "${prisonerEdward.lastName}",
                    "prisonId": "${prisonerEdward.prisonId}",
                    "prisonName": "${prisonerEdward.prisonName}",
                    "cellLocation": "${prisonerEdward.cellLocation}"
                  }
                }
              ]
            }
          """,
          false,
        )
    }

    @Test
    fun `when invalid sort values are provided responds 400 Bad Request`() {
      val url = "/prisoner/$prisonerNumber/non-associations?sortBy=InvalidField"
      webTestClient.get()
        .uri(url)
        .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `non-associations can be sorted`() {
      // prisoners in MDI
      val prisonerJohn = offenderSearchPrisoners["A1234BC"]!!
      val prisonerMerlin = offenderSearchPrisoners["D5678EF"]!!
      val prisonerJosh = offenderSearchPrisoners["G9012HI"]!!
      // prisoner in another prison
      val prisonerEdward = offenderSearchPrisoners["L3456MN"]!!

      // open non-association, same prison
      createNonAssociation(
        prisonerJohn.prisonerNumber,
        prisonerMerlin.prisonerNumber,
        isClosed = false,
      )

      // closed non-association, same prison
      createNonAssociation(
        firstPrisonerNumber = prisonerMerlin.prisonerNumber,
        secondPrisonerNumber = prisonerJosh.prisonerNumber,
        isClosed = true,
      )

      // non-association with someone in a different prison
      createNonAssociation(
        firstPrisonerNumber = prisonerEdward.prisonerNumber,
        secondPrisonerNumber = prisonerMerlin.prisonerNumber,
      )

      val prisoners = listOf(prisonerJohn, prisonerMerlin, prisonerJosh, prisonerEdward)
      offenderSearchMockServer.stubSearchByPrisonerNumbers(
        prisonerNumbers = prisoners.map(OffenderSearchPrisoner::prisonerNumber),
        prisoners,
      )

      // NOTE: Non-associations for Merlin
      val url = "/prisoner/${prisonerMerlin.prisonerNumber}/non-associations?includeOtherPrisons=true&includeClosed=true&sortBy=LAST_NAME&sortDirection=DESC"
      val prisonerNonAssociations = webTestClient.get()
        .uri(url)
        .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .returnResult<PrisonerNonAssociations>()
        .responseBody
        .blockFirst()!!

      val lastNames = prisonerNonAssociations.nonAssociations.map { nonna ->
        nonna.otherPrisonerDetails.lastName
      }
      assertThat(lastNames).isEqualTo(
        listOf(
          "Plimburkson",
          "Lillibluprs",
          "Doe",
        ),
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
      firstPrisonerRole = Role.VICTIM,
      secondPrisonerNumber = secondPrisonerNumber,
      secondPrisonerRole = Role.PERPETRATOR,
      reason = Reason.BULLYING,
      restrictionType = RestrictionType.CELL,
      comment = "They keep fighting",
      authorisedBy = "USER_1",
      updatedBy = "A_USER",
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
