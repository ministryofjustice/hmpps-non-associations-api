package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.NonAssociationDetails
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.OffenderNonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PrisonApiResourceTest : IntegrationTestBase() {

  private val mapper = ObjectMapper().findAndRegisterModules()
  @Nested
  inner class `GET non association details by bookingId` {
    val bookingId: Long = 123456
    val nonAssociationDetails =
      NonAssociationDetails(
        offenderNo = "G9109UD",
        firstName = "Fred",
        lastName = "Bloggs",
        agencyDescription = "Moorland (HMP & YOI)",
        assignedLivingUnitDescription = "MDI-1-1-3",
        nonAssociations = listOf(
          NonAssociation(
            reasonCode = "VIC",
            reasonDescription = "Victim",
            typeCode = "WING",
            typeDescription = "Do Not Locate on Same Wing",
            effectiveDate = LocalDateTime.parse("2021-07-05T10:35:17").format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            expiryDate = LocalDateTime.parse("2021-07-05T10:35:17").format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            authorisedBy = "string",
            comments = "string",
            offenderNonAssociation = OffenderNonAssociation(
              offenderNo = "A1234BC",
              firstName = "Other",
              lastName = "Person",
              reasonCode = "PER",
              reasonDescription = "Perpetrator",
              agencyDescription = "Moorland (HMP & YOI)",
              assignedLivingUnitDescription = "MDI-1-1-3",
              assignedLivingUnitId = 123,
            ),
          ),
        ),
        assignedLivingUnitId = 123,
      )

    @BeforeEach
    fun startMocks() {
      prisonApiMockServer.start()
      prisonApiMockServer.stubGetNonAssociationDetails(bookingId, nonAssociationDetails)
    }

    @AfterEach
    fun stopMocks() {
      prisonApiMockServer.stop()
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
      webTestClient.get()
        .uri("/legacy/api/bookings/$bookingId/non-association-details")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          mapper.writeValueAsString(nonAssociationDetails),
          true,
        )
    }

  }
}
