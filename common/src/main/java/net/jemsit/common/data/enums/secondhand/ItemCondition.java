package net.jemsit.common.data.enums.secondhand;

/**
 * Physical condition of a second-hand listing. Distinct from {@code CarCondition} (NEW/USED only):
 * pre-loved goods need the extra gradations buyers filter on.
 */
public enum ItemCondition {
    NEW,
    LIKE_NEW,
    USED,
    FOR_PARTS,
}
