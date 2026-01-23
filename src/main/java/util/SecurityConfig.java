package util;

import java.io.InputStream;
import java.util.Properties;

public class SecurityConfig {
    private static String userKey = "currentUser";  // défaut
    private static String roleKey = "userRole";     // défaut
    
    public static void load() {
        try {
            InputStream input = SecurityConfig.class.getClassLoader()
                .getResourceAsStream("framework.properties");
            
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                
                userKey = props.getProperty("security.user.key", "currentUser");
                roleKey = props.getProperty("security.role.key", "userRole");
                
                System.out.println("SecurityConfig loaded: userKey=" + userKey + ", roleKey=" + roleKey);
            } else {
                System.out.println("framework.properties not found, using defaults");
            }
        } catch (Exception e) {
            System.err.println("Error loading framework.properties: " + e.getMessage());
        }
    }
    
    public static String getUserKey() {
        return userKey;
    }
    
    public static String getRoleKey() {
        return roleKey;
    }
}