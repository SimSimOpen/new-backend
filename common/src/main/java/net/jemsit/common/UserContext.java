package net.jemsit.common;


import net.jemsit.common.data.enums.Roles;

import java.util.List;

public class UserContext {
    public static final ThreadLocal<List<Roles>> userRole = new ThreadLocal<>();
    public static final ThreadLocal<String> userToken = new ThreadLocal<>(); //be careful with this token, it can be User token or Agent token, it is  not always Agent Token
    public static final ThreadLocal<Long> userId = new ThreadLocal<>(); //be careful with this token, it can be User id or Agent id, it is  not always Agent id
    public static void userRole(List<Roles> roles) {
        userRole.set(roles);
    }
    public static List<Roles> getRoles() {
        return userRole.get();
    }
    public static void setUserToken(String token) {
        userToken.set(token);
    }
    public static String getUserToken() {
        return userToken.get();
    }
    public static void setUserId(Long id) {userId.set(id);}
    public static Long getUserId() {return userId.get();}

    public static void clear() {
        userToken.remove();
    }
}
