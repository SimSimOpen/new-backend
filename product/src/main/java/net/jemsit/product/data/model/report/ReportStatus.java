package net.jemsit.product.data.model.report;

/** Where a complaint stands. Only an admin moves it out of OPEN. */
public enum ReportStatus {
    OPEN,
    /** The admin agreed and acted on the listing. */
    RESOLVED,
    /** The admin looked and found nothing wrong. */
    DISMISSED
}
