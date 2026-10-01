package net.jemsit.notify;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.sql.DataSource;

/**
 * Everything a host project gets by putting one line in its {@code build.gradle}.
 *
 * <p>Registered through {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports},
 * so a host does not add an annotation, an import or a {@code @ComponentScan} entry. Drop in the
 * dependency, call {@link NotifyPublisher}, and {@code GET /notify/stream} exists.
 *
 * <p><b>Why no {@code @ComponentScan} here.</b> Component scanning from an auto-configuration is the
 * thing Boot's own documentation tells you not to do: it runs during configuration class processing,
 * ahead of the condition evaluation everything else depends on, and it makes the module's beans appear
 * or not appear based on where the host happens to point its own scanner. Every bean below is declared
 * explicitly and guarded with {@code @ConditionalOnMissingBean}, which has a useful second effect - a
 * host that wants different behaviour declares its own {@link NotifyPublisher} or
 * {@link NotifyStreamRegistry} and this one steps aside, with no exclusion list and no profile.
 *
 * <p><b>Requires a {@code DataSource}.</b> The outbox table is the delivery mechanism, so a host with
 * no database has nothing to be lossless against and the module switches itself off rather than
 * degrading to a best-effort socket. {@code afterName} lists both the Boot 4 and Boot 3 locations of
 * {@code DataSourceAutoConfiguration} by string, so the condition is evaluated after the datasource
 * exists without compiling against either name - which matters because this module is meant to be
 * dropped into projects on whichever Boot version they already run.
 *
 * <p><b>Package layout caveat, and the only one.</b> A host whose {@code @SpringBootApplication} sits
 * at {@code net.jemsit} - or anywhere else that is an ancestor of this package - will component-scan
 * {@link NotifyStreamController} itself. That is harmless: the scan wins and the {@code @Bean} below
 * backs off. What is <em>not</em> harmless is such a host also declaring
 * {@code @EnableJpaRepositories("net.jemsit")}, which SimSim does. That is precisely why this module
 * persists through {@link NotifyStore} and {@link JdbcTemplate} instead of Spring Data repositories:
 * there are no repository interfaces for two registrars to claim.
 */
@AutoConfiguration(afterName = {
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(SseEmitter.class)
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(prefix = "notify", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(NotifyProperties.class)
public class NotifyAutoConfiguration {

    /**
     * A private {@link JdbcTemplate} over the host's datasource rather than injecting the host's own.
     *
     * <p>Two reasons. Depending on a {@code JdbcTemplate} bean would make this module stop working on a
     * host that never enabled {@code JdbcTemplateAutoConfiguration}, which is a strange thing to break
     * over. And building our own does not opt out of the host's transactions: {@code JdbcTemplate}
     * obtains connections through {@code DataSourceUtils}, which returns the connection already bound
     * to the current transaction when there is one. So a {@code send()} called inside a host's
     * {@code @Transactional} method still commits with it - which is what you want, because a
     * notification about a change that then rolls back should never have been sent.
     */
    @Bean
    @ConditionalOnMissingBean
    public NotifyStore notifyStore(DataSource dataSource,
                                   NotifyProperties properties,
                                   @Value("${notify.source:${spring.application.name:notify}}") String source) {
        // Resolved here rather than inside NotifyProperties, which cannot see other properties, and
        // rather than at each of the four places that read it, because a value that differed between
        // them would show up as one phone grouping the same backend under two names. This bean is the
        // root of the module's dependency chain, so the default is in place before any endpoint can
        // serve a request.
        properties.setSource(source.trim());
        return new NotifyStore(new JdbcTemplate(dataSource), properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public NotifyStreamRegistry notifyStreamRegistry(NotifyProperties properties, NotifyStore store) {
        return new NotifyStreamRegistry(properties, store);
    }

    @Bean
    @ConditionalOnMissingBean
    public NotifyStreamService notifyStreamService(NotifyStore store,
                                                   NotifyStreamRegistry streams,
                                                   NotifyProperties properties) {
        return new NotifyStreamService(store, streams, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public NotifyPublisher notifyPublisher(NotifyStore store,
                                           NotifyStreamRegistry streams,
                                           NotifyProperties properties) {
        return new NotifyPublisher(store, streams, properties);
    }

    /**
     * The controller, declared as a bean so a host with a narrow component scan still gets the
     * endpoints. {@code RequestMappingHandlerMapping} treats any bean whose class carries
     * {@code @RequestMapping} as a handler, whether or not it was found by scanning.
     */
    @Bean
    @ConditionalOnMissingBean
    public NotifyStreamController notifyStreamController(NotifyStreamService streamService,
                                                         NotifyPublisher publisher) {
        return new NotifyStreamController(streamService, publisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public NotifyTokenInterceptor notifyTokenInterceptor(NotifyProperties properties) {
        return new NotifyTokenInterceptor(properties);
    }

    /**
     * Registers the token interceptor over the module's own path prefix.
     *
     * <p>A {@code WebMvcConfigurer} bean is collected by Boot's {@code DelegatingWebMvcConfiguration}
     * alongside the host's own configurers rather than replacing them, so this adds a rule and changes
     * nothing else about how the host's MVC is set up. It also keeps working under a host that declares
     * {@code @EnableWebMvc}, which switches Boot's auto-configuration off but still injects every
     * configurer it can find.
     */
    @Bean
    public WebMvcConfigurer notifyWebMvcConfigurer(NotifyTokenInterceptor interceptor,
                                                   NotifyProperties properties) {
        String pattern = properties.getBasePath() + "/**";
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor).addPathPatterns(pattern);
            }
        };
    }
}
