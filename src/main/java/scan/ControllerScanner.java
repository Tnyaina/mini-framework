package scan;

import annotation.Controller;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ControllerScanner {
    
    /**
     * Scanne TOUT le projet pour trouver les classes annotées @Controller
     */
    public static List<Class<?>> getAllControllers() throws Exception {
        List<Class<?>> controllers = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        // Récupérer tous les répertoires du classpath
        Enumeration<URL> resources = classLoader.getResources("");
        
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            File directory = new File(resource.getFile());
            
            if (directory.exists()) {
                scanDirectory(directory, "", controllers);
            }
        }
        
        return controllers;
    }
    
    /**
     * Scanne récursivement un répertoire
     */
    private static void scanDirectory(File directory, String packageName, List<Class<?>> controllers) {
        File[] files = directory.listFiles();
        
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Sous-dossier : scanner récursivement
                    String subPackage = packageName.isEmpty() 
                        ? file.getName() 
                        : packageName + "." + file.getName();
                    scanDirectory(file, subPackage, controllers);
                    
                } else if (file.getName().endsWith(".class")) {
                    // Fichier .class : vérifier l'annotation
                    String className = packageName.isEmpty()
                        ? file.getName().replace(".class", "")
                        : packageName + "." + file.getName().replace(".class", "");
                    
                    try {
                        Class<?> clazz = Class.forName(className);
                        if (clazz.isAnnotationPresent(Controller.class)) {
                            controllers.add(clazz);
                        }
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        // Ignorer les classes qui ne peuvent pas être chargées
                    }
                }
            }
        }
    }
}