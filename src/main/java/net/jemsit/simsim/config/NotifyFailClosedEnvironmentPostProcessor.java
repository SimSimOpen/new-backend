package net.jemsit.simsim.config;

import org.apache.commons.logging.Log;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps "fail closed until the env is set" a property of the code rather than of a runbook.
 *
 * <p>{@code notify.enabled=true} with a blank {@code notify.token} leaves every {@code /notify}
 * endpoint unauthenticated, including {@code POST /notify/publish}. On a public hostname that is an
 * open relay to any phone configured to trust this app. The module warns about it but does not refuse,
 * by design: it is meant to be zero-config on a LAN, and this host is not on a LAN.
 *
 * <p>So the combination is resolved here, before the context exists, by forcing the module back off.
 * It degrades to inert rather than throwing, which means a misconfiguration costs the operator their
 * notifications and nothing else - it cannot take the application down.
 *
 * <p>Runs last among environment post-processors so it sees {@code application.properties} and the
 * container's env vars already merged into the environment.
 */
public class NotifyFailClosedEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /**
     * Deferred because at this point the logging system has not been configured yet; Boot replays
     * these once the environment can set levels, so the message lands in the normal application log
     * rather than only on stderr.
     */
    private final Log log;

    public NotifyFailClosedEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(NotifyFailClosedEnvironmentPostProcessor.class);
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!"true".equalsIgnoreCase(environment.getProperty("notify.enabled", "false"))) {
            return;
        }
        String token = environment.getProperty("notify.token", "");
        if (token != null && !token.isBlank()) {
            return;
        }
        Map<String, Object> off = new HashMap<>();
        off.put("notify.enabled", "false");
        // addFirst so this wins over both application.properties and the NOTIFY_ENABLED env var
        environment.getPropertySources().addFirst(new MapPropertySource("notifyFailClosed", off));
        log.error("NOTIFY_ENABLED=true but NOTIFY_TOKEN is blank; forcing notify.enabled=false rather "
                + "than exposing POST /notify/publish unauthenticated. Add NOTIFY_TOKEN to the ENV_FILE "
                + "repository secret and redeploy.");
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
