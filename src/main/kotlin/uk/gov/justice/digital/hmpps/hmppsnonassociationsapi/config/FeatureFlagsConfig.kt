package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class FeatureFlagsConfig(
  @Value("\${feature.legacy-endpoint-nomis-source-of-truth:true}")
  val legacyEndpointNomisSourceOfTruth: Boolean,
)
