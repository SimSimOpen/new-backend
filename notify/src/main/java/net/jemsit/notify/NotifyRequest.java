package net.jemsit.notify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * What a caller supplies when publishing. Deliberately separate from {@link NotifyMessage}, which is
 * what the phone receives: this one has no id and no timestamp because the module assigns both, and
 * pretending otherwise would invite a host to invent an id that then collides with the cursor space.
 *
 * <p>Every field but {@code title} is optional. {@code tag} and {@code priority} default inside
 * {@link NotifyPublisher}; a null {@code source} falls back to {@code notify.source}, which itself
 * defaults to the host's {@code spring.application.name}.
 *
 * <p>{@code ignoreUnknown} is here because this record is also the body of {@code POST /notify/publish},
 * and a curl one-liner typed from memory should not fail because it carried an extra key.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NotifyRequest(String title,
                            String body,
                            String tag,
                            String priority,
                            String source) {

    public static NotifyRequest of(String title, String body) {
        return new NotifyRequest(title, body, null, null, null);
    }
}
