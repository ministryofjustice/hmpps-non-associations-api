package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CloseNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.DeleteNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationListInclusion
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationListOptions
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationsSort
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PatchNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PrisonerNonAssociations
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
    summary = "Get non-associations by prisoner number",
    description = "Requires ROLE_NON_ASSOCIATIONS role.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns non-association details for this prisoner",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request; for example including neither open nor closed non-associations",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
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

    @Schema(
      description = "Whether to include open non-associations or not",
      required = false,
      defaultValue = "true",
      example = "false",
    )
    @RequestParam(required = false, defaultValue = "true")
    includeOpen: Boolean = true,

    @Schema(
      description = "Whether to include closed non-associations or not",
      required = false,
      defaultValue = "false",
      example = "true",
    )
    @RequestParam(required = false, defaultValue = "false")
    includeClosed: Boolean = false,

    @Schema(
      description = "Whether to include non-associations with prisoners in other prisons",
      required = false,
      defaultValue = "false",
      example = "true",
    )
    @RequestParam(required = false, defaultValue = "false")
    includeOtherPrisons: Boolean = false,

    @Schema(
      description = "Sort non-associations by",
      required = false,
      defaultValue = "WHEN_CREATED",
      example = "LAST_NAME",
      allowableValues = [
        "WHEN_CREATED",
        "WHEN_UPDATED",
        "LAST_NAME",
        "FIRST_NAME",
        "PRISONER_NUMBER",
      ],
    )
    @RequestParam(required = false)
    sortBy: NonAssociationsSort?,

    @Schema(
      description = "Sort direction (fallback depends on sortBy)",
      required = false,
      defaultValue = "DESC",
      example = "DESC",
      allowableValues = ["ASC", "DESC"],
    )
    @RequestParam(required = false)
    sortDirection: Sort.Direction?,
  ): PrisonerNonAssociations {
    val inclusion = NonAssociationListInclusion.of(includeOpen, includeClosed)
      ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "includeOpen and includeClosed cannot both be false")

    return nonAssociationsService.getPrisonerNonAssociations(
      prisonerNumber,
      NonAssociationListOptions(
        inclusion = inclusion,
        includeOtherPrisons = includeOtherPrisons,
        sortBy = sortBy,
        sortDirection = sortDirection,
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

  @PostMapping("/non-associations/between")
  @PreAuthorize("hasRole('ROLE_NON_ASSOCIATIONS')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get non-associations between two or more prisoners by prisoner number.",
    description = "Requires ROLE_NON_ASSOCIATIONS role.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns the non-associations",
      ),
      ApiResponse(
        responseCode = "400",
        description = "When fewer than two distinct prisoner numbers are provided or neither open nor closed non-associations are included",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
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
    ],
  )
  fun getNonAssociationsBetweenPrisoners(
    @ArraySchema(
      arraySchema = Schema(description = "Two or more distinct prisoner numbers"),
      schema = Schema(description = "Prisoner number", required = true, example = "A1234BC", type = "string"),
      minItems = 2,
      uniqueItems = true,
    )
    @RequestBody
    @Validated
    prisonerNumbers: List<String>?,

    @Schema(
      description = "Whether to include open non-associations or not",
      required = false,
      defaultValue = "true",
      example = "false",
    )
    @RequestParam(required = false, defaultValue = "true")
    includeOpen: Boolean = true,

    @Schema(
      description = "Whether to include closed non-associations or not",
      required = false,
      defaultValue = "false",
      example = "true",
    )
    @RequestParam(required = false, defaultValue = "false")
    includeClosed: Boolean = false,
  ): List<NonAssociation> {
    val distinctPrisonerNumbers = prisonerNumbers?.toSet()?.filter { it.isNotEmpty() }
    if (distinctPrisonerNumbers == null || distinctPrisonerNumbers.size < 2) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Two or more distinct prisoner numbers are required")
    }

    val inclusion = NonAssociationListInclusion.of(includeOpen, includeClosed)
      ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "includeOpen and includeClosed cannot both be false")

    return nonAssociationsService.getAnyBetween(prisonerNumbers, inclusion)
  }

  @PatchMapping("/non-associations/{id}")
  @PreAuthorize("hasRole('ROLE_NON_ASSOCIATIONS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Partial update of a non-association by ID.",
    description = "Requires ROLE_NON_ASSOCIATIONS role with write scope.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Non-association updated and returned",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Missing required role. Requires the NON_ASSOCIATIONS role with write scope.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Non-association not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun patchNonAssociation(
    @Schema(description = "The non-association ID", example = "42", required = true)
    @PathVariable
    id: Long,
    @RequestBody
    @Validated
    nonAssociationPatch: PatchNonAssociationRequest,
  ): NonAssociation =
    eventPublishWrapper(NonAssociationDomainEventType.NON_ASSOCIATION_UPSERT) {
      nonAssociationsService.updateNonAssociation(id, nonAssociationPatch)
    }

  @PutMapping("/non-associations/{id}/close")
  @PreAuthorize("hasRole('ROLE_NON_ASSOCIATIONS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Close a non-association",
    description = "Requires ROLE_NON_ASSOCIATIONS role with write scope.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Non-association updated and returned",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Missing required role. Requires the NON_ASSOCIATIONS role with write scope.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Non-association not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun closeNonAssociation(
    @Schema(description = "The non-association ID", example = "42", required = true)
    @PathVariable
    id: Long,
    @RequestBody
    @Validated
    closeNonAssociationRequest: CloseNonAssociationRequest,
  ): NonAssociation =
    eventPublishWrapper(NonAssociationDomainEventType.NON_ASSOCIATION_CLOSED) {
      nonAssociationsService.closeNonAssociation(id, closeNonAssociationRequest)
    }

  @PostMapping("/non-associations/{id}/delete")
  @PreAuthorize("hasRole('ROLE_DELETE_NON_ASSOCIATIONS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete a non-association",
    description = "Requires DELETE_NON_ASSOCIATIONS role with write scope.",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Non-association deleted",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Missing required role. Requires the DELETE_NON_ASSOCIATIONS role with write scope.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Non-association not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun deleteNonAssociation(
    @Schema(description = "The non-association ID", example = "42", required = true)
    @PathVariable
    id: Long,
    @RequestBody
    @Validated
    deleteNonAssociationRequest: DeleteNonAssociationRequest,
  ) {
    eventPublishWrapperAudit(NonAssociationDomainEventType.NON_ASSOCIATION_DELETED) {
      Pair(nonAssociationsService.deleteNonAssociation(id, deleteNonAssociationRequest), deleteNonAssociationRequest)
    }
  }
}
