package net.jemsit.product.mapper;

import lombok.RequiredArgsConstructor;
import net.jemsit.common.api.AuthApi;
import org.springframework.stereotype.Component;

/**
 * The phone number shown on a listing. It is never typed into the listing: it is read from the
 * account of the agent who owns it, so the call button always reaches that agent's current number.
 */
@Component
@RequiredArgsConstructor
public class AgentPhones {

    private final AuthApi authApi;

    public String of(Long agentId) {
        return authApi.phoneNumber(agentId);
    }
}
