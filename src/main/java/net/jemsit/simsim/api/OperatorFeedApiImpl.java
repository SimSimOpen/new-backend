package net.jemsit.simsim.api;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jemsit.common.api.OperatorFeedApi;
import net.jemsit.common.data.enums.Roles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

/**
 * Decides what each operator card says, and when it is allowed to be written.
 *
 * <p>Publishing is deferred to after the caller's transaction commits and then handed to
 * {@link OperatorFeedDispatcher}, which explains why. The two properties that ordering buys:
 * <ul>
 *   <li>A listing update that rolls back announces nothing, because {@code afterCommit} never runs.
 *       Hibernate flushes at commit, so a constraint violation surfaces after the hook has already
 *       been reached - without the deferral that would produce a card for a change that never
 *       landed.</li>
 *   <li>A failing outbox insert cannot damage the caller's write. Inside a transaction the insert
 *       would share its connection, and on Postgres a statement error aborts the whole transaction:
 *       catching the exception would still leave {@code COMMIT} to become {@code ROLLBACK}, silently
 *       discarding a realtor's update behind a 200 response.</li>
 * </ul>
 *
 * <p>Nothing is rethrown. See {@link OperatorFeedApi} for why: a failed publish means the operator
 * missed a card, not that the listing failed to go live.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OperatorFeedApiImpl implements OperatorFeedApi {

    /**
     * One card per listing per this many views, plus the very first view. Kept here rather than at the
     * call site so the cadence is set once and {@code incrementViews} stays a single line.
     */
    private static final int VIEW_MILESTONE = 1;

    private final OperatorFeedDispatcher dispatcher;

    @Value("${notify.token:}")
    private String notifyToken;

    @PostConstruct
    void reportState() {
        if (!dispatcher.isActive()) {
            log.info("Operator feed inactive: notify.enabled is false, no NotifyPublisher bean");
            return;
        }
        // The module warns about a blank token but does not refuse one - it is meant to be zero-config
        // on a LAN. This host is not on a LAN, and NotifyFailClosedEnvironmentPostProcessor should have
        // forced the module off before getting here, so reaching this line means something bypassed it.
        if (notifyToken == null || notifyToken.isBlank()) {
            log.error("Operator feed is ACTIVE WITH NO TOKEN: every /notify endpoint is "
                    + "unauthenticated, including POST /notify/publish. Set NOTIFY_TOKEN.");
            return;
        }
        log.info("Operator feed active: listing-goes-live, view milestones every {} views, new accounts",
                VIEW_MILESTONE);
    }

    @Override
    public void listingWentLive(Long propertyId, String title, BigDecimal price, String agent) {
        publish(listingName(propertyId, title) + " is live",
                describeListing(price, agent),
                "listing-" + propertyId,
                "normal");
    }

    @Override
    public void viewMilestone(String kind, Long listingId, long views, String title) {
        if (!isViewMilestone(views)) {
            return;
        }
        String family = kind != null && !kind.isBlank() ? kind.trim().toLowerCase() + "-" : "";
        publish(listingName(listingId, title) + " - " + views + (views == 1L ? " view" : " views"),
                views == 1L ? "First view" : "Passed " + views + " views",
                "views-" + family + listingId,
                "low");
    }

    @Override
    public boolean isViewMilestone(long views) {
        return dispatcher.isActive() && (views == 1L || views % VIEW_MILESTONE == 0L);
    }

    @Override
    public void accountRegistered(Roles role, String username) {
        String roleName = (role != null ? role.name() : "user").toLowerCase();
        // Per-account rather than per-role: the phone sets onlyAlertOnce, so a role-level tag would buzz
        // on the first signup ever and then update that one card silently for every later account.
        String suffix = username != null && !username.isBlank() ? username.trim() : roleName;
        publish("New " + roleName + " account",
                username != null && !username.isBlank() ? username.trim() : null,
                "account-" + suffix,
                "high");
    }

    private void publish(String title, String body, String tag, String priority) {
        if (!dispatcher.isActive()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatcher.send(title, body, tag, priority);
                }
            });
            return;
        }
        dispatcher.send(title, body, tag, priority);
    }

    /** The title column is NOT NULL and the module rejects a blank one, so always have something. */
    private static String listingName(Long propertyId, String title) {
        return title != null && !title.isBlank() ? title.trim() : "Listing " + propertyId;
    }

    private static String describeListing(BigDecimal price, String agent) {
        StringBuilder body = new StringBuilder();
        if (price != null) {
            body.append(price.stripTrailingZeros().toPlainString());
        }
        if (agent != null && !agent.isBlank()) {
            if (!body.isEmpty()) {
                body.append(" - ");
            }
            body.append("agent ").append(agent.trim());
        }
        return body.isEmpty() ? null : body.toString();
    }
}
