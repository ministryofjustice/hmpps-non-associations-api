package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.NonAssociationAlreadyClosedException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.NonAssociationAlreadyOpenException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.NonAssociationNotFoundException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.NullPrisonerLocationsException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.OpenNonAssociationAlreadyExistsException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.UserInContextMissingException
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CloseNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.CreateNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.DeleteNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.LegacyNonAssociation
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.LegacyNonAssociationOtherPrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationListInclusion
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociationListOptions
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PatchNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.PrisonerNonAssociations
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.ReopenNonAssociationRequest
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.toPrisonerNonAssociations
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.translateFromRolesAndReason
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.updateWith
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.NonAssociationsRepository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.findAllByPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.findAnyBetweenPrisonerNumbers
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository.findAnyInvolvingPrisonerNumbers
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import java.time.Clock
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.dto.NonAssociation as NonAssociationDTO
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.NonAssociation as NonAssociationJPA

@Service
class NonAssociationsService(
  private val nonAssociationsRepository: NonAssociationsRepository,
  private val offenderSearch: OffenderSearchService,
  private val authenticationHolder: HmppsAuthenticationHolder,
  private val telemetryClient: TelemetryClient,
  private val clock: Clock,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun createNonAssociation(createNonAssociationRequest: CreateNonAssociationRequest): NonAssociationDTO {
    val createdBy = authenticationHolder.authenticationOrNull?.userName
      ?: throw UserInContextMissingException()

    val prisonersToKeepApart = listOf(
      createNonAssociationRequest.firstPrisonerNumber,
      createNonAssociationRequest.secondPrisonerNumber,
    )

    if (nonAssociationsRepository.findAnyBetweenPrisonerNumbers(prisonersToKeepApart).isNotEmpty()) {
      throw OpenNonAssociationAlreadyExistsException(prisonersToKeepApart)
    }

    // ensure that prisoner numbers exist and their locations are not null
    val prisonersWithNullLocations = offenderSearch.searchByPrisonerNumbers(prisonersToKeepApart)
      .values
      .filter { it.prisonId == null }
      .map { it.prisonerNumber }
    if (prisonersWithNullLocations.isNotEmpty()) {
      throw NullPrisonerLocationsException(prisonersWithNullLocations)
    }

    val nonAssociationJpa = createNonAssociationRequest.toNewEntity(
      createdBy = createdBy,
      clock = clock,
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

  fun getNonAssociations(
    inclusion: NonAssociationListInclusion = NonAssociationListInclusion.ALL,
    pageable: Pageable = PageRequest.of(0, 20, Sort.by("id")),
  ): Page<NonAssociationDTO> {
    return when (inclusion) {
      NonAssociationListInclusion.OPEN_ONLY -> nonAssociationsRepository.findAllByIsClosed(false, pageable)
      NonAssociationListInclusion.CLOSED_ONLY -> nonAssociationsRepository.findAllByIsClosed(true, pageable)
      NonAssociationListInclusion.ALL -> nonAssociationsRepository.findAll(pageable)
    }.map(NonAssociationJPA::toDto)
  }

  /**
   * Returns all non-associations that exist amongst provided group of prisoners
   *
   * Optionally, a prisonId can be provided to only return non-associations
   * where both prisoners are at this given prison
   */
  fun getAnyBetween(
    prisonerNumbers: Collection<String>,
    inclusion: NonAssociationListInclusion = NonAssociationListInclusion.OPEN_ONLY,
    prisonId: String? = null,
  ): List<NonAssociationDTO> {
    var nonAssociations = nonAssociationsRepository.findAnyBetweenPrisonerNumbers(prisonerNumbers, inclusion)

    // return only non-associations where both prisoners are in given prison
    if (prisonId != null) {
      nonAssociations = filterByPrisonId(nonAssociations, prisonId)
    }

    return nonAssociations.map(NonAssociationJPA::toDto)
  }

  /**
   * Returns all non-associations involving any of the provided prisoners
   *
   * Optionally a prisonId can be provided to only return non-associations
   * where both prisoners are at this given prison
   */
  fun getAnyInvolving(
    prisonerNumbers: Collection<String>,
    inclusion: NonAssociationListInclusion = NonAssociationListInclusion.OPEN_ONLY,
    prisonId: String? = null,
  ): List<NonAssociationDTO> {
    var nonAssociations = nonAssociationsRepository.findAnyInvolvingPrisonerNumbers(prisonerNumbers, inclusion)

    // return only non-associations where both prisoners are in given prison
    if (prisonId != null) {
      nonAssociations = filterByPrisonId(nonAssociations, prisonId)
    }

    return nonAssociations.map(NonAssociationJPA::toDto)
  }

  @Transactional
  fun updateNonAssociation(id: Long, update: PatchNonAssociationRequest): NonAssociationDTO {
    val updatedBy = authenticationHolder.authenticationOrNull?.userName
      ?: throw UserInContextMissingException()

    val nonAssociation = nonAssociationsRepository.findById(id).getOrNull() ?: throw NonAssociationNotFoundException(id)

    nonAssociation.updateWith(update, updatedBy, clock)

    log.info("Updated Non-association [$id]")
    return nonAssociation.toDto()
  }

  @Transactional
  fun closeNonAssociation(id: Long, closeRequest: CloseNonAssociationRequest): NonAssociationDTO {
    val closedBy = closeRequest.closedBy
      ?: authenticationHolder.authenticationOrNull?.userName
      ?: throw UserInContextMissingException()

    val nonAssociation = nonAssociationsRepository.findById(id).getOrNull() ?: throw NonAssociationNotFoundException(id)

    if (nonAssociation.isClosed) {
      throw NonAssociationAlreadyClosedException(id)
    }

    nonAssociation.close(
      closedAt = closeRequest.closedAt ?: LocalDateTime.now(clock),
      closedBy = closedBy,
      closedReason = closeRequest.closedReason,
    )

    log.info("Closed Non-association [$id]")
    return nonAssociation.toDto()
  }

  @Transactional
  fun reopenNonAssociation(id: Long, reopenNonAssociationRequest: ReopenNonAssociationRequest): NonAssociationDTO {
    val reopenedBy = reopenNonAssociationRequest.reopenedBy
      ?: authenticationHolder.authenticationOrNull?.userName
      ?: throw UserInContextMissingException()

    val nonAssociation = nonAssociationsRepository.findById(id).getOrNull() ?: throw NonAssociationNotFoundException(id)

    if (nonAssociationsRepository.findAnyBetweenPrisonerNumbers(
        listOf(
          nonAssociation.firstPrisonerNumber,
          nonAssociation.secondPrisonerNumber,
        ),
      ).isNotEmpty()
    ) {
      throw NonAssociationAlreadyOpenException(id)
    }

    nonAssociation.reopen(
      reopenedAt = reopenNonAssociationRequest.reopenedAt ?: LocalDateTime.now(clock),
      reopenedBy = reopenedBy,
      reopenedReason = reopenNonAssociationRequest.reopenReason,
    )

    log.info("Re-opened Non-association [$id]")
    return nonAssociation.toDto()
  }

  @Transactional
  fun deleteNonAssociation(id: Long, deleteRequest: DeleteNonAssociationRequest): NonAssociationDTO {
    val nonAssociation = nonAssociationsRepository.findById(id).getOrNull() ?: throw NonAssociationNotFoundException(id)

    nonAssociationsRepository.delete(nonAssociation)

    log.info("Deleted Non-association [$id]")
    return nonAssociation.toDto()
  }

  @Transactional(readOnly = true)
  fun getPrisonerNonAssociations(prisonerNumber: String, options: NonAssociationListOptions): PrisonerNonAssociations {
    var nonAssociations = nonAssociationsRepository.findAllByPrisonerNumber(prisonerNumber)

    // load all prisoner mentioned in any non-association
    // note that we always want to retrieve the information about the key
    // prisoner, even if they don't have any non-associations
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

  fun getLegacyById(id: Long): LegacyNonAssociation? {
    return nonAssociationsRepository.findById(id).getOrNull()?.toLegacy()
  }

  private fun filterByPrisonId(nonAssociations: List<NonAssociationJPA>, prisonId: String): List<NonAssociationJPA> {
    val prisonerNumbers = nonAssociations.flatMap { nonna ->
      listOf(nonna.firstPrisonerNumber, nonna.secondPrisonerNumber)
    }
    val prisoners = offenderSearch.searchByPrisonerNumbers(prisonerNumbers)

    return nonAssociations.filter { nonna ->
      prisoners[nonna.firstPrisonerNumber]!!.prisonId == prisonId &&
        prisoners[nonna.secondPrisonerNumber]!!.prisonId == prisonId
    }
  }

  private fun persistNonAssociation(nonAssociation: NonAssociationJPA): NonAssociationJPA {
    return nonAssociationsRepository.save(nonAssociation)
  }
}

private fun NonAssociationJPA.toLegacy(): LegacyNonAssociation {
  val (firstPrisonerReason, secondPrisonerReason) = translateFromRolesAndReason(
    firstPrisonerRole,
    secondPrisonerRole,
    reason,
  )
  val typeCode = restrictionType.toLegacyRestrictionType()
  return LegacyNonAssociation(
    id = id!!,
    offenderNo = firstPrisonerNumber,
    reasonCode = firstPrisonerReason,
    reasonDescription = firstPrisonerReason.description,
    typeCode = typeCode,
    typeDescription = typeCode.description,
    effectiveDate = whenCreated,
    expiryDate = closedAt,
    authorisedBy = authorisedBy,
    comments = comment,
    offenderNonAssociation = LegacyNonAssociationOtherPrisonerDetails(
      offenderNo = secondPrisonerNumber,
      reasonCode = secondPrisonerReason,
      reasonDescription = secondPrisonerReason.description,
    ),
  )
}
