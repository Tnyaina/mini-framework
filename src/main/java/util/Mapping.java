package util;

import java.lang.reflect.Method;

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
}