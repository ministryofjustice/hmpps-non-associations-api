package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.LegacyReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.LegacyRestrictionType
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.LegacyNonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.LegacyNonAssociationDetails
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.LegacyOffenderNonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.SqsIntegrationTestBase
import java.time.LocalDateTime

@ActiveProfiles("test", "nomis")
class PrisonApiResourceTest : SqsIntegrationTestBase() {

  final val prisonerNumber = "A1234BC"

  val nonAssociationDetails =
    LegacyNonAssociationDetails(
      offenderNo = prisonerNumber,
      firstName = "James",
      lastName = "Hall",
      agencyDescription = "Moorland (HMP & YOI)",
      assignedLivingUnitDescription = "MDI-1-1-3",
      nonAssociations = listOf(
        LegacyNonAssociation(
          reasonCode = LegacyReason.VIC,
          reasonDescription = "Victim",
          typeCode = LegacyRestrictionType.WING,
          typeDescription = "Do Not Locate on Same Wing",
          effectiveDate = LocalDateTime.parse("2021-07-05T10:35:17"),
          expiryDate = LocalDateTime.parse("2021-07-05T10:35:17"),
          authorisedBy = "Officer Alice B.",
          comments = "Mr. Bloggs assaulted Mr. Hall",
          offenderNonAssociation = LegacyOffenderNonAssociation(
            offenderNo = "B1234CD",
            firstName = "Joseph",
            lastName = "Bloggs",
            reasonCode = LegacyReason.PER,
            reasonDescription = "Perpetrator",
            agencyDescription = "Moorland (HMP & YOI)",
            assignedLivingUnitDescription = "MDI-2-3-4",
          ),
        ),
      ),
    )

  @Nested
  inner class `GET non association details by prisoner number` {

    @BeforeEach
    fun stubPrisonApi() {
      prisonApiMockServer.stubGetNonAssociationDetailsByPrisonerNumber(nonAssociationDetails)
    }

    @Test
    fun `without a valid token responds 401 unauthorized`() {
      webTestClient.get()
        .uri("/legacy/api/offenders/$prisonerNumber/non-association-details")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `when the Prison API responds 404 Not Found, client gets the same status code`() {
      val prisonerNumber = prisonerNumber + "not-a-prisoner"
      webTestClient.get()
        .uri("/legacy/api/offenders/$prisonerNumber/non-association-details")
        .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `with a valid token returns the non-association details`() {
      val expectedResponse = jsonString(nonAssociationDetails)
      webTestClient.get()
        .uri("/legacy/api/offenders/$prisonerNumber/non-association-details")
        .headers(setAuthorisation(roles = listOf("ROLE_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          expectedResponse,
          true,
        )
    }
  }
}
