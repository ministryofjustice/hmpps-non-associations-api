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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PrisonerNonAssociations
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service.NonAssociationListOptions
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service.NonAssociationsService
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.services.NonAssociationDomainEventType

@RestController
@Validated
@RequestMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "Non-Associations", description = "**IMPORTANT**: This is a work in progress API and it's subject to change, DO NOT USE.")
class NonAssociationsResource(
  private val nonAssociationsService: NonAssociationsService,
) : NonAssociationsBaseResource() {
  @GetMapping("/prisoner/{prisonerNumber}/non-associations")
  @PreAuthorize("hasRole('ROLE_NON_ASSOCIATIONS')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get non-associations by prisoner number. Requires ROLE_NON_ASSOCIATIONS role.",
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
        description = "Any of the prisoners could not be found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getDetailsByPrisonerNumber(
    @Schema(description = "The offender prisoner number", example = "A1234BC", required = true)
    @PathVariable
    prisonerNumber: String,
  ): PrisonerNonAssociations {
    return nonAssociationsService.getPrisonerNonAssociations(
      prisonerNumber,
      NonAssociationListOptions(
        onlySamePrison = true,
        includeClosed = false,
      ),
    )
  }

  @PostMapping("/non-associations")
  @PreAuthorize("hasRole('ROLE_NON_ASSOCIATIONS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a non-association between two prisoners.",
    description = "Requires ROLE_NON_ASSOCIATIONS role with write scope.",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Returns the created non-association",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request body",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Missing required privileges. Requires the NON_ASSOCIATIONS role with write scope",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Any of the prisoners could not be found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun createNonAssociation(
    @RequestBody
    @Validated
    createNonAssociation: CreateNonAssociationRequest,
  ): NonAssociation =
    eventPublishWrapper(NonAssociationDomainEventType.NON_ASSOCIATION_CREATED) {
      nonAssociationsService.createNonAssociation(createNonAssociation)
    }

  @GetMapping("/non-associations/{id}")
  @PreAuthorize("hasRole('ROLE_NON_ASSOCIATIONS')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get a non-association between two prisoners by ID.",
    description = "Requires ROLE_NON_ASSOCIATIONS role.",
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
        description = "Missing required role. Requires the NON_ASSOCIATIONS role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Non-association not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getNonAssociation(
    @Schema(description = "The non-association ID", example = "42", required = true)
    @PathVariable
    id: Long,
  ): NonAssociation {
    return nonAssociationsService.getById(id) ?: throw ResponseStatusException(
      HttpStatus.NOT_FOUND,
      "Non-association with ID $id not found",
    )
  }
}
