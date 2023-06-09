package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsnonassociations.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.NonAssociationDetails
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service.NonAssociationsService

@RestController
@RequestMapping("/legacy/api", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "Non-Associations", description = "Retrieve non association details from prison-api")
class PrisonApiResource(
  val nonAssociationsService: NonAssociationsService,
) {
  @GetMapping("/bookings/{bookingId}/non-association-details")
  @Operation(
    summary = "Get non-associations by booking ID",
    description = "Booking ID is an internal ID for a prisoner in NOMIS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns list of non-associations",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect data specified to return list of non-associations",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Booking ID not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getDetailsByBookingId(
    @Schema(description = "The offender booking ID", example = "123456", required = true)
    @PathVariable
    bookingId: Long,
  ): NonAssociationDetails {
    return nonAssociationsService.getDetails(bookingId)
  }
}
