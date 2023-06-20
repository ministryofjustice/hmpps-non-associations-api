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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationDetails

@RestController
@Validated
@RequestMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "Non-Associations", description = "Retrieve non-associations")
class NonAssociationsResource() {
  @GetMapping("/prisoner/{prisonerNumber}/non-associations")
  @PreAuthorize("hasRole('ROLE_NON_ASSOCIATIONS')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "**IMPORTANT**: This is a work in progress API and it's subject to change, DO NOT USE. Get non-associations by prisoner number. Requires ROLE_NON_ASSOCIATIONS role.",
    description = "The offender prisoner number",
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
        responseCode = "403",
        description = "Missing required role. Requires the NON_ASSOCIATIONS role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner number not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getDetailsByPrisonerNumber(
    @Schema(description = "The offender prisoner number", example = "A1234BC", required = true)
    @PathVariable
    prisonerNumber: String,
  ): NonAssociationDetails {
    return NonAssociationDetails(prisonerNumber)
  }
}
