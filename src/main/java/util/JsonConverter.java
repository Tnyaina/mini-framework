package util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

public class JsonConverter {
    
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        Class<?> clazz = obj.getClass();
        
        // Primitives et String
        if (clazz == String.class) {
            return "\"" + escapeJson(obj.toString()) + "\"";
        }
        if (clazz == Integer.class || clazz == Long.class || 
            clazz == Double.class || clazz == Float.class || 
            clazz == Boolean.class || clazz.isPrimitive()) {
            return obj.toString();
        }
        
        // Tableaux
        if (clazz.isArray()) {
            return arrayToJson(obj);
        }
        
        // Collections
        if (obj instanceof Collection) {
            return collectionToJson((Collection<?>) obj);
        }
        
        // Map
        if (obj instanceof Map) {
            return mapToJson((Map<?, ?>) obj);
        }
        
        // Objets custom
        return objectToJson(obj);
    }
    
    private static String objectToJson(Object obj) {
        StringBuilder json = new StringBuilder("{");
        Field[] fields = obj.getClass().getDeclaredFields();
        boolean first = true;
        
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                if (value != null) {
                    if (!first) json.append(",");
                    json.append("\"").append(field.getName()).append("\":");
                    json.append(toJson(value));
                    first = false;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        
        json.append("}");
        return json.toString();
    }
    
    private static String arrayToJson(Object array) {
        StringBuilder json = new StringBuilder("[");
        int length = Array.getLength(array);
        
        for (int i = 0; i < length; i++) {
            if (i > 0) json.append(",");
            json.append(toJson(Array.get(array, i)));
        }
        
        json.append("]");
        return json.toString();
    }
    
    private static String collectionToJson(Collection<?> collection) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        
        for (Object item : collection) {
            if (!first) json.append(",");
            json.append(toJson(item));
            first = false;
        }
        
        json.append("]");
        return json.toString();
    }
    
    private static String mapToJson(Map<?, ?> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");
            json.append(toJson(entry.getValue()));
            first = false;
        }
        
        json.append("}");
        return json.toString();
    }
    
    private static String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}