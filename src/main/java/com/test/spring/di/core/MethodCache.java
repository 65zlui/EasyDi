package com.test.spring.di.core;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for method reflection to improve performance.
 */
public class MethodCache {
    // Cache structure: Class -> Method Name -> Parameter Types -> Method
    private static final Map<Class<?>, Map<String, Map<String, Method>>> methodCache = new ConcurrentHashMap<>();
    
    /**
     * Get a method from the cache or find it using reflection.
     * 
     * @param clazz The class containing the method
     * @param methodName The name of the method
     * @param parameterTypes The parameter types of the method
     * @return The method
     * @throws NoSuchMethodException if the method is not found
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) 
            throws NoSuchMethodException {
        // Generate a key for the parameter types
        String paramKey = generateParameterKey(parameterTypes);
        
        // Get or create the method map for this class
        Map<String, Map<String, Method>> methodMap = methodCache.computeIfAbsent(
                clazz, k -> new ConcurrentHashMap<>());
        
        // Get or create the parameter map for this method name
        Map<String, Method> paramMap = methodMap.computeIfAbsent(
                methodName, k -> new ConcurrentHashMap<>());
        
        // Get the method from the cache or find it using reflection
        return paramMap.computeIfAbsent(paramKey, k -> {
            try {
                Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                // Use a placeholder to indicate that the method doesn't exist
                return null;
            }
        });
    }
    
    /**
     * Generate a key for the parameter types.
     * 
     * @param parameterTypes The parameter types
     * @return A string key
     */
    private static String generateParameterKey(Class<?>... parameterTypes) {
        if (parameterTypes.length == 0) {
            return "()";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(parameterTypes[i].getName());
        }
        
        sb.append(')');
        return sb.toString();
    }
    
    /**
     * Invoke a method on an object with the given parameters.
     * Uses the method cache for improved performance.
     * 
     * @param obj The object to invoke the method on
     * @param methodName The name of the method
     * @param args The arguments to pass to the method
     * @return The result of the method invocation
     * @throws Exception if the method invocation fails
     */
    public static Object invokeMethod(Object obj, String methodName, Object... args) throws Exception {
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            paramTypes[i] = args[i].getClass();
        }
        
        Method method = getMethod(obj.getClass(), methodName, paramTypes);
        if (method == null) {
            throw new NoSuchMethodException("Method not found: " + methodName);
        }
        
        return method.invoke(obj, args);
    }
    
    /**
     * Clear the method cache.
     * This can be useful when testing or when classes might be reloaded.
     */
    public static void clearCache() {
        methodCache.clear();
    }
}
