package net.jemsit.product.data.model.report;

/** Why a user reported a listing. */
public enum ReportReason {
    /** Not a real offer, or photos that are not of this item. */
    FAKE,
    /** Asks for prepayment, phishing, or another attempt to cheat. */
    SCAM,
    /** Wrong price, address, category or description. */
    WRONG_INFO,
    /** Already sold or rented, or the seller cannot be reached. */
    UNAVAILABLE,
    /** Offensive, illegal or adult content. */
    INAPPROPRIATE,
    OTHER
}
