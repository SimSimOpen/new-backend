package net.jemsit.notification.telegram;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

/**
 * Pulls bot updates with long polling, so it works on localhost and needs no public webhook URL.
 * Telegram allows only one poller per bot token: run a single backend instance per token.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TelegramPoller {

    private static final int POLL_TIMEOUT_SECONDS = 25;

    private final TelegramBotClient bot;
    private final TelegramOtpService otpService;
    private volatile boolean running;
    private boolean conflictReported;
    private Thread thread;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!bot.isConfigured()) {
            log.info("telegram.bot-token / telegram.bot-username not set: Telegram login codes are disabled");
            return;
        }
        running = true;
        thread = Thread.ofVirtual().name("telegram-poller").start(this::loop);
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void loop() {
        long offset = 0;
        try {
            bot.deleteWebhook();
        } catch (Exception e) {
            log.warn("Could not clear Telegram webhook: {}", e.getMessage());
        }
        log.info("Telegram poller started");
        while (running) {
            try {
                List<TelegramBotClient.Update> updates = bot.getUpdates(offset, POLL_TIMEOUT_SECONDS);
                conflictReported = false;
                for (TelegramBotClient.Update update : updates) {
                    offset = update.updateId() + 1;
                    try {
                        otpService.handle(update.message());
                    } catch (Exception e) {
                        log.error("Failed to handle Telegram update {}: {}", update.updateId(), e.getMessage());
                    }
                }
            } catch (HttpClientErrorException.Conflict e) {
                // Another process polls the same bot token and receives (and acts on) some of the updates,
                // so /start links and shared contacts end up in the other instance's Redis
                if (!conflictReported) {
                    log.error("Telegram bot token is being polled by ANOTHER instance (409 Conflict). Updates are "
                            + "split between them, so login codes will not arrive reliably. Use a separate bot "
                            + "for each environment.");
                    conflictReported = true;
                }
                if (!pause()) {
                    break;
                }
            } catch (Exception e) {
                if (!running) {
                    break;
                }
                log.warn("Telegram polling failed, retrying in 5s: {}", e.getMessage());
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }
        log.info("Telegram poller stopped");
    }

    private boolean pause() {
        try {
            Thread.sleep(5_000);
            return true;
        } catch (InterruptedException ie) {
            return false;
        }
    }
}
