package util;

import annotation.AuthRequired;
import annotation.RequireRole;
import annotation.AllowAnonymous;
import java.lang.reflect.Method;
import java.util.Map;

public class SecurityChecker {
    
    public static class SecurityResult {
        private boolean allowed;
        private int statusCode;
        private String message;
        
        public SecurityResult(boolean allowed, int statusCode, String message) {
            this.allowed = allowed;
            this.statusCode = statusCode;
            this.message = message;
        }
        
        public boolean isAllowed() { return allowed; }
        public int getStatusCode() { return statusCode; }
        public String getMessage() { return message; }
        
        public static SecurityResult allow() {
            return new SecurityResult(true, 200, "OK");
        }
        
        public static SecurityResult deny(int status, String msg) {
            return new SecurityResult(false, status, msg);
        }
    }
    
    /**
     * Vérifie si l'utilisateur peut accéder à cette méthode
     */
    public static SecurityResult checkAccess(Method method, Map<String, Object> sessionMap) {
        // 1. Si @AllowAnonymous → accès libre
        if (method.isAnnotationPresent(AllowAnonymous.class)) {
            return SecurityResult.allow();
        }
        
        // 2. Si @RequireRole → vérifier auth + role
        if (method.isAnnotationPresent(RequireRole.class)) {
            String requiredRole = method.getAnnotation(RequireRole.class).value();
            return checkRole(sessionMap, requiredRole);
        }
        
        // 3. Si @AuthRequired → vérifier auth seulement
        if (method.isAnnotationPresent(AuthRequired.class)) {
            return checkAuthentication(sessionMap);
        }
        
        // 4. Sans annotation → accès libre
        return SecurityResult.allow();
    }
    
    /**
     * Vérifie si l'utilisateur est authentifié
     */
    private static SecurityResult checkAuthentication(Map<String, Object> sessionMap) {
        String userKey = SecurityConfig.getUserKey();
        Object user = sessionMap.get(userKey);
        
        if (user == null) {
            return SecurityResult.deny(401, "Authentication required");
        }
        
        return SecurityResult.allow();
    }
    
    /**
     * Vérifie si l'utilisateur a le bon rôle
     */
    private static SecurityResult checkRole(Map<String, Object> sessionMap, String requiredRole) {
        String userKey = SecurityConfig.getUserKey();
        String roleKey = SecurityConfig.getRoleKey();
        
        Object user = sessionMap.get(userKey);
        if (user == null) {
            return SecurityResult.deny(401, "Authentication required");
        }
        
        Object userRole = sessionMap.get(roleKey);
        if (userRole == null || !requiredRole.equals(userRole.toString())) {
            return SecurityResult.deny(403, "Access denied. Required role: " + requiredRole);
        }
        
        return SecurityResult.allow();
    }
}