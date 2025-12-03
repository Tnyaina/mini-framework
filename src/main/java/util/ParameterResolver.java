package util;

import annotation.Param;
import annotation.PathVariable;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

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
            
            // Injecter Map<String, Object> avec tous les paramètres de la requête
            if (paramType == Map.class) {
                // Vérifier les types génériques
                Type genericType = param.getParameterizedType();
                if (genericType instanceof java.lang.reflect.ParameterizedType) {
                    java.lang.reflect.ParameterizedType pType = (java.lang.reflect.ParameterizedType) genericType;
                    Type[] typeArgs = pType.getActualTypeArguments();
                    
                    // Accepter seulement Map<String, Object> ou Map<String, ?>
                    if (typeArgs.length == 2 && typeArgs[0] != String.class) {
                        args[i] = null;
                        continue;
                    }
                }
                
                Map<String, Object> paramMap = new HashMap<>();
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
                
                args[i] = paramMap;
                continue;
            }
            
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
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0f;
        if (type == boolean.class) return false;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        return null;
    }
}