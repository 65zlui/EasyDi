package com.test.spring.di.core;

import com.test.spring.di.annotation.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Cache for reflection metadata to improve performance.
 */
public class ReflectionCache {
    // Cache for injectable fields by class
    private static final Map<Class<?>, List<Field>> injectableFieldsCache = new ConcurrentHashMap<>();
    
    // Cache for injectable constructors by class
    private static final Map<Class<?>, Constructor<?>> injectableConstructorCache = new ConcurrentHashMap<>();
    
    // Cache for default constructors by class
    private static final Map<Class<?>, Constructor<?>> defaultConstructorCache = new ConcurrentHashMap<>();

    /**
     * Get all injectable fields for a class, including fields from superclasses.
     * Results are cached for performance.
     *
     * @param clazz The class to get injectable fields for
     * @return List of injectable fields
     */
    public static List<Field> getInjectableFields(Class<?> clazz) {
        return injectableFieldsCache.computeIfAbsent(clazz, ReflectionCache::findInjectableFields);
    }

    /**
     * Find all injectable fields for a class, including fields from superclasses.
     *
     * @param clazz The class to find injectable fields for
     * @return List of injectable fields
     */
    private static List<Field> findInjectableFields(Class<?> clazz) {
        List<Field> injectableFields = new ArrayList<>();
        Class<?> currentClass = clazz;
        
        while (currentClass != null) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class)) {
                    field.setAccessible(true);
                    injectableFields.add(field);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        
        return injectableFields;
    }

    /**
     * Get an injectable constructor for a class.
     * Results are cached for performance.
     *
     * @param clazz The class to get an injectable constructor for
     * @return The injectable constructor, or null if none is found
     */
    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> getInjectableConstructor(Class<T> clazz) {
        return (Constructor<T>) injectableConstructorCache.computeIfAbsent(clazz, ReflectionCache::findInjectableConstructor);
    }

    /**
     * Find an injectable constructor for a class.
     *
     * @param clazz The class to find an injectable constructor for
     * @return The injectable constructor, or null if none is found
     */
    private static Constructor<?> findInjectableConstructor(Class<?> clazz) {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.isAnnotationPresent(Inject.class)) {
                constructor.setAccessible(true);
                return constructor;
            }
        }
        return null;
    }

    /**
     * Get the default constructor for a class.
     * Results are cached for performance.
     *
     * @param clazz The class to get the default constructor for
     * @return The default constructor
     * @throws NoSuchMethodException if no default constructor is found
     */
    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> getDefaultConstructor(Class<T> clazz) throws NoSuchMethodException {
        return (Constructor<T>) defaultConstructorCache.computeIfAbsent(clazz, c -> {
            try {
                Constructor<?> constructor = c.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor;
            } catch (NoSuchMethodException e) {
                return null;
            }
        });
    }

    /**
     * Clear all caches.
     * This can be useful when testing or when classes might be reloaded.
     */
    public static void clearCaches() {
        injectableFieldsCache.clear();
        injectableConstructorCache.clear();
        defaultConstructorCache.clear();
    }
}
