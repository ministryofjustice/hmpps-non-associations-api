package uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.stereotype.Service
import java.util.Optional

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@Service(value = "auditorAware")
class AuditorAwareImpl(private val userSecurityUtils: AuthenticationFacade) : AuditorAware<String> {
  override fun getCurrentAuditor(): Optional<String> {
    return Optional.ofNullable(userSecurityUtils.currentUsername)
  }
}
