package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.ValidationException
import jakarta.validation.constraints.Pattern
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
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
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.NonAssociationNotFoundException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CloseNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.DeleteNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationListInclusion
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationListOptions
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationsSort
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PatchNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PrisonerNonAssociations
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.Reason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.ReopenNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.RestrictionType
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.Role
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service.NonAssociationsService
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.services.NonAssociationDomainEventType

@RestController
@Validated
@RequestMapping("/", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "Non-Associations", description = "Endpoints to get/create/update prisoners' non-associations")
class NonAssociationsResource(
  private val nonAssociationsService: NonAssociationsService,
) : NonAssociationsBaseResource() {
  @GetMapping("/prisoner/{prisonerNumber}/non-associations")
  @PreAuthorize("hasRole('ROLE_READ_NON_ASSOCIATIONS')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get non-associations by prisoner number",
    description = "Requires READ_NON_ASSOCIATIONS role.",
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
  @Suppress("ktlint:standard:function-signature")
  fun getPrisonerNonAssociations(
    @Schema(description = "The offender prisoner number", example = "A1234BC", required = true)
    @PathVariable
    @Pattern(regexp = "[a-zA-Z][0-9]{4}[a-zA-Z]{2}", message = "Prisoner number must be in the correct format")
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
        "WHEN_CLOSED",
        "LAST_NAME",
        "FIRST_NAME",
        "PRISONER_NUMBER",
        "PRISON_ID",
        "PRISON_NAME",
        "CELL_LOCATION",
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
      ?: throw ValidationException("includeOpen and includeClosed cannot both be false")

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
  @PreAuthorize("hasRole('ROLE_WRITE_NON_ASSOCIATIONS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a non-association between two prisoners.",
    description = "Requires WRITE_NON_ASSOCIATIONS role with write scope.",
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
        description = "Some of the prisoners were not be found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Open non-association already exists or some prisoner’s location is null.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun createNonAssociation(
    @RequestBody
    @Validated
    createNonAssociation: CreateNonAssociationRequest,
  ): NonAssociation {
    return eventPublishWrapper(NonAssociationDomainEventType.NON_ASSOCIATION_CREATED) {
      nonAssociationsService.createNonAssociation(createNonAssociation)
    }
  }

  @GetMapping("/non-associations")
  @PreAuthorize("hasRole('ROLE_READ_NON_ASSOCIATIONS')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get non-associations, filtered and paged",
    description = "Requires READ_NON_ASSOCIATIONS role",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "A page of non-associations are returned",
      ),
      ApiResponse(
        responseCode = "400",
        description = "When input parameters are not valid",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Missing required role. Requires the READ_NON_ASSOCIATIONS role.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @Suppress("ktlint:standard:function-signature")
  fun getNonAssociations(
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

    @ParameterObject
    @PageableDefault(page = 0, size = 20, sort = ["id"], direction = Sort.Direction.ASC)
    pageable: Pageable,
  ): Page<NonAssociation> {
    if (pageable.pageSize > 200) {
      throw ValidationException("Page size must be 200 or less")
    }
    val inclusion = NonAssociationListInclusion.of(includeOpen, includeClosed)
      ?: throw ValidationException("includeOpen and includeClosed cannot both be false")

    return nonAssociationsService.getNonAssociations(inclusion, pageable)
  }

  @GetMapping("/non-associations/{id}")
  @PreAuthorize("hasRole('ROLE_READ_NON_ASSOCIATIONS')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get a non-association between two prisoners by ID.",
    description = "Requires READ_NON_ASSOCIATIONS role.",
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
    return nonAssociationsService.getById(id)
      ?: throw NonAssociationNotFoundException(id)
  }

  @PostMapping("/non-associations/between")
  @PreAuthorize("hasRole('ROLE_READ_NON_ASSOCIATIONS')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get non-associations between two or more prisoners by prisoner number. " +
      "Both people in the non-associations must be in the provided list.",
    description = "Requires READ_NON_ASSOCIATIONS role.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns the non-associations",
      ),
      ApiResponse(
        responseCode = "400",
        description = "When fewer than two distinct prisoner numbers are provided " +
          "or neither open nor closed non-associations are included",
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
  @Suppress("ktlint:standard:function-signature")
  fun getNonAssociationsBetweenPrisoners(
    @ArraySchema(
      arraySchema = Schema(description = "Two or more distinct prisoner numbers"),
      schema = Schema(description = "Prisoner number", required = true, example = "A1234BC", type = "string"),
      minItems = 2,
      uniqueItems = true,
    )
    @RequestBody
    @Validated
    prisonerNumbers: List<
      @Pattern(regexp = "[a-zA-Z][0-9]{4}[a-zA-Z]{2}", message = "Prisoner number must be in the correct format")
      String,
      >?,

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
      description = "When provided return only non-associations where both prisoners are in the given prison",
      required = false,
      defaultValue = "null",
      example = "MDI",
    )
    @RequestParam(required = false)
    prisonId: String? = null,
  ): List<NonAssociation> {
    val distinctPrisonerNumbers = prisonerNumbers?.toSet()?.filter { it.isNotEmpty() }
    if (distinctPrisonerNumbers == null || distinctPrisonerNumbers.size < 2) {
      throw ValidationException("Two or more distinct prisoner numbers are required")
    }

    val inclusion = NonAssociationListInclusion.of(includeOpen, includeClosed)
      ?: throw ValidationException("includeOpen and includeClosed cannot both be false")

    return nonAssociationsService.getAnyBetween(prisonerNumbers, inclusion, prisonId)
  }

  @PostMapping("/non-associations/involving")
  @PreAuthorize("hasRole('ROLE_READ_NON_ASSOCIATIONS')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get non-associations involving any of the given prisoners. " +
      "Either person in the non-association must be in the provided list.",
    description = "Requires READ_NON_ASSOCIATIONS role.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns the non-associations",
      ),
      ApiResponse(
        responseCode = "400",
        description = "When fewer than one distinct prisoner numbers are provided " +
          "or neither open nor closed non-associations are included",
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
  @Suppress("ktlint:standard:function-signature")
  fun getNonAssociationsInvolvingPrisoners(
    @ArraySchema(
      arraySchema = Schema(description = "One or more distinct prisoner numbers"),
      schema = Schema(description = "Prisoner number", required = true, example = "A1234BC", type = "string"),
      minItems = 1,
      uniqueItems = true,
    )
    @RequestBody
    @Validated
    prisonerNumbers: List<
      @Pattern(regexp = "[a-zA-Z][0-9]{4}[a-zA-Z]{2}", message = "Prisoner number must be in the correct format")
      String,
      >?,

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
      description = "When provided return only non-associations where both prisoners are in the given prison",
      required = false,
      defaultValue = "null",
      example = "MDI",
    )
    @RequestParam(required = false)
    prisonId: String? = null,
  ): List<NonAssociation> {
    val distinctPrisonerNumbers = prisonerNumbers?.toSet()?.filter { it.isNotEmpty() }
    if (distinctPrisonerNumbers.isNullOrEmpty()) {
      throw ValidationException("One or more distinct prisoner numbers are required")
    }

    val inclusion = NonAssociationListInclusion.of(includeOpen, includeClosed)
      ?: throw ValidationException("includeOpen and includeClosed cannot both be false")

    return nonAssociationsService.getAnyInvolving(prisonerNumbers, inclusion, prisonId)
  }

  @PatchMapping("/non-associations/{id}")
  @PreAuthorize("hasRole('ROLE_WRITE_NON_ASSOCIATIONS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Partial update of a non-association by ID.",
    description = "Requires WRITE_NON_ASSOCIATIONS role with write scope.",
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
  ): NonAssociation = eventPublishWrapper(NonAssociationDomainEventType.NON_ASSOCIATION_UPSERT) {
    nonAssociationsService.updateNonAssociation(id, nonAssociationPatch)
  }

  @PutMapping("/non-associations/{id}/close")
  @PreAuthorize("hasRole('ROLE_WRITE_NON_ASSOCIATIONS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Close a non-association",
    description = "Requires WRITE_NON_ASSOCIATIONS role with write scope.",
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
  ): NonAssociation = eventPublishWrapper(NonAssociationDomainEventType.NON_ASSOCIATION_CLOSED) {
    nonAssociationsService.closeNonAssociation(id, closeNonAssociationRequest)
  }

  @PostMapping("/non-associations/{id}/delete")
  @PreAuthorize("hasRole('ROLE_DELETE_NON_ASSOCIATIONS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete a non-association",
    description = "Requires DELETE_NON_ASSOCIATIONS role with write scope.\n" +
      "**Please note**: This is a special endpoint which should NOT be exposed to regular users, " +
      "they should instead close non-associations.",
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
    deleteEventPublishWrapper {
      Pair(nonAssociationsService.deleteNonAssociation(id, deleteNonAssociationRequest), deleteNonAssociationRequest)
    }
  }

  @PutMapping("/non-associations/{id}/reopen")
  @PreAuthorize("hasRole('ROLE_REOPEN_NON_ASSOCIATIONS') and hasAuthority('SCOPE_write')")
  @Operation(
    summary = "Re-open a non-association",
    description = "Requires REOPEN_NON_ASSOCIATIONS role with write scope.\n" +
      "**Please note**: This is a special endpoint which should NOT be exposed to regular users, " +
      "they should instead create a new non-association.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Non-association re-opened",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Missing required role. Requires the REOPEN_NON_ASSOCIATIONS role with write scope.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Non-association not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun reopenNonAssociation(
    @Schema(description = "The non-association ID", example = "42", required = true)
    @PathVariable
    id: Long,
    @RequestBody
    @Validated
    reopenNonAssociationRequest: ReopenNonAssociationRequest,
  ): NonAssociation = eventPublishWrapper(NonAssociationDomainEventType.NON_ASSOCIATION_REOPENED) {
    nonAssociationsService.reopenNonAssociation(id, reopenNonAssociationRequest)
  }

  @GetMapping("/constants")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "List codes and descriptions for enumerated field types",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns codes and descriptions",
      ),
    ],
  )
  fun constants(): Map<String, List<Constant>> {
    return mapOf(
      "roles" to Role.entries.map { Constant(it.name, it.description) },
      "reasons" to Reason.entries.map { Constant(it.name, it.description) },
      "restrictionTypes" to RestrictionType.entries.map { Constant(it.name, it.description) },
    )
  }
}

data class Constant(
  val code: String,
  val description: String,
)
