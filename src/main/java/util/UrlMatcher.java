package util;

import java.util.HashMap;
import java.util.Map;

public class UrlMatcher {
    
    public static boolean matches(String pattern, String url) {
        String[] patternSegments = pattern.split("/");
        String[] urlSegments = url.split("/");
        
        if (patternSegments.length != urlSegments.length) {
            return false;
        }
        
        for (int i = 0; i < patternSegments.length; i++) {
            String patternSeg = patternSegments[i];
            String urlSeg = urlSegments[i];
            
            if (patternSeg.startsWith("{") && patternSeg.endsWith("}")) {
                continue;
            }
            
            if (!patternSeg.equals(urlSeg)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Extrait les variables d'URL depuis un pattern et une URL réelle
     * Ex: pattern="/emp/{id}/details", url="/emp/21/details" -> {"id": "21"}
     */
    public static Map<String, String> extractPathVariables(String pattern, String url) {
        Map<String, String> variables = new HashMap<>();
        
        String[] patternSegments = pattern.split("/");
        String[] urlSegments = url.split("/");
        
        if (patternSegments.length != urlSegments.length) {
            return variables;
        }
        
        for (int i = 0; i < patternSegments.length; i++) {
            String patternSeg = patternSegments[i];
            
            if (patternSeg.startsWith("{") && patternSeg.endsWith("}")) {
                String variableName = patternSeg.substring(1, patternSeg.length() - 1);
                variables.put(variableName, urlSegments[i]);
            }
        }
        
        return variables;
    }
}