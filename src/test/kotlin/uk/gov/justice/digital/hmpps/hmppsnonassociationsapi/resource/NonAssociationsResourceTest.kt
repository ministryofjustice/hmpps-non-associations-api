package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import com.fasterxml.jackson.core.type.TypeReference
import io.swagger.v3.oas.annotations.media.Schema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CloseNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.DeleteNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.LegacyReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationsSort
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PatchNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PrisonerNonAssociations
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.Reason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.ReopenNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.RestrictionType
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.Role
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.offendersearch.OffenderSearchPrisoner
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.createNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.genNonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.util.offenderSearchPrisoners
import java.lang.String.format
import java.time.Clock
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation as NonAssociationJPA

// language=text
const val expectedUsername = "A_TEST_USER"

@WithMockUser(username = expectedUsername)
class NonAssociationsResourceTest : SqsIntegrationTestBase() {
  @TestConfiguration
  class FixedClockConfig {
    @Primary
    @Bean
    fun fixedClock(): Clock = clock
  }

  @Nested
  inner class `Constants and enumerations` {
    private val url = "/constants"

    @Test
    fun `without a valid token responds 401 Unauthorized`() {
      webTestClient.get()
        .uri(url)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `returns all enumerations`() {
      val expectedRoles = Role.entries.map { mapOf("code" to it.name, "description" to it.description) }
      val expectedReasons = Reason.entries.map { mapOf("code" to it.name, "description" to it.description) }
      val expectedRestrictionTypes = RestrictionType.entries.map { mapOf("code" to it.name, "description" to it.description) }

      webTestClient.get()
        .uri(url)
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody().consumeWith { response ->
          val body = objectMapper.readValue(response.responseBody, object : TypeReference<Map<String, List<Map<String, String>>>>() {})
          assertThat(body["roles"]).isEqualTo(expectedRoles)
          assertThat(body["reasons"]).isEqualTo(expectedReasons)
          assertThat(body["restrictionTypes"]).isEqualTo(expectedRestrictionTypes)
        }
    }
  }

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
      val notFoundPrisonerNumber = "X1111TT" // NOT FOUND
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
            roles = listOf("ROLE_WRITE_NON_ASSOCIATIONS"),
            scopes = listOf("write", "read"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").isEqualTo(
          "Could not find the following prisoners: [X1111TT]",
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

      val expectedResponse =
        // language=json
        """
        {
          "firstPrisonerNumber": "${request.firstPrisonerNumber}",
          "firstPrisonerRole": "${request.firstPrisonerRole}",
          "firstPrisonerRoleDescription": "${request.firstPrisonerRole.description}",
          "secondPrisonerNumber": "${request.secondPrisonerNumber}",
          "secondPrisonerRole": "${request.secondPrisonerRole}",
          "secondPrisonerRoleDescription": "${request.secondPrisonerRole.description}",
          "reason": "${request.reason}",
          "reasonDescription": "${request.reason.description}",
          "restrictionType": "${request.restrictionType}",
          "restrictionTypeDescription": "${request.restrictionType.description}",
          "comment": "${request.comment}",
          "updatedBy": "$expectedUsername",
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
            roles = listOf("ROLE_WRITE_NON_ASSOCIATIONS"),
            scopes = listOf("write", "read"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isCreated
        .expectBody().json(expectedResponse, false)
        .consumeWith {
          val nonAssociation = objectMapper.readValue(it.responseBody, NonAssociation::class.java)
          assertThat(nonAssociation.id).isGreaterThan(0)
          assertThat(nonAssociation.whenCreated).isNotNull()
          assertThat(nonAssociation.whenUpdated).isEqualTo(nonAssociation.whenCreated)

          val nonAssociationJpa = repository.findById(nonAssociation.id).get()
          with(nonAssociationJpa) {
            assertThat(firstPrisonerNumber).isEqualTo(firstPrisoner.prisonerNumber)
            assertThat(firstPrisonerRole).isEqualTo(Role.VICTIM)
            assertThat(secondPrisonerNumber).isEqualTo(secondPrisoner.prisonerNumber)
            assertThat(secondPrisonerRole).isEqualTo(Role.PERPETRATOR)
            assertThat(reason).isEqualTo(Reason.VIOLENCE)
            assertThat(restrictionType).isEqualTo(RestrictionType.CELL)
            assertThat(comment).isEqualTo("They keep fighting")
            assertThat(authorisedBy).isEqualTo(expectedUsername)
            assertThat(updatedBy).isEqualTo(expectedUsername)
            assertThat(whenCreated).isEqualTo(nonAssociation.whenCreated)
            assertThat(whenUpdated).isEqualTo(nonAssociation.whenCreated)

            assertThat(isOpen).isTrue()
            assertThat(closedBy).isNull()
            assertThat(closedAt).isNull()
            assertThat(closedReason).isNull()
          }
        }
    }

    @Test
    fun `cannot create NA for already open NA between same prisoners`() {
      val firstPrisoner = offenderSearchPrisoners["A1234BC"]!!
      val secondPrisoner = offenderSearchPrisoners["D5678EF"]!!

      repository.save(
        genNonAssociation(
          firstPrisonerNumber = secondPrisoner.prisonerNumber,
          secondPrisonerNumber = firstPrisoner.prisonerNumber,
          createTime = LocalDateTime.now(clock),
        ),
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

      webTestClient.post()
        .uri(url)
        .headers(
          setAuthorisation(
            user = expectedUsername,
            roles = listOf("ROLE_WRITE_NON_ASSOCIATIONS"),
            scopes = listOf("write", "read"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isEqualTo(409)
    }

    @Test
    fun `cannot create NA if some prisoner has a null location`() {
      val firstPrisoner = offenderSearchPrisoners["A1234BC"]!!
      val secondPrisoner = offenderSearchPrisoners["D1234DD"]!!

      offenderSearchMockServer.stubSearchByPrisonerNumbers(
        listOf("A1234BC", "D1234DD"),
        listOf(firstPrisoner, secondPrisoner),
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

      webTestClient.post()
        .uri(url)
        .headers(
          setAuthorisation(
            user = expectedUsername,
            roles = listOf("ROLE_WRITE_NON_ASSOCIATIONS"),
            scopes = listOf("write", "read"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isEqualTo(409)
    }

    @Test
    fun `can create NA for already closed NA between same prisoners`() {
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

      repository.save(
        genNonAssociation(
          firstPrisonerNumber = firstPrisoner.prisonerNumber,
          secondPrisonerNumber = secondPrisoner.prisonerNumber,
          createTime = LocalDateTime.now(clock),
          closed = true,
        ),
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

      webTestClient.post()
        .uri(url)
        .headers(
          setAuthorisation(
            user = expectedUsername,
            roles = listOf("ROLE_WRITE_NON_ASSOCIATIONS"),
            scopes = listOf("write", "read"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isCreated
    }
  }

  @Nested
  inner class `Update a non-association` {

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

      val expectedResponse =
        // language=json
        """
        {
          "firstPrisonerNumber": "${nonAssociation.firstPrisonerNumber}",
          "firstPrisonerRole": "${nonAssociation.firstPrisonerRole}",
          "firstPrisonerRoleDescription": "${nonAssociation.firstPrisonerRole.description}",
          "secondPrisonerNumber": "${nonAssociation.secondPrisonerNumber}",
          "secondPrisonerRole": "${nonAssociation.secondPrisonerRole}",
          "secondPrisonerRoleDescription": "${nonAssociation.secondPrisonerRole.description}",
          "reason": "${nonAssociation.reason}",
          "reasonDescription": "${nonAssociation.reason.description}",
          "restrictionType": "${nonAssociation.restrictionType}",
          "restrictionTypeDescription": "${nonAssociation.restrictionType.description}",
          "comment": "$updatedComment",
          "updatedBy": "$expectedUsername",
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
            roles = listOf("ROLE_WRITE_NON_ASSOCIATIONS"),
            scopes = listOf("write", "read"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(expectedResponse, false)
        .consumeWith {
          val nonAssociation = objectMapper.readValue(it.responseBody, NonAssociation::class.java)
          assertThat(nonAssociation.id).isGreaterThan(0)
          assertThat(nonAssociation.whenCreated).isNotNull()
          assertThat(nonAssociation.whenUpdated).isAfterOrEqualTo(nonAssociation.whenCreated)
        }
    }
  }

  @Nested
  inner class `Get a legacy non-association` {
    @Test
    fun `without a valid token responds 401 Unauthorized`() {
      val nonAssociation = createNonAssociation()

      webTestClient.get()
        .uri("/legacy/api/non-associations/${nonAssociation.id}")
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `without the correct role responds 403 Forbidden`() {
      val nonAssociation = createNonAssociation()

      webTestClient.get()
        .uri("/legacy/api/non-associations/${nonAssociation.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_SOMETHING_ELSE")))
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `when the non-association doesn't exist responds 404 Not Found`() {
      webTestClient.get()
        .uri("/legacy/api/non-associations/101")
        .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC")))
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `when an open non-association exists returns it`() {
      val nonAssociation = createNonAssociation()

      webTestClient.get()
        .uri("/legacy/api/non-associations/${nonAssociation.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC")))
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          // language=json
          """
          {
            "id": ${nonAssociation.id},
            "offenderNo": "${nonAssociation.firstPrisonerNumber}",
            "reasonCode": "BUL",
            "reasonDescription": "Anti Bullying Strategy",
            "typeCode": "CELL",
            "typeDescription": "Do Not Locate in Same Cell",
            "expiryDate": null,
            "authorisedBy": "Mr Bobby",
            "comments": "They keep fighting",
            "offenderNonAssociation": {
              "offenderNo": "${nonAssociation.secondPrisonerNumber}",
              "reasonCode": "BUL",
              "reasonDescription": "Anti Bullying Strategy"
            }
          }
          """,
          false,
        )
    }

    @Test
    fun `when a closed non-association exists returns it`() {
      val nonAssociation = createNonAssociation(isClosed = true)

      webTestClient.get()
        .uri("/legacy/api/non-associations/${nonAssociation.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS_SYNC")))
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          // language=json
          """
          {
            "id": ${nonAssociation.id},
            "offenderNo": "${nonAssociation.firstPrisonerNumber}",
            "reasonCode": "BUL",
            "reasonDescription": "Anti Bullying Strategy",
            "typeCode": "CELL",
            "typeDescription": "Do Not Locate in Same Cell",
            "expiryDate": "${nonAssociation.closedAt?.format(dtFormat)}",
            "authorisedBy": "Mr Bobby",
            "comments": "They keep fighting",
            "offenderNonAssociation": {
              "offenderNo": "${nonAssociation.secondPrisonerNumber}",
              "reasonCode": "BUL",
              "reasonDescription": "Anti Bullying Strategy"
            }
          }
          """,
          false,
        )
    }
  }

  @Nested
  inner class `Get legacy non-associations list` {
    private lateinit var prisonerJohn: OffenderSearchPrisoner
    private lateinit var prisonerMerlin: OffenderSearchPrisoner
    private lateinit var prisonerJosh: OffenderSearchPrisoner
    private lateinit var prisonerEdward: OffenderSearchPrisoner
    private lateinit var openNonAssociation: NonAssociationJPA
    private lateinit var closedNa: NonAssociationJPA
    private lateinit var otherPrisonNa: NonAssociationJPA
    private lateinit var openNonAssociationLegacyReasons: Pair<LegacyReason, LegacyReason>
    private lateinit var closedNaLegacyReasons: Pair<LegacyReason, LegacyReason>
    private lateinit var otherPrisonNaLegacyReasons: Pair<LegacyReason, LegacyReason>

    @BeforeEach
    fun setup() {
      // prisoners in MDI
      prisonerJohn = offenderSearchPrisoners["A1234BC"]!!
      prisonerMerlin = offenderSearchPrisoners["D5678EF"]!!
      prisonerJosh = offenderSearchPrisoners["G9012HI"]!!
      // prisoner in another prison
      prisonerEdward = offenderSearchPrisoners["L3456MN"]!!

      // open non-association, same prison
      openNonAssociation = createNonAssociation(
        prisonerJohn.prisonerNumber,
        prisonerMerlin.prisonerNumber,
        isClosed = false,
      )
      openNonAssociationLegacyReasons = LegacyReason.BUL to LegacyReason.BUL

      // closed non-association
      closedNa = createNonAssociation(
        firstPrisonerNumber = prisonerMerlin.prisonerNumber,
        secondPrisonerNumber = prisonerJosh.prisonerNumber,
        isClosed = true,
        restrictionType = RestrictionType.LANDING,
        firstPrisonerRole = Role.NOT_RELEVANT,
        secondPrisonerRole = Role.PERPETRATOR,
      )
      closedNaLegacyReasons = LegacyReason.BUL to LegacyReason.BUL

      // non-association with someone in a different prison
      otherPrisonNa = createNonAssociation(
        firstPrisonerNumber = prisonerEdward.prisonerNumber,
        secondPrisonerNumber = prisonerMerlin.prisonerNumber,
        firstPrisonerRole = Role.VICTIM,
        secondPrisonerRole = Role.UNKNOWN,
      )
      otherPrisonNaLegacyReasons = LegacyReason.BUL to LegacyReason.BUL

      val prisoners = listOf(prisonerJohn, prisonerMerlin, prisonerJosh, prisonerEdward)
      offenderSearchMockServer.stubSearchByPrisonerNumbers(
        prisonerNumbers = prisoners.map(OffenderSearchPrisoner::prisonerNumber),
        prisoners,
      )
    }

    @Test
    fun `a non-association that exists is returned in the legacy format for all NAs`() {
      webTestClient.get()
        .uri("/legacy/api/offenders/${prisonerMerlin.prisonerNumber}/non-association-details?currentPrisonOnly=false&excludeInactive=false")
        .headers(
          setAuthorisation(),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody().json(
          // language=json
          """
           {
            "offenderNo": "${prisonerMerlin.prisonerNumber}",
            "firstName": "${prisonerMerlin.firstName}",
            "lastName": "${prisonerMerlin.lastName}",
            "agencyDescription": "${prisonerMerlin.prisonName}",
            "assignedLivingUnitDescription": "${prisonerMerlin.cellLocation}",
            "nonAssociations": [
              {
                "reasonCode": "${otherPrisonNaLegacyReasons.second}",
                "reasonDescription": "${otherPrisonNaLegacyReasons.second.description}",
                "typeCode": "${otherPrisonNa.restrictionType.toLegacyRestrictionType()}",
                "typeDescription": "${otherPrisonNa.restrictionType.toLegacyRestrictionType().description}",
                "effectiveDate": "${otherPrisonNa.whenCreated.format(dtFormat)}",
                "expiryDate": null,
                "authorisedBy": "${otherPrisonNa.authorisedBy}",
                "comments": "${otherPrisonNa.comment}",
                "offenderNonAssociation": {
                  "offenderNo": "${prisonerEdward.prisonerNumber}",
                  "firstName": "${prisonerEdward.firstName}",
                  "lastName": "${prisonerEdward.lastName}",
                  "reasonCode": "${otherPrisonNaLegacyReasons.first}",
                  "reasonDescription": "${otherPrisonNaLegacyReasons.first.description}",
                  "agencyDescription": "${prisonerEdward.prisonName}",
                  "assignedLivingUnitDescription": "${prisonerEdward.cellLocation}"
                }
              },
              {
                "reasonCode": "${closedNaLegacyReasons.first}",
                "reasonDescription": "${closedNaLegacyReasons.first.description}",
                "typeCode": "${closedNa.restrictionType.toLegacyRestrictionType()}",
                "typeDescription": "${closedNa.restrictionType.toLegacyRestrictionType().description}",
                "effectiveDate": "${closedNa.whenCreated.format(dtFormat)}",
                "expiryDate": "${closedNa.closedAt?.format(dtFormat)}",
                "authorisedBy": "${closedNa.authorisedBy}",
                "comments": "${closedNa.comment}",
                "offenderNonAssociation": {
                  "offenderNo": "${prisonerJosh.prisonerNumber}",
                  "firstName": "${prisonerJosh.firstName}",
                  "lastName": "${prisonerJosh.lastName}",
                  "reasonCode": "${closedNaLegacyReasons.second}",
                  "reasonDescription": "${closedNaLegacyReasons.second.description}",
                  "agencyDescription": "${prisonerJosh.prisonName}",
                  "assignedLivingUnitDescription": "${prisonerJosh.cellLocation}"
                }
              },
              {
                "reasonCode": "${openNonAssociationLegacyReasons.second}",
                "reasonDescription": "${openNonAssociationLegacyReasons.second.description}",
                "typeCode": "${openNonAssociation.restrictionType.toLegacyRestrictionType()}",
                "typeDescription": "${openNonAssociation.restrictionType.toLegacyRestrictionType().description}",
                "effectiveDate": "${openNonAssociation.whenCreated.format(dtFormat)}",
                "expiryDate": null,
                "authorisedBy": "${openNonAssociation.authorisedBy}",
                "comments": "${openNonAssociation.comment}",
                "offenderNonAssociation": {
                  "offenderNo": "${prisonerJohn.prisonerNumber}",
                  "firstName": "${prisonerJohn.firstName}",
                  "lastName": "${prisonerJohn.lastName}",
                  "reasonCode": "${openNonAssociationLegacyReasons.first}",
                  "reasonDescription": "${openNonAssociationLegacyReasons.first.description}",
                  "agencyDescription": "${prisonerJohn.prisonName}",
                  "assignedLivingUnitDescription": "${prisonerJohn.cellLocation}"
                }
              }
            ]
          }
          """,
          false,
        )
    }

    @Test
    fun `a non-association that exists is returned in the legacy format for active only NAs`() {
      webTestClient.get()
        .uri("/legacy/api/offenders/${prisonerMerlin.prisonerNumber}/non-association-details?currentPrisonOnly=false&excludeInactive=true")
        .headers(
          setAuthorisation(),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody().json(
          // language=json
          """
           {
            "offenderNo": "${prisonerMerlin.prisonerNumber}",
            "firstName": "${prisonerMerlin.firstName}",
            "lastName": "${prisonerMerlin.lastName}",
            "agencyDescription": "${prisonerMerlin.prisonName}",
            "assignedLivingUnitDescription": "${prisonerMerlin.cellLocation}",
            "nonAssociations": [
              {
                "reasonCode": "${otherPrisonNaLegacyReasons.second}",
                "reasonDescription": "${otherPrisonNaLegacyReasons.second.description}",
                "typeCode": "${otherPrisonNa.restrictionType.toLegacyRestrictionType()}",
                "typeDescription": "${otherPrisonNa.restrictionType.toLegacyRestrictionType().description}",
                "effectiveDate": "${otherPrisonNa.whenCreated.format(dtFormat)}",
                "expiryDate": null,
                "authorisedBy": "${otherPrisonNa.authorisedBy}",
                "comments": "${otherPrisonNa.comment}",
                "offenderNonAssociation": {
                  "offenderNo": "${prisonerEdward.prisonerNumber}",
                  "firstName": "${prisonerEdward.firstName}",
                  "lastName": "${prisonerEdward.lastName}",
                  "reasonCode": "${otherPrisonNaLegacyReasons.first}",
                  "reasonDescription": "${otherPrisonNaLegacyReasons.first.description}",
                  "agencyDescription": "${prisonerEdward.prisonName}",
                  "assignedLivingUnitDescription": "${prisonerEdward.cellLocation}"
                }
              },
              {
                "reasonCode": "${openNonAssociationLegacyReasons.second}",
                "reasonDescription": "${openNonAssociationLegacyReasons.second.description}",
                "typeCode": "${openNonAssociation.restrictionType.toLegacyRestrictionType()}",
                "typeDescription": "${openNonAssociation.restrictionType.toLegacyRestrictionType().description}",
                "effectiveDate": "${openNonAssociation.whenCreated.format(dtFormat)}",
                "expiryDate": null,
                "authorisedBy": "${openNonAssociation.authorisedBy}",
                "comments": "${openNonAssociation.comment}",
                "offenderNonAssociation": {
                  "offenderNo": "${prisonerJohn.prisonerNumber}",
                  "firstName": "${prisonerJohn.firstName}",
                  "lastName": "${prisonerJohn.lastName}",
                  "reasonCode": "${openNonAssociationLegacyReasons.first}",
                  "reasonDescription": "${openNonAssociationLegacyReasons.first.description}",
                  "agencyDescription": "${prisonerJohn.prisonName}",
                  "assignedLivingUnitDescription": "${prisonerJohn.cellLocation}"
                }
              }
            ]
          }
          """,
          false,
        )
    }

    @Test
    fun `a non-association that exists is returned in the legacy format for current active prison only`() {
      webTestClient.get()
        .uri("/legacy/api/offenders/${prisonerMerlin.prisonerNumber}/non-association-details?currentPrisonOnly=true&excludeInactive=true")
        .headers(
          setAuthorisation(),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody().json(
          // language=json
          """
           {
            "offenderNo": "${prisonerMerlin.prisonerNumber}",
            "firstName": "${prisonerMerlin.firstName}",
            "lastName": "${prisonerMerlin.lastName}",
            "agencyDescription": "${prisonerMerlin.prisonName}",
            "assignedLivingUnitDescription": "${prisonerMerlin.cellLocation}",
            "nonAssociations": [
              {
                "reasonCode": "${openNonAssociationLegacyReasons.second}",
                "reasonDescription": "${openNonAssociationLegacyReasons.second.description}",
                "typeCode": "${openNonAssociation.restrictionType.toLegacyRestrictionType()}",
                "typeDescription": "${openNonAssociation.restrictionType.toLegacyRestrictionType().description}",
                "effectiveDate": "${openNonAssociation.whenCreated.format(dtFormat)}",
                "expiryDate": null,
                "authorisedBy": "${openNonAssociation.authorisedBy}",
                "comments": "${openNonAssociation.comment}",
                "offenderNonAssociation": {
                  "offenderNo": "${prisonerJohn.prisonerNumber}",
                  "firstName": "${prisonerJohn.firstName}",
                  "lastName": "${prisonerJohn.lastName}",
                  "reasonCode": "${openNonAssociationLegacyReasons.first}",
                  "reasonDescription": "${openNonAssociationLegacyReasons.first.description}",
                  "agencyDescription": "${prisonerJohn.prisonName}",
                  "assignedLivingUnitDescription": "${prisonerJohn.cellLocation}"
                }
              }
            ]
          }
          """,
          false,
        )
    }

    @Test
    fun `a non-association that exists is returned in the legacy format for all active and inactive current prison only`() {
      webTestClient.get()
        .uri("/legacy/api/offenders/${prisonerMerlin.prisonerNumber}/non-association-details?currentPrisonOnly=true&excludeInactive=false")
        .headers(
          setAuthorisation(),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody().json(
          // language=json
          """
           {
            "offenderNo": "${prisonerMerlin.prisonerNumber}",
            "firstName": "${prisonerMerlin.firstName}",
            "lastName": "${prisonerMerlin.lastName}",
            "agencyDescription": "${prisonerMerlin.prisonName}",
            "assignedLivingUnitDescription": "${prisonerMerlin.cellLocation}",
            "nonAssociations": [
              {
                "reasonCode": "${closedNaLegacyReasons.first}",
                "reasonDescription": "${closedNaLegacyReasons.first.description}",
                "typeCode": "${closedNa.restrictionType.toLegacyRestrictionType()}",
                "typeDescription": "${closedNa.restrictionType.toLegacyRestrictionType().description}",
                "effectiveDate": "${closedNa.whenCreated.format(dtFormat)}",
                "expiryDate": "${closedNa.closedAt?.format(dtFormat)}",
                "authorisedBy": "${closedNa.authorisedBy}",
                "comments": "${closedNa.comment}",
                "offenderNonAssociation": {
                  "offenderNo": "${prisonerJosh.prisonerNumber}",
                  "firstName": "${prisonerJosh.firstName}",
                  "lastName": "${prisonerJosh.lastName}",
                  "reasonCode": "${closedNaLegacyReasons.second}",
                  "reasonDescription": "${closedNaLegacyReasons.second.description}",
                  "agencyDescription": "${prisonerJosh.prisonName}",
                  "assignedLivingUnitDescription": "${prisonerJosh.cellLocation}"
                }
              },
              {
                "reasonCode": "${openNonAssociationLegacyReasons.second}",
                "reasonDescription": "${openNonAssociationLegacyReasons.second.description}",
                "typeCode": "${openNonAssociation.restrictionType.toLegacyRestrictionType()}",
                "typeDescription": "${openNonAssociation.restrictionType.toLegacyRestrictionType().description}",
                "effectiveDate": "${openNonAssociation.whenCreated.format(dtFormat)}",
                "expiryDate": null,
                "authorisedBy": "${openNonAssociation.authorisedBy}",
                "comments": "${openNonAssociation.comment}",
                "offenderNonAssociation": {
                  "offenderNo": "${prisonerJohn.prisonerNumber}",
                  "firstName": "${prisonerJohn.firstName}",
                  "lastName": "${prisonerJohn.lastName}",
                  "reasonCode": "${openNonAssociationLegacyReasons.first}",
                  "reasonDescription": "${openNonAssociationLegacyReasons.first.description}",
                  "agencyDescription": "${prisonerJohn.prisonName}",
                  "assignedLivingUnitDescription": "${prisonerJohn.cellLocation}"
                }
              }
            ]
          }
          """,
          false,
        )
    }
  }

  @Nested
  inner class `Close a non-association` {

    private lateinit var nonAssociation: NonAssociationJPA
    private lateinit var closedNonAssociation: NonAssociationJPA
    private lateinit var url: String

    @BeforeEach
    fun setUp() {
      nonAssociation = createNonAssociation()
      closedNonAssociation = createNonAssociation(isClosed = true)
      url = "/non-associations/%d/close"
    }

    @Test
    fun `without a valid token responds 401 Unauthorized`() {
      webTestClient.put()
        .uri(format(url, nonAssociation.id))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `without the correct role and scope responds 403 Forbidden`() {
      val request = CloseNonAssociationRequest(
        closedReason = "Ok now",
      )

      // correct role, missing write scope
      webTestClient.put()
        .uri(format(url, nonAssociation.id))
        .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS")))
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus()
        .isForbidden

      // correct role, missing write scope
      webTestClient.put()
        .uri(format(url, nonAssociation.id))
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
      webTestClient.put()
        .uri(format(url, nonAssociation.id))
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
      webTestClient.put()
        .uri(format(url, nonAssociation.id))
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
      webTestClient.put()
        .uri(format(url, nonAssociation.id))
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_NON_ASSOCIATIONS"),
            scopes = listOf("write"),
          ),
        )
        .header("Content-Type", "text/plain")
        .bodyValue(jsonString("closedBy" to "TEST"))
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `for a valid request closes the non-association`() {
      // language=text
      val closedReason = "All fine now"
      val request = mapOf("closedReason" to closedReason)

      val expectedResponse =
        // language=json
        """
        {
          "firstPrisonerNumber": "${nonAssociation.firstPrisonerNumber}",
          "firstPrisonerRole": "${nonAssociation.firstPrisonerRole}",
          "firstPrisonerRoleDescription": "${nonAssociation.firstPrisonerRole.description}",
          "secondPrisonerNumber": "${nonAssociation.secondPrisonerNumber}",
          "secondPrisonerRole": "${nonAssociation.secondPrisonerRole}",
          "secondPrisonerRoleDescription": "${nonAssociation.secondPrisonerRole.description}",
          "reason": "${nonAssociation.reason}",
          "reasonDescription": "${nonAssociation.reason.description}",
          "restrictionType": "${nonAssociation.restrictionType}",
          "restrictionTypeDescription": "${nonAssociation.restrictionType.description}",
          "comment": "${nonAssociation.comment}",
          "updatedBy": "$expectedUsername",
          "isClosed": true,
          "closedReason": "$closedReason",
          "closedBy": $expectedUsername,
          "closedAt": "${LocalDateTime.now(clock)}"
        }
        """

      webTestClient.put()
        .uri(format(url, nonAssociation.id))
        .headers(
          setAuthorisation(
            user = expectedUsername,
            roles = listOf("ROLE_WRITE_NON_ASSOCIATIONS"),
            scopes = listOf("write", "read"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(expectedResponse, false)
        .consumeWith {
          val nonAssociation = objectMapper.readValue(it.responseBody, NonAssociation::class.java)
          assertThat(nonAssociation.id).isGreaterThan(0)
          assertThat(nonAssociation.whenCreated).isNotNull()
          assertThat(nonAssociation.whenUpdated).isAfterOrEqualTo(nonAssociation.whenCreated)
        }
    }

    @Test
    fun `already closed non-association cannot be re-closed`() {
      val request = CloseNonAssociationRequest(
        closedReason = "Please close again",
        closedBy = "MWILLIS",
        closedAt = LocalDateTime.now(clock),
      )

      webTestClient.put()
        .uri(format(url, closedNonAssociation.id))
        .headers(
          setAuthorisation(
            user = "MWILLIS",
            roles = listOf("ROLE_WRITE_NON_ASSOCIATIONS"),
            scopes = listOf("write", "read"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isEqualTo(409)
    }
  }

  @Nested
  inner class `Re-open a non-association` {

    private lateinit var naWhichHasAnAnotherOpenRecord: NonAssociationJPA
    private lateinit var naToBeReopened: NonAssociationJPA
    private lateinit var url: String

    @BeforeEach
    fun setUp() {
      naToBeReopened = createNonAssociation(isClosed = true)
      createNonAssociation(firstPrisonerNumber = "A1111GH", secondPrisonerNumber = "A1111GK")
      naWhichHasAnAnotherOpenRecord = createNonAssociation(firstPrisonerNumber = "A1111GH", secondPrisonerNumber = "A1111GK", isClosed = true)
      url = "/non-associations/%d/reopen"
    }

    @Test
    fun `without a valid token responds 401 Unauthorized`() {
      webTestClient.put()
        .uri(format(url, naToBeReopened.id))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `without the correct role and scope responds 403 Forbidden`() {
      val request = ReopenNonAssociationRequest(
        reopenReason = "All gone wrong again",
      )

      // correct role, missing write scope
      webTestClient.put()
        .uri(format(url, naToBeReopened.id))
        .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS")))
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus()
        .isForbidden

      // correct role, missing write scope
      webTestClient.put()
        .uri(format(url, naToBeReopened.id))
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_REOPEN_NON_ASSOCIATIONS"),
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
      webTestClient.put()
        .uri(format(url, naToBeReopened.id))
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_REOPEN_NON_ASSOCIATIONS"),
            scopes = listOf("write"),
          ),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isBadRequest

      // unsupported Content-Type
      webTestClient.put()
        .uri(format(url, naToBeReopened.id))
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_REOPEN_NON_ASSOCIATIONS"),
            scopes = listOf("write"),
          ),
        )
        .header("Content-Type", "text/plain")
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isBadRequest

      // request body has invalid fields
      webTestClient.put()
        .uri(format(url, naToBeReopened.id))
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_REOPEN_NON_ASSOCIATIONS"),
            scopes = listOf("write"),
          ),
        )
        .header("Content-Type", "text/plain")
        .bodyValue(jsonString("dummy" to "TEST"))
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `for a valid request closes the non-association`() {
      // language=text
      val reopenReason = "Gone Bad"
      val request = mapOf("reopenReason" to reopenReason)

      val expectedResponse =
        // language=json
        """
        {
          "firstPrisonerNumber": "${naToBeReopened.firstPrisonerNumber}",
          "firstPrisonerRole": "${naToBeReopened.firstPrisonerRole}",
          "firstPrisonerRoleDescription": "${naToBeReopened.firstPrisonerRole.description}",
          "secondPrisonerNumber": "${naToBeReopened.secondPrisonerNumber}",
          "secondPrisonerRole": "${naToBeReopened.secondPrisonerRole}",
          "secondPrisonerRoleDescription": "${naToBeReopened.secondPrisonerRole.description}",
          "reason": "${naToBeReopened.reason}",
          "reasonDescription": "${naToBeReopened.reason.description}",
          "restrictionType": "${naToBeReopened.restrictionType}",
          "restrictionTypeDescription": "${naToBeReopened.restrictionType.description}",
          "comment": "$reopenReason",
          "updatedBy": "$expectedUsername",
          "isClosed": false,
          "closedReason": null,
          "closedBy": null,
          "closedAt": null
        }
        """

      webTestClient.put()
        .uri(format(url, naToBeReopened.id))
        .headers(
          setAuthorisation(
            user = expectedUsername,
            roles = listOf("ROLE_REOPEN_NON_ASSOCIATIONS"),
            scopes = listOf("write", "read"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(expectedResponse, false)
        .consumeWith {
          val nonAssociation = objectMapper.readValue(it.responseBody, NonAssociation::class.java)
          assertThat(nonAssociation.id).isGreaterThan(0)
          assertThat(nonAssociation.whenCreated).isNotNull()
          assertThat(nonAssociation.whenUpdated).isAfterOrEqualTo(nonAssociation.whenCreated)
        }
    }

    @Test
    fun `closed non-association with existing open non-association cannot be re-opened`() {
      val request = ReopenNonAssociationRequest(
        reopenReason = "Please open again",
        staffUserNameRequestingReopen = "MWILLIS",
        reopenedAt = LocalDateTime.now(clock),
      )

      webTestClient.put()
        .uri(format(url, naWhichHasAnAnotherOpenRecord.id))
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_REOPEN_NON_ASSOCIATIONS"),
            scopes = listOf("write", "read"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isEqualTo(409)
    }
  }

  @Nested
  inner class `Delete a non-association` {

    private lateinit var nonAssociation: NonAssociationJPA
    private lateinit var url: String

    @BeforeEach
    fun setUp() {
      nonAssociation = createNonAssociation()
      url = "/non-associations/%d/delete"
    }

    @Test
    fun `without a valid token responds 401 Unauthorized`() {
      webTestClient.post()
        .uri(format(url, nonAssociation.id))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `without the correct role and scope responds 403 Forbidden`() {
      val request = DeleteNonAssociationRequest(
        deletionReason = "Raised in Error",
        staffUserNameRequestingDeletion = "JBarnes",
      )

      // correct role, missing write scope
      webTestClient.post()
        .uri(format(url, nonAssociation.id))
        .headers(setAuthorisation(roles = listOf("ROLE_DELETE_NON_ASSOCIATIONS")))
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus()
        .isForbidden

      // correct role, missing write scope
      webTestClient.post()
        .uri(format(url, nonAssociation.id))
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_DELETE_NON_ASSOCIATIONS"),
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
        .uri(format(url, nonAssociation.id))
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_DELETE_NON_ASSOCIATIONS"),
            scopes = listOf("write"),
          ),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isBadRequest

      // unsupported Content-Type
      webTestClient.post()
        .uri(format(url, nonAssociation.id))
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_DELETE_NON_ASSOCIATIONS"),
            scopes = listOf("write"),
          ),
        )
        .header("Content-Type", "text/plain")
        .bodyValue("{}")
        .exchange()
        .expectStatus()
        .isBadRequest

      // request body has invalid fields
      webTestClient.post()
        .uri(format(url, nonAssociation.id))
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_DELETE_NON_ASSOCIATIONS"),
            scopes = listOf("write"),
          ),
        )
        .header("Content-Type", "text/plain")
        .bodyValue(jsonString("staffUserNameRequestingDeletion" to "TEST"))
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `for a valid request deletes the non-association`() {
      val request = mapOf("deletionReason" to "Raised in error, please remove", "staffUserNameRequestingDeletion" to "A Test Staff Member")

      webTestClient.post()
        .uri(format(url, nonAssociation.id))
        .headers(
          setAuthorisation(
            user = expectedUsername,
            roles = listOf("ROLE_DELETE_NON_ASSOCIATIONS"),
            scopes = listOf("write", "read"),
          ),
        )
        .header("Content-Type", "application/json")
        .bodyValue(jsonString(request))
        .exchange()
        .expectStatus().isNoContent

      assertThat(repository.findById(nonAssociation.id!!)).isEmpty
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
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
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
            roles = listOf("ROLE_READ_NON_ASSOCIATIONS"),
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
        .jsonPath("firstPrisonerRoleDescription").isEqualTo(existingNonAssociation.firstPrisonerRole.description)
        .jsonPath("secondPrisonerNumber").isEqualTo(existingNonAssociation.secondPrisonerNumber)
        .jsonPath("secondPrisonerRole").isEqualTo(existingNonAssociation.secondPrisonerRole.toString())
        .jsonPath("secondPrisonerRoleDescription").isEqualTo(existingNonAssociation.secondPrisonerRole.description)
        .jsonPath("restrictionType").isEqualTo(existingNonAssociation.restrictionType.toString())
        .jsonPath("restrictionTypeDescription").isEqualTo(existingNonAssociation.restrictionType.description)
        .jsonPath("comment").isEqualTo(existingNonAssociation.comment)
    }
  }

  @Nested
  inner class `Get complete non-associations lists` {
    private lateinit var na1: NonAssociationJPA
    private lateinit var na2: NonAssociationJPA
    private lateinit var na3: NonAssociationJPA
    private lateinit var na4: NonAssociationJPA
    private lateinit var na5: NonAssociationJPA

    @BeforeEach
    fun setup() {
      na1 = createNonAssociation(firstPrisonerNumber = "A1234AA", secondPrisonerNumber = "A1234AB")
      na2 = createNonAssociation(firstPrisonerNumber = "A1235AA", secondPrisonerNumber = "A1235AB", isClosed = true)
      na3 = createNonAssociation(firstPrisonerNumber = "A1236AA", secondPrisonerNumber = "A1236AB", isClosed = true)
      na4 = createNonAssociation(firstPrisonerNumber = "A1237AA", secondPrisonerNumber = "A1237AB")
      na5 = createNonAssociation(firstPrisonerNumber = "A1238AA", secondPrisonerNumber = "A1238AB")
    }

    @Test
    fun `without a valid token responds 401 Unauthorized`() {
      webTestClient.get()
        .uri("/non-associations")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `without the correct role responds 403 Forbidden`() {
      // wrong role
      webTestClient.get()
        .uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_SOMETHING_ELSE")))
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `without a valid page size responds 400 Bad Request`() {
      webTestClient.get()
        .uri("/non-associations?size=1000")
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `a paged list of any non-association is returned for a size of one`() {
      webTestClient.get()
        .uri("/non-associations?size=1&includeClosed=true")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_READ_NON_ASSOCIATIONS"),
          ),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody().json(
          // language=json
          """
          {
            "content": [
              {
                "id": ${na1.id},
                "firstPrisonerNumber": "${na1.firstPrisonerNumber}",
                "secondPrisonerNumber": "${na1.secondPrisonerNumber}"
              }
            ],
            "pageable": {
              "pageNumber": 0,
              "pageSize": 1,
              "sort": {
                "sorted": true
              },
              "offset": 0,
              "paged": true,
              "unpaged": false
            },
            "totalPages": 5,
            "totalElements": 5,
            "last": false,
            "size": 1,
            "number": 0,
            "numberOfElements": 1,
            "first": true,
            "empty": false
           }
           """,
          false,
        )
    }

    @Test
    fun `a paged list of only open non-associations for a size of one`() {
      webTestClient.get()
        .uri("/non-associations?size=1")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_READ_NON_ASSOCIATIONS"),
          ),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody().json(
          // language=json
          """
          {
            "content": [
              {
                "id": ${na1.id},
                "firstPrisonerNumber": "${na1.firstPrisonerNumber}",
                "secondPrisonerNumber": "${na1.secondPrisonerNumber}"
              }
            ],
            "totalPages": 3,
            "totalElements": 3,
            "number": 0,
            "size": 1,
            "first": true,
            "last": false,
            "empty": false
          }
          """,
          false,
        )
    }

    @Test
    fun `a paged list of only closed non-associations for a size of one`() {
      webTestClient.get()
        .uri("/non-associations?includeClosed=true&includeOpen=false&size=1")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_READ_NON_ASSOCIATIONS"),
          ),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody().json(
          // language=json
          """
          {
            "content": [
              {
                "id": ${na2.id},
                "firstPrisonerNumber": "${na2.firstPrisonerNumber}",
                "secondPrisonerNumber": "${na2.secondPrisonerNumber}"
              }
            ],
            "totalPages": 2,
            "totalElements": 2,
            "number": 0,
            "size": 1,
            "first": true,
            "last": false,
            "empty": false
          }
          """,
          false,
        )
    }

    @Test
    fun `a paged list of any non-associations is returned for a size of two next page`() {
      webTestClient.get()
        .uri("/non-associations?size=2&includeClosed=true&page=1")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_READ_NON_ASSOCIATIONS"),
          ),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody().json(
          // language=json
          """
          {
            "content": [
              {
                "id": ${na3.id},
                "firstPrisonerNumber": "${na3.firstPrisonerNumber}",
                "secondPrisonerNumber": "${na3.secondPrisonerNumber}"
              },
               {
                "id": ${na4.id},
                "firstPrisonerNumber": "${na4.firstPrisonerNumber}",
                "secondPrisonerNumber": "${na4.secondPrisonerNumber}"
              }
            ],
            "pageable": {
              "pageNumber": 1,
              "pageSize": 2,
              "sort": {
                "sorted": true
              },
              "offset": 2,
              "paged": true,
              "unpaged": false
            },
            "totalPages": 3,
            "totalElements": 5,
            "last": false,
            "size": 2,
            "number": 1,
            "numberOfElements": 2,
            "first": false,
            "empty": false
          }
           """,
          false,
        )
    }

    @Test
    fun `a total list of any non-associations`() {
      webTestClient.get()
        .uri("/non-associations?includeOpen=true&includeClosed=true&size=6")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_READ_NON_ASSOCIATIONS"),
          ),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody().json(
          // language=json
          """
          {
            "content": [
              {
                "id": ${na1.id},
                "firstPrisonerNumber": "${na1.firstPrisonerNumber}",
                "secondPrisonerNumber": "${na1.secondPrisonerNumber}"
              },
              {
                "id": ${na2.id},
                "firstPrisonerNumber": "${na2.firstPrisonerNumber}",
                "secondPrisonerNumber": "${na2.secondPrisonerNumber}"
              },
              {
                "id": ${na3.id},
                "firstPrisonerNumber": "${na3.firstPrisonerNumber}",
                "secondPrisonerNumber": "${na3.secondPrisonerNumber}"
              },
               {
                "id": ${na4.id},
                "firstPrisonerNumber": "${na4.firstPrisonerNumber}",
                "secondPrisonerNumber": "${na4.secondPrisonerNumber}"
              },
              {
                "id": ${na5.id},
                "firstPrisonerNumber": "${na5.firstPrisonerNumber}",
                "secondPrisonerNumber": "${na5.secondPrisonerNumber}"
              }
            ],
            "pageable": {
              "pageNumber": 0,
              "pageSize": 6,
              "sort": {
                "sorted": true
              },
              "offset": 0,
              "paged": true,
              "unpaged": false
            },
            "totalPages": 1,
            "totalElements": 5,
            "last": true,
            "size": 6,
            "number": 0,
            "numberOfElements": 5,
            "first": true,
            "empty": false
          }
           """,
          false,
        )
    }

    @Test
    fun `a total list of only open non-associations`() {
      webTestClient.get()
        .uri("/non-associations?size=100")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_READ_NON_ASSOCIATIONS"),
          ),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody().json(
          // language=json
          """
          {
            "content": [
              {
                "id": ${na1.id},
                "firstPrisonerNumber": "${na1.firstPrisonerNumber}",
                "secondPrisonerNumber": "${na1.secondPrisonerNumber}"
              },
              {
                "id": ${na4.id},
                "firstPrisonerNumber": "${na4.firstPrisonerNumber}",
                "secondPrisonerNumber": "${na4.secondPrisonerNumber}"
              },
              {
                "id": ${na5.id},
                "firstPrisonerNumber": "${na5.firstPrisonerNumber}",
                "secondPrisonerNumber": "${na5.secondPrisonerNumber}"
              }
            ],
            "totalPages": 1,
            "totalElements": 3,
            "number": 0,
            "size": 100,
            "first": true,
            "last": true,
            "empty": false
          }
          """,
          false,
        )
    }

    @Test
    fun `a total list of only closed non-associations`() {
      webTestClient.get()
        .uri("/non-associations?includeOpen=false&includeClosed=true&size=100")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_READ_NON_ASSOCIATIONS"),
          ),
        )
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody().json(
          // language=json
          """
          {
            "content": [
              {
                "id": ${na2.id},
                "firstPrisonerNumber": "${na2.firstPrisonerNumber}",
                "secondPrisonerNumber": "${na2.secondPrisonerNumber}"
              },
              {
                "id": ${na3.id},
                "firstPrisonerNumber": "${na3.firstPrisonerNumber}",
                "secondPrisonerNumber": "${na3.secondPrisonerNumber}"
              }
            ],
            "totalPages": 1,
            "totalElements": 2,
            "number": 0,
            "size": 100,
            "first": true,
            "last": true,
            "empty": false
          }
          """,
          false,
        )
    }
  }

  @Nested
  inner class `Get non-associations lists for a prisoner` {
    private val prisonerNumber = "A1234BC"

    @Test
    fun `list endpoint documents all sorting options`() {
      val sortByParameter = NonAssociationsResource::getPrisonerNonAssociations.parameters.find { it.name == "sortBy" }!!
      val schemaAnnotation = sortByParameter.annotations.filterIsInstance<Schema>()[0]
      assertThat(NonAssociationsSort.entries).allMatch {
        schemaAnnotation.allowableValues.contains(it.name)
      }
    }

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
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
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
          openCount = 0,
          closedCount = 0,
          nonAssociations = emptyList(),
        ),
      )

      webTestClient.get()
        .uri("/prisoner/$prisonerNumber/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          expectedResponse,
          true,
        )
    }

    @Test
    fun `respond with 400 if neither open nor closed non-associations are requested`() {
      webTestClient.get()
        .uri("/prisoner/$prisonerNumber/non-associations?includeOpen=false&includeClosed=false")
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus()
        .isBadRequest
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
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
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
              "openCount": 1,
              "closedCount": 1,
              "nonAssociations": [
                {
                  "id": ${openNonAssociation.id},
                  "role": "${openNonAssociation.secondPrisonerRole}",
                  "roleDescription": "${openNonAssociation.secondPrisonerRole.description}",
                  "reason": "${openNonAssociation.reason}",
                  "reasonDescription": "${openNonAssociation.reason.description}",
                  "restrictionType": "${openNonAssociation.restrictionType}",
                  "restrictionTypeDescription": "${openNonAssociation.restrictionType.description}",
                  "comment": "${openNonAssociation.comment}",
                  "updatedBy": "A_DPS_USER",
                  "isClosed": false,
                  "closedReason": null,
                  "closedBy": null,
                  "closedAt": null,
                  "otherPrisonerDetails": {
                    "prisonerNumber": "${prisonerJohn.prisonerNumber}",
                    "role": "${openNonAssociation.firstPrisonerRole}",
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
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
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
              "openCount": 1,
              "closedCount": 1,
              "nonAssociations": [
                {
                  "id": ${openNonAssociation.id},
                  "role": "${openNonAssociation.secondPrisonerRole}",
                  "roleDescription": "${openNonAssociation.secondPrisonerRole.description}",
                  "reason": "${openNonAssociation.reason}",
                  "reasonDescription": "${openNonAssociation.reason.description}",
                  "restrictionType": "${openNonAssociation.restrictionType}",
                  "restrictionTypeDescription": "${openNonAssociation.restrictionType.description}",
                  "comment": "${openNonAssociation.comment}",
                  "updatedBy": "A_DPS_USER",
                  "isClosed": false,
                  "closedReason": null,
                  "closedBy": null,
                  "closedAt": null,
                  "otherPrisonerDetails": {
                    "prisonerNumber": "${prisonerJohn.prisonerNumber}",
                    "role": "${openNonAssociation.firstPrisonerRole}",
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
                  "role": "${closedNonAssociation.firstPrisonerRole}",
                  "roleDescription": "${closedNonAssociation.firstPrisonerRole.description}",
                  "reason": "${closedNonAssociation.reason}",
                  "reasonDescription": "${closedNonAssociation.reason.description}",
                  "restrictionType": "${closedNonAssociation.restrictionType}",
                  "restrictionTypeDescription": "${closedNonAssociation.restrictionType.description}",
                  "comment": "${closedNonAssociation.comment}",
                  "updatedBy": "CLOSE_USER",
                  "isClosed": true,
                  "closedReason": "They're friends now",
                  "closedBy": "CLOSE_USER",
                  "otherPrisonerDetails": {
                    "prisonerNumber": "${prisonerJosh.prisonerNumber}",
                    "role": "${closedNonAssociation.secondPrisonerRole}",
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
    fun `optionally returns only closed non-associations`() {
      // prisoners in MDI
      val prisonerJohn = offenderSearchPrisoners["A1234BC"]!!
      val prisonerMerlin = offenderSearchPrisoners["D5678EF"]!!
      val prisonerJosh = offenderSearchPrisoners["G9012HI"]!!
      // prisoner in another prison
      val prisonerEdward = offenderSearchPrisoners["L3456MN"]!!

      // open non-association, same prison, not returned
      createNonAssociation(
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
      val url = "/prisoner/${prisonerMerlin.prisonerNumber}/non-associations?includeOpen=false&includeClosed=true"
      webTestClient.get()
        .uri(url)
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
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
              "openCount": 1,
              "closedCount": 1,
              "nonAssociations": [
                {
                  "id": ${closedNonAssociation.id},
                  "role": "${closedNonAssociation.firstPrisonerRole}",
                  "roleDescription": "${closedNonAssociation.firstPrisonerRole.description}",
                  "reason": "${closedNonAssociation.reason}",
                  "reasonDescription": "${closedNonAssociation.reason.description}",
                  "restrictionType": "${closedNonAssociation.restrictionType}",
                  "restrictionTypeDescription": "${closedNonAssociation.restrictionType.description}",
                  "comment": "${closedNonAssociation.comment}",
                  "updatedBy": "CLOSE_USER",
                  "isClosed": true,
                  "closedReason": "They're friends now",
                  "closedBy": "CLOSE_USER",
                  "otherPrisonerDetails": {
                    "prisonerNumber": "${prisonerJosh.prisonerNumber}",
                    "role": "${closedNonAssociation.secondPrisonerRole}",
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
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
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
              "openCount": 2,
              "closedCount": 1,
              "nonAssociations": [
                {
                  "id": ${openNonAssociation.id},
                  "role": "${openNonAssociation.secondPrisonerRole}",
                  "roleDescription": "${openNonAssociation.secondPrisonerRole.description}",
                  "reason": "${openNonAssociation.reason}",
                  "reasonDescription": "${openNonAssociation.reason.description}",
                  "restrictionType": "${openNonAssociation.restrictionType}",
                  "restrictionTypeDescription": "${openNonAssociation.restrictionType.description}",
                  "comment": "${openNonAssociation.comment}",
                  "updatedBy": "A_DPS_USER",
                  "isClosed": false,
                  "closedReason": null,
                  "closedBy": null,
                  "closedAt": null,
                  "otherPrisonerDetails": {
                    "prisonerNumber": "${prisonerJohn.prisonerNumber}",
                    "role": "${openNonAssociation.firstPrisonerRole}",
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
                  "role": "${otherPrisonNonAssociation.secondPrisonerRole}",
                  "roleDescription": "${otherPrisonNonAssociation.secondPrisonerRole.description}",
                  "reason": "${otherPrisonNonAssociation.reason}",
                  "reasonDescription": "${otherPrisonNonAssociation.reason.description}",
                  "restrictionType": "${otherPrisonNonAssociation.restrictionType}",
                  "restrictionTypeDescription": "${otherPrisonNonAssociation.restrictionType.description}",
                  "comment": "${otherPrisonNonAssociation.comment}",
                  "updatedBy": "A_DPS_USER",
                  "isClosed": false,
                  "closedReason": null,
                  "closedBy": null,
                  "closedAt": null,
                  "otherPrisonerDetails": {
                    "prisonerNumber": "${prisonerEdward.prisonerNumber}",
                    "role": "${otherPrisonNonAssociation.firstPrisonerRole}",
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
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
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
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
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

  @Nested
  inner class `Get non-associations between a group of prisoners` {
    private val prisonerJohnNumber = "A1234BC"
    private val prisonerMerlinNumber = "D5678EF"

    private val urlPath = "/non-associations/between"

    @Test
    fun `without a valid token responds 401 Unauthorized`() {
      webTestClient.post()
        .uri(urlPath)
        .bodyValue(listOf(prisonerJohnNumber, prisonerMerlinNumber))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `without the correct role responds 403 Forbidden`() {
      webTestClient.post()
        .uri(urlPath)
        .headers(setAuthorisation(roles = listOf("WRONG_ROLE")))
        .bodyValue(listOf(prisonerJohnNumber, prisonerMerlinNumber))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `without two or more distinct prisoner numbers responds with 400 Bad Request`() {
      // no prisoners provided
      webTestClient.post()
        .uri(urlPath)
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus()
        .isBadRequest

      // empty list provided
      webTestClient.post()
        .uri(urlPath)
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(emptyList<String>())
        .exchange()
        .expectStatus()
        .isBadRequest

      // 1 prisoner provided
      webTestClient.post()
        .uri(urlPath)
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(listOf(prisonerJohnNumber))
        .exchange()
        .expectStatus()
        .isBadRequest

      // 2 non-distinct prisoners provided
      webTestClient.post()
        .uri(urlPath)
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(listOf(prisonerJohnNumber, prisonerJohnNumber))
        .exchange()
        .expectStatus()
        .isBadRequest

      // 1 non-blank prisoner provided
      webTestClient.post()
        .uri(urlPath)
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(listOf(prisonerJohnNumber, ""))
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `when there are no non-associations between the prisoners`() {
      createNonAssociation("A0011AA", prisonerJohnNumber)

      webTestClient.post()
        .uri(urlPath)
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(listOf(prisonerJohnNumber, prisonerMerlinNumber))
        .exchange()
        .expectStatus().isOk
        .expectBody().json("[]", true)
    }

    @Test
    fun `when there are non-associations between the prisoners`() {
      createNonAssociation()
      createNonAssociation(isClosed = true)

      webTestClient.post()
        .uri(urlPath)
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(listOf(prisonerJohnNumber, prisonerMerlinNumber))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          // language=json
          """
          [
            {
              "firstPrisonerNumber": "A1234BC",
              "firstPrisonerRole": "VICTIM",
              "firstPrisonerRoleDescription": "Victim",
              "secondPrisonerNumber": "D5678EF",
              "secondPrisonerRole": "PERPETRATOR",
              "secondPrisonerRoleDescription": "Perpetrator",
              "reason": "BULLYING",
              "reasonDescription": "Bullying",
              "restrictionType": "CELL",
              "restrictionTypeDescription": "Cell only",
              "comment": "They keep fighting",
              "updatedBy": "A_DPS_USER",
              "isClosed": false,
              "closedBy": null,
              "closedReason": null,
              "closedAt": null
            }
          ]
          """,
          false,
        )
    }

    @Test
    fun `when there are non-associations between the prisoners including open and closed`() {
      createNonAssociation()
      createNonAssociation(isClosed = true)

      webTestClient.post()
        .uri {
          it.path(urlPath)
            .queryParam("includeClosed", true)
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(listOf(prisonerJohnNumber, prisonerMerlinNumber))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          // language=json
          """
          [
            {
              "firstPrisonerNumber": "A1234BC",
              "firstPrisonerRole": "VICTIM",
              "firstPrisonerRoleDescription": "Victim",
              "secondPrisonerNumber": "D5678EF",
              "secondPrisonerRole": "PERPETRATOR",
              "secondPrisonerRoleDescription": "Perpetrator",
              "reason": "BULLYING",
              "reasonDescription": "Bullying",
              "restrictionType": "CELL",
              "restrictionTypeDescription": "Cell only",
              "comment": "They keep fighting",
              "updatedBy": "A_DPS_USER",
              "isClosed": false,
              "closedBy": null,
              "closedReason": null,
              "closedAt": null
            },
            {
              "firstPrisonerNumber": "A1234BC",
              "firstPrisonerRole": "VICTIM",
              "firstPrisonerRoleDescription": "Victim",
              "secondPrisonerNumber": "D5678EF",
              "secondPrisonerRole": "PERPETRATOR",
              "secondPrisonerRoleDescription": "Perpetrator",
              "reason": "BULLYING",
              "reasonDescription": "Bullying",
              "restrictionType": "CELL",
              "restrictionTypeDescription": "Cell only",
              "comment": "They keep fighting",
              "updatedBy": "CLOSE_USER",
              "isClosed": true,
              "closedBy": "CLOSE_USER",
              "closedReason": "They're friends now"
            }
          ]
          """,
          false,
        )
    }

    @Test
    fun `when there are non-associations between the prisoners including only closed`() {
      createNonAssociation()
      createNonAssociation(isClosed = true)

      webTestClient.post()
        .uri {
          it.path(urlPath)
            .queryParam("includeOpen", false)
            .queryParam("includeClosed", true)
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(listOf(prisonerJohnNumber, prisonerMerlinNumber))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          // language=json
          """
          [
            {
              "firstPrisonerNumber": "A1234BC",
              "firstPrisonerRole": "VICTIM",
              "firstPrisonerRoleDescription": "Victim",
              "secondPrisonerNumber": "D5678EF",
              "secondPrisonerRole": "PERPETRATOR",
              "secondPrisonerRoleDescription": "Perpetrator",
              "reason": "BULLYING",
              "reasonDescription": "Bullying",
              "restrictionType": "CELL",
              "restrictionTypeDescription": "Cell only",
              "comment": "They keep fighting",
              "updatedBy": "CLOSE_USER",
              "isClosed": true,
              "closedBy": "CLOSE_USER",
              "closedReason": "They're friends now"
            }
          ]
          """,
          false,
        )
    }

    @Test
    fun `when there are non-associations between the prisoners but neither open nor closed were included`() {
      createNonAssociation()
      createNonAssociation(isClosed = true)

      webTestClient.post()
        .uri {
          it.path(urlPath)
            .queryParam("includeOpen", false)
            .queryParam("includeClosed", false)
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(listOf(prisonerJohnNumber, prisonerMerlinNumber))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `when non-associations are requested between more than 2 prisoners`() {
      /*
      %% language=mermaid
      classDiagram
        A0000AA -- A1111AA
        A0000AA -- A2222AA
        A2222AA -- A3333AA
        A1111AA -- A4444AA
        A4444AA -- A2222AA : closed
      */
      createNonAssociation("A0000AA", "A1111AA") // never returned
      createNonAssociation("A0000AA", "A2222AA") // returned
      createNonAssociation("A2222AA", "A3333AA") // never returned
      createNonAssociation("A1111AA", "A4444AA") // never returned
      createNonAssociation("A4444AA", "A2222AA", true) // returned when closed is included

      webTestClient.post()
        .uri(urlPath)
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(listOf("A0000AA", "A2222AA", "A4444AA", "B0000BB"))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          // language=json
          """
          [
            {
              "firstPrisonerNumber": "A0000AA",
              "secondPrisonerNumber": "A2222AA",
              "isClosed": false
            }
          ]
          """,
          false,
        )

      webTestClient.post()
        .uri {
          it.path(urlPath)
            .queryParam("includeClosed", true)
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(listOf("A0000AA", "A2222AA", "A4444AA", "B0000BB"))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          // language=json
          """
          [
            {
              "firstPrisonerNumber": "A0000AA",
              "secondPrisonerNumber": "A2222AA",
              "isClosed": false
            },
            {
              "firstPrisonerNumber": "A4444AA",
              "secondPrisonerNumber": "A2222AA",
              "isClosed": true
            }
          ]
          """,
          false,
        )
    }

    @Test
    fun `when prisonId is provided`() {
      // prisoners in MDI
      val prisonerJohn = offenderSearchPrisoners["A1234BC"]!!
      val prisonerMerlin = offenderSearchPrisoners["D5678EF"]!!
      val prisonerJosh = offenderSearchPrisoners["G9012HI"]!!
      // prisoner in another prison
      val prisonerEdward = offenderSearchPrisoners["L3456MN"]!!

      // only non-associations between provided prisoners are returned
      createNonAssociation(prisonerMerlin.prisonerNumber, prisonerJosh.prisonerNumber) // returned
      createNonAssociation(prisonerJosh.prisonerNumber, prisonerJohn.prisonerNumber) // not returned (other prisoner)
      createNonAssociation(prisonerEdward.prisonerNumber, prisonerJosh.prisonerNumber) // not returned (other prison)

      // Stub Offender Search API request
      val prisonerNumbers = listOf(
        prisonerMerlin.prisonerNumber,
        prisonerJosh.prisonerNumber,
        prisonerEdward.prisonerNumber,
      )
      val prisoners = listOf(
        prisonerMerlin,
        prisonerJosh,
        prisonerEdward,
      )
      offenderSearchMockServer.stubSearchByPrisonerNumbers(
        prisonerNumbers,
        prisoners,
      )

      webTestClient.post()
        .uri {
          it.path(urlPath)
            .queryParam("prisonId", "MDI")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(listOf(prisonerMerlin.prisonerNumber, prisonerEdward.prisonerNumber, prisonerJosh.prisonerNumber))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          // language=json
          """
          [
            {
              "firstPrisonerNumber": "${prisonerMerlin.prisonerNumber}",
              "secondPrisonerNumber": "${prisonerJosh.prisonerNumber}",
              "isClosed": false
            }
          ]
          """,
          false,
        )
    }
  }

  @Nested
  inner class `Get non-associations involving a group of prisoners` {
    private val prisonerJohnNumber = "A1234BC"
    private val prisonerMerlinNumber = "D5678EF"

    private val urlPath = "/non-associations/involving"

    @Test
    fun `without a valid token responds 401 Unauthorized`() {
      webTestClient.post()
        .uri(urlPath)
        .bodyValue(listOf(prisonerJohnNumber, prisonerMerlinNumber))
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `without the correct role responds 403 Forbidden`() {
      webTestClient.post()
        .uri(urlPath)
        .headers(setAuthorisation(roles = listOf("WRONG_ROLE")))
        .bodyValue(listOf(prisonerJohnNumber, prisonerMerlinNumber))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `without one or more distinct prisoner numbers responds with 400 Bad Request`() {
      // no prisoners provided
      webTestClient.post()
        .uri(urlPath)
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus()
        .isBadRequest

      // empty list of prisoners provided
      webTestClient.post()
        .uri(urlPath)
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(emptyList<String>())
        .exchange()
        .expectStatus()
        .isBadRequest

      // 1 blank prisoner provided
      webTestClient.post()
        .uri(urlPath)
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(listOf(""))
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `when there are no non-associations involving the prisoners`() {
      createNonAssociation("A0011AA", "D4444DD")

      webTestClient.post()
        .uri(urlPath)
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(listOf(prisonerJohnNumber, prisonerMerlinNumber))
        .exchange()
        .expectStatus().isOk
        .expectBody().json("[]", true)
    }

    @Test
    fun `when there are non-associations involving the prisoners`() {
      createNonAssociation("D4444DD", prisonerJohnNumber)
      createNonAssociation(prisonerMerlinNumber, "C3333CC", isClosed = true)

      webTestClient.post()
        .uri(urlPath)
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(listOf(prisonerJohnNumber, prisonerMerlinNumber))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          // language=json
          """
          [
            {
              "firstPrisonerNumber": "D4444DD",
              "firstPrisonerRole": "VICTIM",
              "firstPrisonerRoleDescription": "Victim",
              "secondPrisonerNumber": "$prisonerJohnNumber",
              "secondPrisonerRole": "PERPETRATOR",
              "secondPrisonerRoleDescription": "Perpetrator",
              "reason": "BULLYING",
              "reasonDescription": "Bullying",
              "restrictionType": "CELL",
              "restrictionTypeDescription": "Cell only",
              "comment": "They keep fighting",
              "updatedBy": "A_DPS_USER",
              "isClosed": false,
              "closedBy": null,
              "closedReason": null,
              "closedAt": null
            }
          ]
          """,
          false,
        )
    }

    @Test
    fun `when there are non-associations involving the prisoners including open and closed`() {
      createNonAssociation("D4444DD", prisonerJohnNumber)
      createNonAssociation(prisonerMerlinNumber, "C3333CC", isClosed = true)

      webTestClient.post()
        .uri {
          it.path(urlPath)
            .queryParam("includeClosed", true)
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(listOf(prisonerJohnNumber, prisonerMerlinNumber))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          // language=json
          """
          [
            {
              "firstPrisonerNumber": "D4444DD",
              "firstPrisonerRole": "VICTIM",
              "firstPrisonerRoleDescription": "Victim",
              "secondPrisonerNumber": "$prisonerJohnNumber",
              "secondPrisonerRole": "PERPETRATOR",
              "secondPrisonerRoleDescription": "Perpetrator",
              "reason": "BULLYING",
              "reasonDescription": "Bullying",
              "restrictionType": "CELL",
              "restrictionTypeDescription": "Cell only",
              "comment": "They keep fighting",
              "updatedBy": "A_DPS_USER",
              "isClosed": false,
              "closedBy": null,
              "closedReason": null,
              "closedAt": null
            },
            {
              "firstPrisonerNumber": "$prisonerMerlinNumber",
              "firstPrisonerRole": "VICTIM",
              "firstPrisonerRoleDescription": "Victim",
              "secondPrisonerNumber": "C3333CC",
              "secondPrisonerRole": "PERPETRATOR",
              "secondPrisonerRoleDescription": "Perpetrator",
              "reason": "BULLYING",
              "reasonDescription": "Bullying",
              "restrictionType": "CELL",
              "restrictionTypeDescription": "Cell only",
              "comment": "They keep fighting",
              "updatedBy": "CLOSE_USER",
              "isClosed": true,
              "closedBy": "CLOSE_USER",
              "closedReason": "They're friends now"
            }
          ]
          """,
          false,
        )
    }

    @Test
    fun `when there are non-associations involving the prisoners including only closed`() {
      createNonAssociation("D4444DD", prisonerJohnNumber)
      createNonAssociation(prisonerMerlinNumber, "C3333CC", isClosed = true)

      webTestClient.post()
        .uri {
          it.path(urlPath)
            .queryParam("includeOpen", false)
            .queryParam("includeClosed", true)
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(listOf(prisonerJohnNumber, prisonerMerlinNumber))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          // language=json
          """
          [
            {
              "firstPrisonerNumber": "$prisonerMerlinNumber",
              "firstPrisonerRole": "VICTIM",
              "firstPrisonerRoleDescription": "Victim",
              "secondPrisonerNumber": "C3333CC",
              "secondPrisonerRole": "PERPETRATOR",
              "secondPrisonerRoleDescription": "Perpetrator",
              "reason": "BULLYING",
              "reasonDescription": "Bullying",
              "restrictionType": "CELL",
              "restrictionTypeDescription": "Cell only",
              "comment": "They keep fighting",
              "updatedBy": "CLOSE_USER",
              "isClosed": true,
              "closedBy": "CLOSE_USER",
              "closedReason": "They're friends now"
            }
          ]
          """,
          false,
        )
    }

    @Test
    fun `when there are non-associations involving the prisoners but neither open nor closed were included`() {
      createNonAssociation()
      createNonAssociation(isClosed = true)

      webTestClient.post()
        .uri {
          it.path(urlPath)
            .queryParam("includeOpen", false)
            .queryParam("includeClosed", false)
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(listOf(prisonerJohnNumber, prisonerMerlinNumber))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `when non-associations are requested involving more than 2 prisoners`() {
      /*
      %% language=mermaid
      classDiagram
        A0000AA -- A1111AA
        A0000AA -- A2222AA
        A2222AA -- A3333AA
        A1111AA -- A4444AA
        A4444AA -- A2222AA : closed
      */
      createNonAssociation("A0000AA", "A1111AA") // never returned
      createNonAssociation("A0000AA", "A2222AA") // returned
      createNonAssociation("A2222AA", "A3333AA") // returned
      createNonAssociation("A1111AA", "A4444AA") // returned
      createNonAssociation("A4444AA", "A2222AA", true) // returned when closed is included

      webTestClient.post()
        .uri(urlPath)
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(listOf("A2222AA", "A4444AA", "B0000BB"))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          // language=json
          """
          [
            {
              "firstPrisonerNumber": "A0000AA",
              "secondPrisonerNumber": "A2222AA",
              "isClosed": false
            },
            {
              "firstPrisonerNumber": "A2222AA",
              "secondPrisonerNumber": "A3333AA",
              "isClosed": false
            },
            {
              "firstPrisonerNumber": "A1111AA",
              "secondPrisonerNumber": "A4444AA",
              "isClosed": false
            }
          ]
          """,
          false,
        )

      webTestClient.post()
        .uri {
          it.path(urlPath)
            .queryParam("includeClosed", true)
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(listOf("A2222AA", "A4444AA", "B0000BB"))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          // language=json
          """
          [
            {
              "firstPrisonerNumber": "A0000AA",
              "secondPrisonerNumber": "A2222AA",
              "isClosed": false
            },
            {
              "firstPrisonerNumber": "A2222AA",
              "secondPrisonerNumber": "A3333AA",
              "isClosed": false
            },
            {
              "firstPrisonerNumber": "A1111AA",
              "secondPrisonerNumber": "A4444AA",
              "isClosed": false
            },
            {
              "firstPrisonerNumber": "A4444AA",
              "secondPrisonerNumber": "A2222AA",
              "isClosed": true
            }
          ]
          """,
          false,
        )
    }

    @Test
    fun `when prisonId is provided`() {
      // prisoners in MDI
      val prisonerJohn = offenderSearchPrisoners["A1234BC"]!!
      val prisonerMerlin = offenderSearchPrisoners["D5678EF"]!!
      val prisonerJosh = offenderSearchPrisoners["G9012HI"]!!
      // prisoner in another prison
      val prisonerEdward = offenderSearchPrisoners["L3456MN"]!!

      // only non-associations involving any of the provided prisoners are returned
      createNonAssociation(prisonerMerlin.prisonerNumber, prisonerJosh.prisonerNumber) // returned
      createNonAssociation(prisonerJohn.prisonerNumber, prisonerMerlin.prisonerNumber, true) // not returned
      createNonAssociation(prisonerJosh.prisonerNumber, prisonerJohn.prisonerNumber) // not returned (other prisoners)
      createNonAssociation(prisonerEdward.prisonerNumber, prisonerJosh.prisonerNumber) // not returned (other prison)

      // Stub Offender Search API request
      val prisonerNumbers = listOf(
        prisonerMerlin.prisonerNumber,
        prisonerJosh.prisonerNumber,
        prisonerEdward.prisonerNumber,
      )
      val prisoners = listOf(
        prisonerMerlin,
        prisonerJosh,
        prisonerEdward,
      )
      offenderSearchMockServer.stubSearchByPrisonerNumbers(
        prisonerNumbers,
        prisoners,
      )

      webTestClient.post()
        .uri {
          it.path(urlPath)
            .queryParam("prisonId", "MDI")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_READ_NON_ASSOCIATIONS")))
        .bodyValue(listOf(prisonerMerlin.prisonerNumber, prisonerEdward.prisonerNumber))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          // language=json
          """
          [
            {
              "firstPrisonerNumber": "${prisonerMerlin.prisonerNumber}",
              "secondPrisonerNumber": "${prisonerJosh.prisonerNumber}",
              "isClosed": false
            }
          ]
          """,
          false,
        )
    }
  }

  private fun createNonAssociation(
    firstPrisonerNumber: String = "A1234BC",
    secondPrisonerNumber: String = "D5678EF",
    isClosed: Boolean = false,
    restrictionType: RestrictionType = RestrictionType.CELL,
    firstPrisonerRole: Role = Role.VICTIM,
    secondPrisonerRole: Role = Role.PERPETRATOR,
  ): NonAssociationJPA {
    val now = LocalDateTime.now(clock)
    val nonna = NonAssociationJPA(
      firstPrisonerNumber = firstPrisonerNumber,
      firstPrisonerRole = firstPrisonerRole,
      secondPrisonerNumber = secondPrisonerNumber,
      secondPrisonerRole = secondPrisonerRole,
      reason = Reason.BULLYING,
      restrictionType = restrictionType,
      comment = "They keep fighting",
      authorisedBy = "Mr Bobby",
      updatedBy = "A_DPS_USER",
      whenCreated = now,
      whenUpdated = now,
    )

    if (isClosed) {
      nonna.updatedBy = "CLOSE_USER"
      nonna.isClosed = true
      nonna.closedReason = "They're friends now"
      nonna.closedBy = "CLOSE_USER"
      nonna.closedAt = now
    }

    return repository.save(nonna)
  }
}
