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
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationsDetails
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service.NonAssociationsService

// TODO: Check endpoint format/document it

@RestController
@RequestMapping("/api", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "TODO", description = "TODO")
class PrisonApiResource(
  val nonAssociationsService: NonAssociationsService,
) {
  @GetMapping("/bookings/{bookingId}/non-associations-details")
  @Operation(
    summary = "Get non-associations by bookingId",
    description = "TODO",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns list of non-associations",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to use this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Booking ID not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  suspend fun getListByBookingId(
    @Schema(description = "The offender booking id", example = "123456", required = true)
    @PathVariable
    bookingId: Long,
  ): NonAssociationsDetails {
    return nonAssociationsService.getDetails(bookingId)
  }
}
