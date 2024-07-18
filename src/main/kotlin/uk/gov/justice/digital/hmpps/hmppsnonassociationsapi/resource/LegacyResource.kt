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
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.NonAssociationNotFoundException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.LegacyNonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service.NonAssociationsService

@RestController
@Validated
@RequestMapping("/legacy/api", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "Legacy non-associations-details",
  description = "Mimics the NOMIS/Prison API interface for retrieving non-associations. <em>**IMPORTANT**</em>: Use new `GET /prisoner/{prisonerNumber}/non-associations` endpoint if possible",
)
class LegacyResource(
  val nonAssociationsService: NonAssociationsService,
) {

  @GetMapping("/non-associations/{id}")
  @PreAuthorize("hasRole('ROLE_NON_ASSOCIATIONS_SYNC')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get a non-association between two prisoners by ID in NOMIS/Prison API format",
    description = "Requires NON_ASSOCIATIONS_SYNC role.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns the non-association",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Missing required role. Requires the NON_ASSOCIATIONS_SYNC role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Non-association not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getLegacyById(
    @Schema(description = "The non-association ID", example = "42", required = true)
    @PathVariable
    id: Long,
  ): LegacyNonAssociation {
    return nonAssociationsService.getLegacyById(id)
      ?: throw NonAssociationNotFoundException(id)
  }
}
