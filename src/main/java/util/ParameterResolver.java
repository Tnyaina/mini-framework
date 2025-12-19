package util;

import annotation.Param;
import annotation.PathVariable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

public class ParameterResolver {

    public static Object[] resolveParameters(Method method, HttpServletRequest request) {
        return resolveParameters(method, request, null);
    }

    public static Object[] resolveParameters(Method method, HttpServletRequest request,
            Map<String, String> pathVariables) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();

            // 1. Injection Map<String, Object> (inclut maintenant les fichiers)
            if (paramType == Map.class) {
                args[i] = injectMapParameters(param, request);
                continue;
            }

            // 2. Support UploadedFile unique
            if (paramType == UploadedFile.class) {
                String paramName = param.isAnnotationPresent(Param.class) 
                    ? param.getAnnotation(Param.class).value()
                    : param.getName();
                
                args[i] = extractUploadedFile(request, paramName);
                continue;
            }

            // 3. Support des tableaux
            if (paramType.isArray()) {
                Class<?> componentType = paramType.getComponentType();
                String paramName = param.isAnnotationPresent(Param.class) 
                    ? param.getAnnotation(Param.class).value()
                    : param.getName();
                
                // Tableaux UploadedFile[]
                if (componentType == UploadedFile.class) {
                    args[i] = extractUploadedFiles(request, paramName);
                    continue;
                }
                
                // Tableaux d'objets complexes (avec notation indexée)
                if (!isPrimitiveOrWrapper(componentType) && componentType != String.class) {
                    args[i] = bindObjectArray(componentType, paramName, request);
                } else {
                    // Tableaux de primitives/String
                    String[] values = request.getParameterValues(paramName);
                    if (values != null) {
                        Object array = Array.newInstance(componentType, values.length);
                        for (int j = 0; j < values.length; j++) {
                            Array.set(array, j, convertValue(values[j], componentType));
                        }
                        args[i] = array;
                    } else {
                        args[i] = Array.newInstance(componentType, 0);
                    }
                }
                continue;
            }

            // 4. Injection objet custom (binding)
            if (!isPrimitiveOrWrapper(paramType) && paramType != String.class) {
                args[i] = bindObject(paramType, request, "");
                continue;
            }

            // 5. Paramètres simples (@Param, @PathVariable, primitifs)
            String value = null;

            if (param.isAnnotationPresent(PathVariable.class)) {
                String varName = param.getAnnotation(PathVariable.class).value();
                if (pathVariables != null) {
                    value = pathVariables.get(varName);
                }
            } else {
                String paramName;
                if (param.isAnnotationPresent(Param.class)) {
                    paramName = param.getAnnotation(Param.class).value();
                } else {
                    paramName = param.getName();
                }
                value = request.getParameter(paramName);
            }

            args[i] = convertValue(value, paramType);
        }

        return args;
    }

    /**
     * Extraire un fichier uploadé unique
     */
    private static UploadedFile extractUploadedFile(HttpServletRequest request, String name) {
        try {
            Part part = request.getPart(name);
            if (part != null && part.getContentType() != null) {
                return new UploadedFile(
                    UploadedFile.extractFilename(part),
                    part.getContentType(),
                    part.getInputStream().readAllBytes(),
                    part
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Extraire plusieurs fichiers uploadés (même name ou différents)
     */
    private static UploadedFile[] extractUploadedFiles(HttpServletRequest request, String name) {
        try {
            Collection<Part> parts = request.getParts();
            List<UploadedFile> files = new ArrayList<>();
            
            for (Part part : parts) {
                if (part.getName().equals(name) && part.getContentType() != null) {
                    files.add(new UploadedFile(
                        UploadedFile.extractFilename(part),
                        part.getContentType(),
                        part.getInputStream().readAllBytes(),
                        part
                    ));
                }
            }
            
            return files.toArray(new UploadedFile[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new UploadedFile[0];
    }

    /**
     * Binder un tableau d'objets depuis notation indexée
     * Ex: emp[0].nom=John&emp[0].age=30&emp[1].nom=Jane&emp[1].age=25
     */
    private static Object bindObjectArray(Class<?> componentType, String paramName, HttpServletRequest request) {
        Pattern pattern = Pattern.compile("^" + Pattern.quote(paramName) + "\\[(\\d+)\\]\\.(.+)$");
        
        Map<Integer, Map<String, String>> groupedParams = new TreeMap<>();
        
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String currentParam = paramNames.nextElement();
            Matcher matcher = pattern.matcher(currentParam);
            
            if (matcher.matches()) {
                int index = Integer.parseInt(matcher.group(1));
                String propertyPath = matcher.group(2);
                String value = request.getParameter(currentParam);
                
                groupedParams.putIfAbsent(index, new HashMap<>());
                groupedParams.get(index).put(propertyPath, value);
            }
        }
        
        if (groupedParams.isEmpty()) {
            return Array.newInstance(componentType, 0);
        }
        
        int maxIndex = groupedParams.keySet().stream().max(Integer::compare).orElse(-1);
        Object array = Array.newInstance(componentType, maxIndex + 1);
        
        for (Map.Entry<Integer, Map<String, String>> entry : groupedParams.entrySet()) {
            int index = entry.getKey();
            Map<String, String> properties = entry.getValue();
            
            Object instance = bindObjectFromMap(componentType, properties);
            Array.set(array, index, instance);
        }
        
        return array;
    }

    /**
     * Créer un objet depuis une Map de propriétés
     * Supporte les propriétés imbriquées: "ecole.nom" -> obj.ecole.nom
     */
    private static Object bindObjectFromMap(Class<?> type, Map<String, String> properties) {
        try {
            Object instance = type.getDeclaredConstructor().newInstance();
            
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String propertyPath = entry.getKey();
                String value = entry.getValue();
                
                setNestedProperty(instance, propertyPath, value);
            }
            
            return instance;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Définir une propriété imbriquée
     * Ex: setNestedProperty(etudiant, "ecole.nom", "MIT")
     */
    private static void setNestedProperty(Object target, String propertyPath, String value) {
        try {
            String[] parts = propertyPath.split("\\.", 2);
            String fieldName = parts[0];
            
            Field field = findField(target.getClass(), fieldName);
            if (field == null) return;
            
            field.setAccessible(true);
            
            if (parts.length == 1) {
                field.set(target, convertValue(value, field.getType()));
            } else {
                Object nestedObject = field.get(target);
                if (nestedObject == null) {
                    nestedObject = field.getType().getDeclaredConstructor().newInstance();
                    field.set(target, nestedObject);
                }
                setNestedProperty(nestedObject, parts[1], value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Trouver un champ dans une classe (y compris héritées)
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return findField(clazz.getSuperclass(), fieldName);
            }
            return null;
        }
    }

    /**
     * Injecter tous les paramètres ET fichiers dans une Map<String, Object>
     */
    private static Map<String, Object> injectMapParameters(Parameter param, HttpServletRequest request) {
        Type genericType = param.getParameterizedType();
        if (genericType instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType pType = (java.lang.reflect.ParameterizedType) genericType;
            Type[] typeArgs = pType.getActualTypeArguments();

            if (typeArgs.length == 2 && typeArgs[0] != String.class) {
                return null;
            }
        }

        Map<String, Object> paramMap = new HashMap<>();
        
        // Paramètres normaux
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String[] values = request.getParameterValues(paramName);

            if (values.length == 1) {
                paramMap.put(paramName, values[0]);
            } else {
                paramMap.put(paramName, values);
            }
        }

        // Fichiers uploadés
        try {
            Collection<Part> parts = request.getParts();
            for (Part part : parts) {
                if (part.getContentType() != null) {
                    UploadedFile file = new UploadedFile(
                        UploadedFile.extractFilename(part),
                        part.getContentType(),
                        part.getInputStream().readAllBytes(),
                        part
                    );
                    
                    // Si déjà une liste pour ce nom, ajouter au tableau
                    Object existing = paramMap.get(part.getName());
                    if (existing instanceof UploadedFile[]) {
                        UploadedFile[] oldArray = (UploadedFile[]) existing;
                        UploadedFile[] newArray = new UploadedFile[oldArray.length + 1];
                        System.arraycopy(oldArray, 0, newArray, 0, oldArray.length);
                        newArray[oldArray.length] = file;
                        paramMap.put(part.getName(), newArray);
                    } else if (existing instanceof UploadedFile) {
                        paramMap.put(part.getName(), new UploadedFile[]{(UploadedFile) existing, file});
                    } else {
                        paramMap.put(part.getName(), file);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return paramMap;
    }

    /**
     * Binding automatique : paramètres HTTP → objet Java
     * Supporte les objets imbriqués via notation point (ex: ecole.nom)
     */
    private static Object bindObject(Class<?> type, HttpServletRequest request, String prefix) {
        try {
            Object instance = type.getDeclaredConstructor().newInstance();

            for (Field field : type.getDeclaredFields()) {
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                String fieldName = prefix.isEmpty() ? field.getName() : prefix + "." + field.getName();

                if (isPrimitiveOrWrapper(fieldType) || fieldType == String.class ||
                        fieldType.isEnum() || fieldType == LocalDate.class || fieldType == LocalDateTime.class) {
                    String value = request.getParameter(fieldName);
                    if (value != null && !value.isEmpty()) {
                        field.set(instance, convertValue(value, fieldType));
                    }
                } else {
                    Object nestedObject = bindObject(fieldType, request, fieldName);
                    if (nestedObject != null) {
                        field.set(instance, nestedObject);
                    }
                }
            }

            return instance;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Vérifier si un type est primitif ou wrapper
     */
    private static boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() ||
                type == Integer.class || type == Long.class ||
                type == Double.class || type == Float.class ||
                type == Boolean.class || type == Short.class ||
                type == Byte.class || type == Character.class;
    }

    /**
     * Convertir une valeur String vers le type cible
     */
    private static Object convertValue(String value, Class<?> targetType) {
        if (value == null || value.isEmpty()) {
            return getDefaultValue(targetType);
        }

        if (targetType == String.class) {
            return value;
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        } else if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(value);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (targetType == short.class || targetType == Short.class) {
            return Short.parseShort(value);
        } else if (targetType == byte.class || targetType == Byte.class) {
            return Byte.parseByte(value);
        } else if (targetType.isEnum()) {
            @SuppressWarnings("unchecked")
            Class<Enum> enumType = (Class<Enum>) targetType;
            return Enum.valueOf(enumType, value.toUpperCase());
        } else if (targetType == LocalDate.class) {
            return LocalDate.parse(value);
        } else if (targetType == LocalDateTime.class) {
            return LocalDateTime.parse(value);
        }

        return null;
    }

    private static Object getDefaultValue(Class<?> type) {
        if (type == int.class)
            return 0;
        if (type == long.class)
            return 0L;
        if (type == double.class)
            return 0.0;
        if (type == float.class)
            return 0f;
        if (type == boolean.class)
            return false;
        if (type == short.class)
            return (short) 0;
        if (type == byte.class)
            return (byte) 0;
        return null;
    }
}