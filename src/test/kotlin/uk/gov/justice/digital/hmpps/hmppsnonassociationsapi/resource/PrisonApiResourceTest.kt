package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.NonAssociationDetails
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.OffenderNonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.IntegrationTestBase
import java.time.LocalDateTime

class PrisonApiResourceTest : IntegrationTestBase() {

  val bookingId: Long = 123456
  val prisonerNumber = "A1234BC"

  val nonAssociationDetails =
    NonAssociationDetails(
      offenderNo = prisonerNumber,
      firstName = "James",
      lastName = "Hall",
      agencyDescription = "Moorland (HMP & YOI)",
      assignedLivingUnitDescription = "MDI-1-1-3",
      assignedLivingUnitId = 113,
      nonAssociations = listOf(
        NonAssociation(
          reasonCode = "VIC",
          reasonDescription = "Victim",
          typeCode = "WING",
          typeDescription = "Do Not Locate on Same Wing",
          effectiveDate = LocalDateTime.parse("2021-07-05T10:35:17"),
          expiryDate = LocalDateTime.parse("2021-07-05T10:35:17"),
          authorisedBy = "Officer Alice B.",
          comments = "Mr. Bloggs assaulted Mr. Hall",
          offenderNonAssociation = OffenderNonAssociation(
            offenderNo = "B1234CD",
            firstName = "Joseph",
            lastName = "Bloggs",
            reasonCode = "PER",
            reasonDescription = "Perpetrator",
            agencyDescription = "Moorland (HMP & YOI)",
            assignedLivingUnitDescription = "MDI-2-3-4",
            assignedLivingUnitId = 234,
          ),
        ),
      ),
    )

  @Nested
  inner class `GET non association details by Prisoner number` {

    @BeforeEach
    fun stubPrisonApi() {
      prisonApiMockServer.stubGetNonAssociationDetails(nonAssociationDetails)
    }

    @Test
    fun `without a prisoner number responds 400 not found`() {
      val prisonerNumber = null
      webTestClient.get()
        .uri("/legacy/api/offenders/$prisonerNumber/non-association-details")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isBadRequest
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
    fun `with a valid token returns the non-association details`() {
      val expectedResponse = jsonString(nonAssociationDetails)
      webTestClient.get()
        .uri("/legacy/api/offenders/$prisonerNumber/non-association-details")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          expectedResponse,
          true,
        )
    }
  }

  @Nested
  inner class `GET non association details by bookingId` {

    @BeforeEach
    fun stubPrisonApi() {
      prisonApiMockServer.stubGetNonAssociationDetails(bookingId, nonAssociationDetails)
    }

    @Test
    fun `without a bookingId responds 400 not found`() {
      val bookingId = null
      webTestClient.get()
        .uri("/legacy/api/bookings/$bookingId/non-association-details")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `without a valid token responds 401 unauthorized`() {
      webTestClient.get()
        .uri("/legacy/api/bookings/$bookingId/non-association-details")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `with a valid token returns the non-association details`() {
      val expectedResponse = jsonString(nonAssociationDetails)
      webTestClient.get()
        .uri("/legacy/api/bookings/$bookingId/non-association-details")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          expectedResponse,
          true,
        )
    }
  }
}
