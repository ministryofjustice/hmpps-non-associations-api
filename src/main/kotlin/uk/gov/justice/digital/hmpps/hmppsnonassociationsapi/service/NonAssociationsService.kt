package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.FeatureFlagsConfig
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.NonAssociationAlreadyClosedException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.UserInContextMissingException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CloseNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PatchNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PrisonerNonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PrisonerNonAssociations
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.LegacyNonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.LegacyNonAssociationDetails
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.prisonapi.LegacyOffenderNonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.toPrisonerNonAssociations
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.updateWith
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.findAllByPairOfPrisonerNumbers
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.findAllByPrisonerNumber
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
    offenderSearch.searchByPrisonerNumbers(
      listOf(
        createNonAssociationRequest.firstPrisonerNumber,
        createNonAssociationRequest.secondPrisonerNumber,
      ),
    )

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

  fun getAllByPairOfPrisonerNumbers(prisonerNumbers: Pair<String, String>): List<NonAssociationDTO> {
    return nonAssociationsRepository.findAllByPairOfPrisonerNumbers(prisonerNumbers).map { it.toDto() }
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
      closedAt = closeRequest.dateOfClosure ?: LocalDateTime.now(clock),
      closedBy = closeRequest.staffMemberRequestingClosure ?: authenticationFacade.currentUsername ?: throw UserInContextMissingException(),
      closedReason = closeRequest.closureReason,
    )

    log.info("Closed Non-association [$id]")
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
        return getPrisonerNonAssociations(
          prisonerNumber,
          NonAssociationListOptions(includeOpen = true, includeClosed = !excludeInactive, includeOtherPrisons = !currentPrisonOnly),
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
    agencyDescription = this.prisonName,
    assignedLivingUnitDescription = this.cellLocation,
    nonAssociations = this.nonAssociations.map {
      LegacyNonAssociation(
        reasonCode = it.roleCode.toLegacyRole(),
        reasonDescription = it.roleCode.toLegacyRole().description,
        typeCode = it.restrictionTypeCode.toLegacyRestrictionType(),
        typeDescription = it.restrictionTypeCode.toLegacyRestrictionType().description,
        effectiveDate = it.whenCreated,
        expiryDate = it.closedAt,
        authorisedBy = it.authorisedBy,
        comments = it.comment,
        offenderNonAssociation = LegacyOffenderNonAssociation(
          offenderNo = it.otherPrisonerDetails.prisonerNumber,
          firstName = it.otherPrisonerDetails.firstName,
          lastName = it.otherPrisonerDetails.lastName,
          reasonCode = it.otherPrisonerDetails.roleCode.toLegacyRole(),
          reasonDescription = it.otherPrisonerDetails.roleCode.toLegacyRole().description,
          agencyDescription = it.otherPrisonerDetails.prisonName,
          assignedLivingUnitDescription = it.otherPrisonerDetails.cellLocation,
        ),
      )
    },
  )

data class NonAssociationListOptions(
  val includeOpen: Boolean = true,
  val includeClosed: Boolean = false,
  val includeOtherPrisons: Boolean = false,
  val sortBy: NonAssociationsSort? = null,
  val sortDirection: Sort.Direction? = null,
) {
  val filterForOpenAndClosed: (NonAssociationJPA) -> Boolean
    get() {
      return if (includeOpen && includeClosed) {
        { true }
      } else if (includeOpen) {
        { it.isOpen }
      } else if (includeClosed) {
        { it.isClosed }
      } else {
        { false }
      }
    }

  val comparator: Comparator<PrisonerNonAssociation>
    get() {
      val sortBy = sortBy ?: NonAssociationsSort.WHEN_CREATED
      val sortDirection = sortDirection ?: sortBy.defaultSortDirection
      return when (sortBy) {
        NonAssociationsSort.WHEN_CREATED -> compareBy(PrisonerNonAssociation::whenCreated)
        NonAssociationsSort.WHEN_UPDATED -> compareBy(PrisonerNonAssociation::whenUpdated)
        NonAssociationsSort.LAST_NAME -> compareBy { nonna -> nonna.otherPrisonerDetails.lastName }
        NonAssociationsSort.FIRST_NAME -> compareBy { nonna -> nonna.otherPrisonerDetails.firstName }
        NonAssociationsSort.PRISONER_NUMBER -> compareBy { nonna -> nonna.otherPrisonerDetails.prisonerNumber }
      }.run {
        if (sortDirection == Sort.Direction.DESC) reversed() else this
      }
    }
}

enum class NonAssociationsSort(val defaultSortDirection: Sort.Direction) {
  WHEN_CREATED(Sort.Direction.DESC),
  WHEN_UPDATED(Sort.Direction.DESC),
  LAST_NAME(Sort.Direction.ASC),
  FIRST_NAME(Sort.Direction.ASC),
  PRISONER_NUMBER(Sort.Direction.ASC),
}
