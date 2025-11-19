package util;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ParameterResolver {

    public static Object[] resolveParameters(Method method, HttpServletRequest request) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            String paramName = param.getName(); // doit correspondre au name dans le formulaire
            Class<?> paramType = param.getType();

            String value = request.getParameter(paramName); // lecture du paramètre
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

        return null; // type non supporté
    }

    private static Object getDefaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0f;
        if (type == boolean.class) return false;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        return null; // String, objets, enums → null par défaut
    }
}
