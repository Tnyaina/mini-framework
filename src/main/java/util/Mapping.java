package util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import annotation.Get;
import annotation.Post;

public class Mapping {
    private Class<?> controllerClass;
    private Method method;
    private String httpMethod; // Nouveau champ pour stocker la méthode HTTP
    
    // Constructeur avec httpMethod (nouveau)
    public Mapping(Class<?> controllerClass, Method method, String httpMethod) {
        this.controllerClass = controllerClass;
        this.method = method;
        this.httpMethod = httpMethod;
    }
    
    // Constructeur sans httpMethod (pour compatibilité)
    public Mapping(Class<?> controllerClass, Method method) {
        this.controllerClass = controllerClass;
        this.method = method;
        this.httpMethod = null;
    }
    
    public Class<?> getControllerClass() {
        return controllerClass;
    }
    
    public void setControllerClass(Class<?> controllerClass) {
        this.controllerClass = controllerClass;
    }
    
    public Method getMethod() {
        return method;
    }
    
    public void setMethod(Method method) {
        this.method = method;
    }
    
    public String getHttpMethod() {
        return httpMethod;
    }
    
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public static Map<String, Mapping> scanControllers() throws Exception {
        Map<String, Mapping> urlMappings = new HashMap<>();
        
        List<Class<?>> controllers = ControllerScanner.getAllControllers();
        System.out.println("Controleurs trouves: " + controllers.size());
        
        for (Class<?> controller : controllers) {
            System.out.println("Controleur: " + controller.getName());
            
            // Séparation explicite du scan GET et POST
            scanGetMethods(controller, urlMappings);
            scanPostMethods(controller, urlMappings);
        }
        
        System.out.println("Total mappings: " + urlMappings.size());
        return urlMappings;
    }
    
    /**
     * Scanner les méthodes GET du contrôleur
     */
    private static void scanGetMethods(Class<?> controller, Map<String, Mapping> urlMappings) {
        for (Method method : controller.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Get.class)) {
                String url = method.getAnnotation(Get.class).value();
                String key = buildMappingKey("GET", url);
                System.out.println("  GET " + url + " -> " + method.getName());
                urlMappings.put(key, new Mapping(controller, method, "GET"));
            }
        }
    }
    
    /**
     * Scanner les méthodes POST du contrôleur
     */
    private static void scanPostMethods(Class<?> controller, Map<String, Mapping> urlMappings) {
        for (Method method : controller.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Post.class)) {
                String url = method.getAnnotation(Post.class).value();
                String key = buildMappingKey("POST", url);
                System.out.println("  POST " + url + " -> " + method.getName());
                urlMappings.put(key, new Mapping(controller, method, "POST"));
            }
        }
    }
    
    /**
     * Construire la clé de mapping avec la méthode HTTP et l'URL
     */
    private static String buildMappingKey(String httpMethod, String url) {
        return httpMethod + ":" + url;
    }
}