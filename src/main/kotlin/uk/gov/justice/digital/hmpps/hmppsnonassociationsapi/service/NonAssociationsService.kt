package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.FeatureFlagsConfig
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.NonAssociationAlreadyClosedException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.OpenNonAssociationAlreadyExistsException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.UserInContextMissingException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CloseNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.DeleteNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationListInclusion
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationListOptions
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PatchNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PrisonerNonAssociations
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.LegacyNonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.LegacyNonAssociationDetails
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.LegacyOffenderNonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.toPrisonerNonAssociations
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.translateFromRolesAndReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.updateWith
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.findAllByPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.findAnyBetweenPrisonerNumbers
import java.time.Clock
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation as NonAssociationDTO
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation as NonAssociationJPA

@Service
@Transactional
class NonAssociationsService(
  private val nonAssociationsRepository: NonAssociationsRepository,
  private val offenderSearch: OffenderSearchService,
  private val authenticationFacade: AuthenticationFacade,
  private val prisonApiService: PrisonApiService,
  private val telemetryClient: TelemetryClient,
  private val featureFlagsConfig: FeatureFlagsConfig,
  private val clock: Clock,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createNonAssociation(createNonAssociationRequest: CreateNonAssociationRequest): NonAssociationDTO {
    val prisonersToKeepApart = listOf(
      createNonAssociationRequest.firstPrisonerNumber,
      createNonAssociationRequest.secondPrisonerNumber,
    )

    if (nonAssociationsRepository.findAnyBetweenPrisonerNumbers(prisonersToKeepApart).isNotEmpty()) {
      throw OpenNonAssociationAlreadyExistsException(prisonersToKeepApart)
    }

    offenderSearch.searchByPrisonerNumbers(prisonersToKeepApart)

    val nonAssociationJpa = createNonAssociationRequest.toNewEntity(
      authorisedBy = authenticationFacade.currentUsername
        ?: throw UserInContextMissingException(),
    )
    val nonAssociation = persistNonAssociation(nonAssociationJpa).toDto()
    log.info("Created Non-association [${nonAssociation.id}]")
    telemetryClient.trackEvent(
      "Created Non-Association",
      mapOf(
        "id" to nonAssociation.id.toString(),
        "1stPrisoner" to nonAssociation.firstPrisonerNumber,
        "2ndPrisoner" to nonAssociation.secondPrisonerNumber,
      ),
      null,
    )

    return nonAssociation
  }

  fun getById(id: Long): NonAssociationDTO? {
    return nonAssociationsRepository.findById(id).getOrNull()?.toDto()
  }

  /**
   * Returns all non-associations that exist amongst provided group of prisoners
   */
  fun getAnyBetween(
    prisonerNumbers: Collection<String>,
    inclusion: NonAssociationListInclusion = NonAssociationListInclusion.OPEN_ONLY,
  ): List<NonAssociationDTO> {
    return nonAssociationsRepository.findAnyBetweenPrisonerNumbers(prisonerNumbers, inclusion)
      .map { it.toDto() }
  }

  fun updateNonAssociation(id: Long, update: PatchNonAssociationRequest): NonAssociationDTO {
    val nonAssociation = nonAssociationsRepository.findById(id).getOrNull() ?: throw ResponseStatusException(
      HttpStatus.NOT_FOUND,
      "Non-association with ID $id not found",
    )

    nonAssociation.updateWith(update)

    log.info("Updated Non-association [$id]")
    return nonAssociation.toDto()
  }

  fun closeNonAssociation(id: Long, closeRequest: CloseNonAssociationRequest): NonAssociationDTO {
    val nonAssociation = nonAssociationsRepository.findById(id).getOrNull() ?: throw ResponseStatusException(
      HttpStatus.NOT_FOUND,
      "Non-association with ID $id not found",
    )

    if (nonAssociation.isClosed) {
      throw NonAssociationAlreadyClosedException(id)
    }

    nonAssociation.close(
      closedAt = closeRequest.closedAt ?: LocalDateTime.now(clock),
      closedBy = closeRequest.closedBy ?: authenticationFacade.currentUsername ?: throw UserInContextMissingException(),
      closedReason = closeRequest.closedReason,
    )

    log.info("Closed Non-association [$id]")
    return nonAssociation.toDto()
  }

  fun deleteNonAssociation(id: Long, deleteRequest: DeleteNonAssociationRequest): NonAssociationDTO {
    val nonAssociation = nonAssociationsRepository.findById(id).getOrNull() ?: throw ResponseStatusException(
      HttpStatus.NOT_FOUND,
      "Non-association with ID $id not found",
    )

    nonAssociationsRepository.delete(nonAssociation)

    log.info("Deleted Non-association [$id]")
    return nonAssociation.toDto()
  }

  fun getPrisonerNonAssociations(prisonerNumber: String, options: NonAssociationListOptions): PrisonerNonAssociations {
    var nonAssociations = nonAssociationsRepository.findAllByPrisonerNumber(prisonerNumber)

    // load all prisoner mentioned in any non-association
    val prisonerNumbers = nonAssociations.flatMapTo(mutableSetOf(prisonerNumber)) {
      listOf(it.firstPrisonerNumber, it.secondPrisonerNumber)
    }
    val prisoners = offenderSearch.searchByPrisonerNumbers(prisonerNumbers)

    // filter out non-associations in other prisons
    // this should be done first because open/closed non-associations will need to be counted
    if (!options.includeOtherPrisons) {
      val prisonId = prisoners[prisonerNumber]!!.prisonId
      nonAssociations = nonAssociations.filter { nonna ->
        prisoners[nonna.firstPrisonerNumber]!!.prisonId == prisonId &&
          prisoners[nonna.secondPrisonerNumber]!!.prisonId == prisonId
      }
    }

    // count open & closed non-associations
    val (openCount, closedCount) = nonAssociations.fold(0 to 0) { counts, nonAssociation ->
      val (openCount, closedCount) = counts
      if (nonAssociation.isOpen) {
        (openCount + 1) to closedCount
      } else {
        openCount to (closedCount + 1)
      }
    }

    // filter out open or closed non-associations if necessary
    nonAssociations = nonAssociations.filter(options.filterForOpenAndClosed)

    return nonAssociations.toPrisonerNonAssociations(
      prisonerNumber,
      prisoners,
      options,
      openCount,
      closedCount,
    )
  }

  fun getLegacyDetails(
    prisonerNumber: String,
    currentPrisonOnly: Boolean = true,
    excludeInactive: Boolean = true,
  ): LegacyNonAssociationDetails {
    return when (featureFlagsConfig.legacyEndpointNomisSourceOfTruth) {
      true -> prisonApiService.getNonAssociationDetails(prisonerNumber, currentPrisonOnly, excludeInactive)
      false -> {
        val inclusion = if (excludeInactive) NonAssociationListInclusion.OPEN_ONLY else NonAssociationListInclusion.ALL
        return getPrisonerNonAssociations(
          prisonerNumber,
          NonAssociationListOptions(inclusion = inclusion, includeOtherPrisons = !currentPrisonOnly),
        ).toLegacy()
      }
    }
  }

  private fun persistNonAssociation(nonAssociation: NonAssociationJPA): NonAssociationJPA {
    return nonAssociationsRepository.save(nonAssociation)
  }
}

private fun PrisonerNonAssociations.toLegacy() =
  LegacyNonAssociationDetails(
    offenderNo = this.prisonerNumber,
    firstName = this.firstName,
    lastName = this.lastName,
    agencyId = this.prisonId,
    agencyDescription = this.prisonName,
    assignedLivingUnitDescription = this.cellLocation,
    nonAssociations = this.nonAssociations.map {
      val (reason, otherReason) = translateFromRolesAndReason(it.role, it.otherPrisonerDetails.role, it.reason)
      LegacyNonAssociation(
        reasonCode = reason,
        reasonDescription = reason.description,
        typeCode = it.restrictionType.toLegacyRestrictionType(),
        typeDescription = it.restrictionType.toLegacyRestrictionType().description,
        effectiveDate = it.whenCreated,
        expiryDate = it.closedAt,
        authorisedBy = it.authorisedBy,
        comments = it.comment,
        offenderNonAssociation = LegacyOffenderNonAssociation(
          offenderNo = it.otherPrisonerDetails.prisonerNumber,
          firstName = it.otherPrisonerDetails.firstName,
          lastName = it.otherPrisonerDetails.lastName,
          reasonCode = otherReason,
          reasonDescription = otherReason.description,
          agencyId = it.otherPrisonerDetails.prisonId,
          agencyDescription = it.otherPrisonerDetails.prisonName,
          assignedLivingUnitDescription = it.otherPrisonerDetails.cellLocation,
        ),
      )
    },
  )
