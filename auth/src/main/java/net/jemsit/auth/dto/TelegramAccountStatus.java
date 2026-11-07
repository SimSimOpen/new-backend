package net.jemsit.auth.dto;

/** What a Telegram username points at, as far as its public t.me page reveals. */
public enum TelegramAccountStatus {
    /** A person's account: clients can open a chat, unless its privacy settings block strangers. */
    PERSONAL,
    /** Clients can only read a channel, they cannot message the agent. */
    CHANNEL,
    GROUP,
    BOT,
    /** Nothing is registered under this username. */
    NOT_FOUND,
    /** t.me could not be reached or returned a page we do not recognise. */
    UNKNOWN
}
