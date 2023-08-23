package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.LegacyNonAssociationDetails
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service.NonAssociationsService

@RestController
@Validated
@RequestMapping("/legacy/api", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_NON_ASSOCIATIONS')")
@Tag(
  name = "Legacy non-associations-details",
  description = "Mimics the Prison API interface for retrieving non-associations. " +
    "Currently this is a façade that calls Prison API, but will in future use the non-associations database and yet maintain the Prison API contract.",
)
class LegacyResource(
  val nonAssociationsService: NonAssociationsService,
) {
  @GetMapping("/offenders/{prisonerNumber}/non-association-details")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get non-associations by prisoner number",
    description = "Currently this is a façade that calls Prison API, but will in future use the non-associations database and yet maintain the Prison API contract.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns non-association details for this prisoner",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner number not found (and possibly when no non-associations exist)",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getDetailsFromPrisonApiByPrisonerNumber(
    @Schema(description = "The offender prisoner number", example = "A1234BC", required = true)
    @PathVariable
    prisonerNumber: String,
    @Schema(
      description = "Returns only non-association details for this prisoner in the same prison",
      required = false,
      defaultValue = "false",
      example = "true",
    )
    @RequestParam(value = "currentPrisonOnly", required = false, defaultValue = "false")
    currentPrisonOnly: Boolean,
    @Schema(
      description = "Returns only active non-association details for this prisoner",
      required = false,
      defaultValue = "false",
      example = "true",
    )
    @RequestParam(value = "excludeInactive", required = false, defaultValue = "false")
    excludeInactive: Boolean,
  ): LegacyNonAssociationDetails {
    return nonAssociationsService.getLegacyDetails(prisonerNumber, currentPrisonOnly, excludeInactive)
  }
}
