package net.jemsit.common.api;

import net.jemsit.common.data.enums.Roles;

import java.math.BigDecimal;

/**
 * The operator's own notification feed, alongside the per-user SSE feed in the {@code notification}
 * module. Nothing here replaces or feeds that one; a caller that already notifies a user does not need
 * to change, and a caller that wants the operator to know adds one line.
 *
 * <p>Every method is fire-and-forget and must never throw. The feed is an observer: if it cannot
 * publish, the operation that triggered it still succeeded and the caller has no way to compensate.
 * Implementations log and swallow.
 *
 * <p>Implemented in the root shell rather than in a feature module, because the shell is the only
 * place that can see the {@code notify} module. {@code product} and {@code auth} depend on this
 * interface and nothing else, which is what keeps that module optional and the application bootable
 * when it is switched off.
 */
public interface OperatorFeedApi {

    /**
     * A listing became publicly visible for the first time. Callers decide what "went live" means and
     * only call this on the transition, not on every save.
     *
     * @param title, price, agent already-loaded values from the saved entity; passing them in avoids a
     *        second read, and all three are optional
     */
    void listingWentLive(Long propertyId, String title, BigDecimal price, String agent);

    /**
     * A listing was viewed. Called on every view; whether this count is worth telling the operator
     * about is the implementation's decision, not the caller's.
     *
     * @param kind the listing family ("property", "car"); the tables number their rows independently,
     *        so the kind keeps one listing's notification tag from colliding with another family's
     * @param listingId the viewed listing's id
     * @param views the count after the increment
     * @param title looked up by the caller, which owns the repository; may be null
     */
    void viewMilestone(String kind, Long listingId, long views, String title);

    /**
     * Whether {@link #viewMilestone} would produce a card for this count. Exists so the caller can skip
     * the title lookup on the views that produce nothing - the counter is a public, unauthenticated
     * route and most calls to it are not milestones. The cadence rule still lives in one place, the
     * implementation; this only exposes its answer. Always false when the feed is off.
     */
    boolean isViewMilestone(long views);

    /**
     * A new account was created, by whatever route created it.
     *
     * @param username the account's identifier, which on the OTP route is a phone number. Deliberately
     *        the username and never the email: this text ends up on a lockscreen and in the outbox.
     */
    void accountRegistered(Roles role, String username);
}
