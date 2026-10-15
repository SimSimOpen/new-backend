package net.jemsit.product.service.category;

/**
 * Counts the publicly visible listings of one marketplace category. Each category that has its own
 * listings (properties today; cars, services, ... later) contributes one bean, so the category
 * service never needs to know about the tables behind them.
 */
public interface CategoryListingCounter {
    String categorySlug();

    long countPublished();
}
