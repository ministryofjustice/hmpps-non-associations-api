package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.jpa.ReasonType

@Repository
interface ReasonTypeRepository : JpaRepository<ReasonType, String>
