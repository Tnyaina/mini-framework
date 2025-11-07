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
    
    public Mapping(Class<?> controllerClass, Method method) {
        this.controllerClass = controllerClass;
        this.method = method;
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

    public static Map<String, Mapping> scanControllers() throws Exception {
        Map<String, Mapping> urlMappings = new HashMap<>();
        
        List<Class<?>> controllers = ControllerScanner.getAllControllers();
        System.out.println("Controleurs trouves: " + controllers.size());
        
        for (Class<?> controller : controllers) {
            System.out.println("Controleur: " + controller.getName());
            for (Method method : controller.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Get.class)) {
                    String url = method.getAnnotation(Get.class).value();
                    System.out.println("  GET " + url + " -> " + method.getName());
                    urlMappings.put("GET:" + url, new Mapping(controller, method));
                }
                if (method.isAnnotationPresent(Post.class)) {
                    String url = method.getAnnotation(Post.class).value();
                    System.out.println("  POST " + url + " -> " + method.getName());
                    urlMappings.put("POST:" + url, new Mapping(controller, method));
                }
            }
        }
        System.out.println("Total mappings: " + urlMappings.size());
        
        return urlMappings;
    }
}