package net.jemsit.product.mapper;

import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Picks the location name to show for the request's Accept-Language. Russian gets the Russian name;
 * Uzbek and English both get the Uzbek (Latin) name, since there are no English names in the data.
 */
public final class LocationNames {

    private LocationNames() {
    }

    /** "ru" or "uz", for queries that pick the name column themselves. */
    public static String currentLang() {
        return "ru".equals(LocaleContextHolder.getLocale().getLanguage()) ? "ru" : "uz";
    }

    public static String pick(String nameRu, String nameUz) {
        String preferred = "ru".equals(currentLang()) ? nameRu : nameUz;
        String fallback = "ru".equals(currentLang()) ? nameUz : nameRu;
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }
}
