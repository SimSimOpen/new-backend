package net.jemsit.simsim.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jemsit.notify.NotifyPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Performs the outbox insert, on this thread and therefore on its own connection.
 *
 * <p>{@code @Async} here is load-bearing, not a latency optimisation. {@code NotifyStore} writes
 * through a {@code JdbcTemplate}, and {@code JdbcTemplate} obtains its connection from
 * {@code DataSourceUtils}. {@code JpaTransactionManager} adopts the entity manager factory's
 * {@code DataSource} in {@code afterPropertiesSet} and then binds that transaction's own JDBC
 * connection to it in {@code doBegin}, so a publish made on a caller's thread inside a
 * {@code @Transactional} method runs inside the caller's transaction.
 *
 * <p>On Postgres that coupling is not survivable by catching. Any statement error puts the
 * transaction into an aborted state in which every later statement fails and {@code COMMIT} is
 * silently turned into {@code ROLLBACK}. So a failing outbox insert - missing table, revoked
 * privilege, dropped connection - would discard the realtor's committed-looking listing update while
 * the request still returned 200, and would leave {@code incrementViews} permanently stuck on the
 * milestone it could never advance past. Swallowing the exception in Java does not un-abort the
 * transaction. Moving the insert to another thread does: nothing is bound there, so it gets a fresh
 * autocommit connection and has no business transaction left to damage.
 *
 * <p>Nothing is rethrown. A failed publish means the operator missed a card, never that a listing
 * failed to go live.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OperatorFeedDispatcher {

    /**
     * Held as a provider rather than injected directly. With {@code notify.enabled=false} the module
     * contributes no beans at all, so a {@code private final NotifyPublisher} would be an
     * unsatisfiable constructor argument and the application would not start.
     */
    private final ObjectProvider<NotifyPublisher> publisherProvider;

    /** Whether the notify module is switched on and able to publish at all. */
    public boolean isActive() {
        return publisherProvider.getIfAvailable() != null;
    }

    @Async
    public void send(String title, String body, String tag, String priority) {
        NotifyPublisher publisher = publisherProvider.getIfAvailable();
        if (publisher == null) {
            return;
        }
        try {
            long id = publisher.send(title, body, tag, priority);
            log.debug("Operator feed published id={} tag={}", id, tag);
        } catch (RuntimeException e) {
            log.error("Operator feed failed to publish tag={}: {}", tag, e.toString());
        }
    }
}
