package net.jemsit.notify;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Everything a host project might want to change, with defaults that are correct for a phone on a LAN
 * and for a phone on cellular behind carrier NAT.
 *
 * <p>Every value is overridable from the host's own application.yml under {@code notify.*}. Nothing
 * here is required, so a host that adds the dependency and writes no configuration at all gets a
 * working endpoint at {@code /notify/stream}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "notify")
public class NotifyProperties {

    /** Master switch. Lets a host keep the dependency on its classpath - for a shared build - while
     *  running the endpoint in only some of its environments. */
    private boolean enabled = true;

    /** Prefix for every endpoint this module exposes. Change it only if a host already owns
     *  {@code /notify}. */
    private String basePath = "/notify";

    /**
     * Name stamped on every notification this backend publishes, so a phone listening to four
     * projects can say which one a card came from and group its inbox accordingly.
     *
     * <p>Left empty by default, which means the auto-configuration substitutes
     * {@code spring.application.name} - the right answer almost always, since that is already the name
     * the host uses for its logs and its actuator identity. Set it explicitly only when a single
     * application publishes on behalf of several logical sources, and pass {@code source} per call.
     */
    private String source = "";

    /**
     * Shared secret required on every endpoint this module exposes. Empty means no authentication,
     * which is acceptable for a backend on a LAN and unacceptable for one with a public hostname.
     *
     * <p>Worth being blunt about what an unguarded instance allows. {@code POST /notify/publish} lets
     * anyone on the internet put arbitrary text into the notification shade of every phone that has
     * ever connected - which is a phishing channel handed to strangers, since the card arrives from an
     * app the user installed and trusts. {@code GET /notify/devices} hands out the device ids and
     * labels. {@code GET /notify/stream} lets anyone read every notification the backend produces.
     *
     * <p>Set this to any long random string on anything reachable from outside. Clients send it as
     * {@code Authorization: Bearer <token>} or as a {@code token} query parameter, the latter because
     * some HTTP clients cannot set headers on an SSE connection. The auto-configuration logs a warning
     * at startup when it is empty.
     */
    private String token = "";

    /**
     * How far back a reconnecting device is allowed to reach. This is the setting that decides what a
     * user sees after their phone was off for a week, and 24 hours is the answer that does not make
     * someone mute the app: everything older than that is stale by definition, and dumping a week of
     * "build failed" cards into the notification shade is how a notification app gets silenced
     * permanently.
     *
     * <p>Enforced as a predicate on the replay query, not by a rewrite job, so an uncollected row is
     * never handed out even if the sweep has not run yet.
     */
    private long maxAgeMs = 86_400_000L;

    /**
     * Ceiling on rows replayed to one device in a single reconnect. Bounds both the response size and
     * the number of cursor acknowledgements the phone will fire afterwards. A device that was away
     * long enough to exceed this simply reconnects again and continues from where it got to.
     */
    private int maxReplay = 50;

    /**
     * Interval between heartbeat events on an idle stream. Two jobs, and both matter:
     * <ul>
     *   <li>It proves the socket is still live. A connection dropped by carrier NAT or a router looks
     *       exactly like a healthy idle stream until a write fails, and without a heartbeat that
     *       discovery takes as long as the next notification does.</li>
     *   <li>It keeps the NAT mapping from expiring in the first place. 20s sits inside the 30s idle
     *       timeout a cheap router typically applies, and inside the 45s the existing SimSim SSE
     *       client already tolerates.</li>
     * </ul>
     * Do not raise this past about 30s without also checking the router.
     */
    private long heartbeatMs = 20_000L;

    /**
     * Whether the module creates its own two tables at startup with
     * {@code create table if not exists}.
     *
     * <p>On by default, because "add one dependency line and it works" is the entire proposition and a
     * module that needs a migration written before it will start is a module nobody installs. The
     * statement is idempotent and additive: it never alters a table and never touches one it did not
     * create.
     *
     * <p>Turn it off on any host whose schema is under Flyway or Liquibase control, where an
     * application creating tables behind the migration tool's back is exactly the surprise that tool
     * exists to prevent. Apply {@code notify-schema.sql} from this module's resources as a versioned
     * migration instead; it carries the same DDL, duplicated deliberately so there is a file to review
     * rather than a statement to dig out of a Java class.
     */
    private boolean createSchema = true;

    /**
     * How long a row stays in the outbox before the retention sweep deletes it. Must exceed
     * {@link #maxAgeMs} by a comfortable margin, otherwise the sweep can delete rows a device would
     * still have been allowed to replay, and a long-offline phone silently loses notifications it
     * should have received.
     */
    private long retentionHours = 168;

    /** How often the retention sweep runs. Deliberately an hour: it is a bounded delete over an
     *  indexed column and there is no benefit to running it more often. */
    private long retentionSweepMs = 3_600_000L;

    /** Truncation limits, so a producer that logs a 400KB stack trace into a body cannot write a
     *  400KB row and then push 400KB down every connected socket. */
    private int titleMaxLength = 200;
    private int bodyMaxLength = 2000;
}
