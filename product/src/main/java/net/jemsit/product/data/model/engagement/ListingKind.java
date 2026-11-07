package net.jemsit.product.data.model.engagement;

/** The listing tables a visitor can save or share. Each constant names its own table for reporting SQL. */
public enum ListingKind {
    PROPERTY("properties"),
    CAR("cars"),
    SECOND_HAND("second_hand_items"),
    SERVICE("service_listings");

    private final String table;

    ListingKind(String table) {
        this.table = table;
    }

    public String table() {
        return table;
    }
}
