package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateSyncRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.UpdateSyncRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service.SyncAndMigrateService

@RestController
@Validated
@RequestMapping("/sync", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "Sync non-associations from NOMIS",
  description = "Receive updates from NOMIS after migration has been completed",
)
@PreAuthorize("hasRole('ROLE_NON_ASSOCIATIONS_SYNC')")
class SyncResource(
  private val syncAndMigrateService: SyncAndMigrateService,
) {

  @PostMapping()
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Sync a non-association",
    description = "Requires ROLE_NON_ASSOCIATIONS_SYNC role.",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Returns the new created non-association",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Missing required role. Requires the ROLE_NON_ASSOCIATIONS_SYNC role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun syncCreate(
    @Valid @RequestBody
    syncRequest: CreateSyncRequest,
  ): NonAssociation {
    return syncAndMigrateService.syncCreate(syncRequest)
  }

  @PutMapping()
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Sync update a non-association",
    description = "Requires ROLE_NON_ASSOCIATIONS_SYNC role.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns the updated non-association",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Missing required role. Requires the ROLE_NON_ASSOCIATIONS_SYNC role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun syncUpdate(
    @Valid @RequestBody
    updateSyncRequest: UpdateSyncRequest,
  ): NonAssociation {
    return syncAndMigrateService.syncUpdate(updateSyncRequest)
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Sync delete a non-association",
    description = "Requires ROLE_NON_ASSOCIATIONS_SYNC role.",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Non-association removed",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Missing required role. Requires the ROLE_NON_ASSOCIATIONS_SYNC role",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun syncDelete(
    @Schema(description = "Non-association ID", example = "1234123423", required = true)
    @PathVariable
    id: String,
  ) =
    syncAndMigrateService.syncDelete(id)
}
