package net.jemsit.notify;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * The module's entire HTTP surface: five endpoints.
 *
 * <p>The path prefix is a placeholder so a host that already owns {@code /notify} can move the module
 * with one property. Spring resolves placeholders in mapping paths against the environment, which is
 * why this needs no custom {@code PathMatchConfigurer}.
 *
 * <ul>
 *   <li>{@code GET /stream} - the phone's connection. Long-lived, text/event-stream.</li>
 *   <li>{@code POST /cursor} - the phone reporting how far it got. Called after every notification and
 *       after every sync, which is what makes the resume survive the app being killed.</li>
 *   <li>{@code GET /health} - one call the phone makes to render a source card's status dot.</li>
 *   <li>{@code GET /devices} - for a human, from a browser, when a notification did not arrive.</li>
 *   <li>{@code POST /publish} - so anything that can speak HTTP can notify the phone, not only Java
 *       code running inside this JVM. A shell script, a cron job, a CI step, a Grafana webhook.</li>
 * </ul>
 */
@RestController
@RequestMapping("${notify.base-path:/notify}")
@RequiredArgsConstructor
@Slf4j
public class NotifyStreamController {

    private final NotifyStreamService streamService;
    private final NotifyPublisher publisher;

    /**
     * Opens the stream.
     *
     * <p>Three ways to say where to resume from, in priority order: the {@code since} parameter, the
     * standard {@code Last-Event-ID} header, and the cursor stored server-side for this device id. The
     * header is supported because it is the mechanism every off-the-shelf SSE client already
     * implements, so a browser pointed at this URL resumes correctly with no JavaScript at all.
     *
     * <p>{@code deviceId} is required and caller-supplied rather than derived from a session, because
     * the caller is a phone that reconnects from a fresh process with no cookies and no auth context.
     * It is an identity for cursor bookkeeping, not a credential - the token is the credential.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String deviceId,
                            @RequestParam(required = false) String label,
                            @RequestParam(required = false) Long since,
                            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        return streamService.openStream(deviceId.trim(), label, since, lastEventId);
    }

    @PostMapping("/cursor")
    public Map<String, Object> cursor(@RequestParam String deviceId,
                                     @RequestBody CursorRequest request) {
        long stored = streamService.recordCursor(deviceId.trim(), request.cursor(), request.label());
        // Echoing the stored value back rather than a bare ok lets a client notice it is behind its own
        // view, which happens when two processes share a device id or when a stale ack arrived late.
        return Map.of("deviceId", deviceId.trim(), "cursor", stored);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return streamService.health();
    }

    @GetMapping("/devices")
    public List<Map<String, Object>> devices() {
        return streamService.deviceList();
    }

    @PostMapping("/publish")
    public Map<String, Object> publish(@RequestBody NotifyRequest request) {
        long id = publisher.send(request);
        return Map.of("id", id, "queued", true);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CursorRequest(long cursor, String label) {
    }
}
