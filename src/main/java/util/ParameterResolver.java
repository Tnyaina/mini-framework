package util;

import annotation.Param;
import annotation.PathVariable;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
            String value = null;
            
            // Vérifier si c'est une variable de chemin (@PathVariable)
            if (param.isAnnotationPresent(PathVariable.class)) {
                String varName = param.getAnnotation(PathVariable.class).value();
                if (pathVariables != null) {
                    value = pathVariables.get(varName);
                }
            }
            // Sinon, vérifier si c'est un paramètre de requête (@Param ou nom par défaut)
            else {
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