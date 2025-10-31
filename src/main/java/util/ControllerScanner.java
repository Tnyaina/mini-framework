package util;

import annotation.Controller;
import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ControllerScanner {
    
    public static List<Class<?>> getAllControllers() throws Exception {
        List<Class<?>> controllers = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        Enumeration<URL> resources = classLoader.getResources("");
        
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            
            if (resource.getProtocol().equals("file")) {
                
                String path = URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8);
                File directory = new File(path);
                
                if (directory.exists()) {
                    scanDirectory(directory, "", controllers);
                }
            }
        }
        
        return controllers;
    }
    
    private static void scanDirectory(File directory, String packageName, List<Class<?>> controllers) {
        File[] files = directory.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                String subPackage = packageName.isEmpty() 
                    ? file.getName() 
                    : packageName + "." + file.getName();
                scanDirectory(file, subPackage, controllers);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName.isEmpty()
                    ? file.getName().replace(".class", "")
                    : packageName + "." + file.getName().replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(Controller.class)) {
                        controllers.add(clazz);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // Ignorer
                }
            }
        }
    }
}