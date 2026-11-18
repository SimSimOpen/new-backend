package net.jemsit.product.service;

import lombok.RequiredArgsConstructor;
import net.jemsit.common.UserContext;
import net.jemsit.common.api.AuthApi;
import net.jemsit.common.data.enums.Roles;
import net.jemsit.common.exceptions.UserException;
import org.springframework.stereotype.Component;

/**
 * Decides who owns a new listing draft. Normally that is the caller; an admin may instead pass the
 * id of an existing agent so the listing is created in that agent's name.
 */
@Component
@RequiredArgsConstructor
public class ListingOwnerResolver {

    private final AuthApi authApi;

    public Long resolve(Long requestedAgentId) {
        Long callerId = UserContext.getUserId();
        if (requestedAgentId == null || requestedAgentId.equals(callerId)) {
            return callerId;
        }
        var callerRoles = UserContext.getRoles();
        if (callerRoles == null || !callerRoles.contains(Roles.ADMIN)) {
            throw new UserException("Only an admin can create a listing for another agent");
        }
        var target = authApi.getById(requestedAgentId);
        var roles = target.roles();
        if (roles == null || !(roles.contains(Roles.AGENT) || roles.contains(Roles.ADMIN))) {
            throw new UserException("User " + requestedAgentId + " is not an agent");
        }
        return target.id();
    }
}
