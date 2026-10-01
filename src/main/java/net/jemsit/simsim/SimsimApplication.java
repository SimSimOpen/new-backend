package net.jemsit.simsim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
// Writing @ComponentScan explicitly replaces the one @SpringBootApplication would have applied, and
// with it drops that scan's default filters. Two matter here, both for net.jemsit.notify:
//
// AutoConfigurationExcludeFilter - without it this scan claims NotifyAutoConfiguration as an ordinary
// config class. Scanned classes are processed before deferred auto-configurations, so its
// @ConditionalOnBean(DataSource) is evaluated before any DataSource exists and the module switches
// itself off with no error, only a line in the --debug conditions report.
//
// The net.jemsit.notify exclusion - the filter above only excludes auto-configurations, and
// NotifyStreamController is a plain @RestController. Left to the scan it is registered regardless of
// notify.enabled, and with the module disabled its NotifyStreamService and NotifyPublisher
// constructor arguments do not exist, so the whole application fails to start. Fail-closed has to
// mean inert, not unbootable. Excluding the package makes the module install the way its own
// documentation says it should: no annotation, no import, no @ComponentScan entry from the host.
// When notify.enabled=true the auto-configuration's @Bean methods supply every one of these classes
// instead, so the exclusion costs nothing.
@ComponentScan(basePackages = {"net.jemsit"},
		excludeFilters = {
				@ComponentScan.Filter(type = FilterType.CUSTOM,
						classes = AutoConfigurationExcludeFilter.class),
				@ComponentScan.Filter(type = FilterType.REGEX,
						pattern = "net\\.jemsit\\.notify\\..*")
		})
@EntityScan(basePackages = {"net.jemsit"})
@EnableJpaRepositories(basePackages = {"net.jemsit"})
@EnableAsync
public class SimsimApplication {

	public static void main(String[] args) {
		SpringApplication.run(SimsimApplication.class, args);
	}

}
