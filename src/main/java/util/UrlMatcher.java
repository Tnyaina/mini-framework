package util;

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
            
            // Si c'est une variable {xxx}, ça match toujours
            if (patternSeg.startsWith("{") && patternSeg.endsWith("}")) {
                continue;
            }
            
            // Sinon, comparaison stricte
            if (!patternSeg.equals(urlSeg)) {
                return false;
            }
        }
        
        return true;
    }
}