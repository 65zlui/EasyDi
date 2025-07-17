package com.test.spring.di.core;

import com.test.spring.di.annotation.Component;
import com.test.spring.di.annotation.Inject;
import com.test.spring.di.exception.BeanNotFoundException;
import com.test.spring.di.exception.CircularDependencyException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The main container for the EasyDi framework.
 * Similar to Koin's KoinApplication.
 */
public class EasyDiContainer {
    // Using ConcurrentHashMap for better thread safety and performance
    private final Map<String, BeanDefinition<?>> definitionsByName = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<BeanDefinition<?>>> definitionsByType = new ConcurrentHashMap<>();
    private final Set<Object> beingInjected = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // Cache for faster type lookups
    private final Map<Class<?>, Class<?>> assignableTypeCache = new ConcurrentHashMap<>();

    /**
     * Create a new EasyDi container with the given modules.
     * @param modules The modules to load.
     * @return A new EasyDi container.
     */
    public static EasyDiContainer startKoin(Module... modules) {
        EasyDiContainer container = new EasyDiContainer();
        container.loadModules(modules);
        return container;
    }
    
    /**
     * Create a new EasyDi container with the given modules and scan the specified packages for components.
     * @param packagesToScan The packages to scan for components
     * @param modules The modules to load
     * @return A new EasyDi container
     */
    public static EasyDiContainer startKoinWithScan(String[] packagesToScan, Module... modules) {
        EasyDiContainer container = new EasyDiContainer();
        container.loadModules(modules);
        container.scanPackages(packagesToScan);
        return container;
    }

    /**
     * Load modules into this container.
     * @param modules The modules to load.
     */
    public void loadModules(Module... modules) {
        for (Module module : modules) {
            for (BeanDefinition<?> definition : module.getDefinitions()) {
                registerDefinition(definition);
            }
        }
    }
    
    /**
     * Scan packages for classes annotated with @Component and register them as beans.
     * @param packageNames The packages to scan
     */
    public void scanPackages(String... packageNames) {
        for (String packageName : packageNames) {
            Set<Class<?>> componentClasses = ComponentScanner.scanPackage(packageName);
            
            for (Class<?> componentClass : componentClasses) {
                Component annotation = componentClass.getAnnotation(Component.class);
                String name = annotation.name();
                
                if (name.isEmpty()) {
                    name = componentClass.getSimpleName();
                    // Make first letter lowercase for conventional bean naming
                    name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                }
                
                // Create a type-safe factory for the component
                // We need to ensure the Supplier<T> and Class<T> have the same type parameter
                @SuppressWarnings("unchecked")
                Class<Object> erasedClass = (Class<Object>) componentClass;
                
                // Create a supplier that returns objects of the component's type
                Supplier<Object> factory = () -> {
                    try {
                        return createInstance(erasedClass);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create instance of " + componentClass.getName(), e);
                    }
                };
                
                // Create the bean definition with proper generic typing
                @SuppressWarnings("unchecked")
                BeanDefinition<Object> definition = new BeanDefinition<>(name, erasedClass, factory, true);
                registerDefinition(definition);
            }
        }
    }

    private void registerDefinition(BeanDefinition<?> definition) {
        String name = definition.getName();
        Class<?> type = definition.getType();

        // Register by name if provided
        if (name != null && !name.isEmpty()) {
            definitionsByName.put(name, definition);
        }

        // Register by type using thread-safe collection
        definitionsByType.computeIfAbsent(type, k -> 
            Collections.synchronizedList(new ArrayList<>())).add(definition);
        
        // Also register for all interfaces and superclasses
        registerForSuperTypes(type, definition);
    }
    
    /**
     * Register a bean definition for all interfaces and superclasses of the given type.
     * This allows getting beans by their interface types.
     * 
     * @param type The bean type
     * @param definition The bean definition
     */
    private void registerForSuperTypes(Class<?> type, BeanDefinition<?> definition) {
        // Register for interfaces
        for (Class<?> iface : type.getInterfaces()) {
            definitionsByType.computeIfAbsent(iface, k -> 
                Collections.synchronizedList(new ArrayList<>())).add(definition);
        }
        
        // Register for superclass if exists
        Class<?> superclass = type.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            definitionsByType.computeIfAbsent(superclass, k -> 
                Collections.synchronizedList(new ArrayList<>())).add(definition);
            // Recursively register for superclass's supertypes
            registerForSuperTypes(superclass, definition);
        }
    }

    /**
     * Get a bean by type.
     * @param type The type of the bean.
     * @param <T> The type of the bean.
     * @return The bean.
     * @throws BeanNotFoundException If no bean of the given type is found.
     */
    public <T> T get(Class<T> type) {
        // Fast path: direct lookup by exact type
        List<BeanDefinition<?>> definitions = definitionsByType.get(type);
        
        if (definitions == null || definitions.isEmpty()) {
            // Slower path: look for assignable types
            for (Map.Entry<Class<?>, List<BeanDefinition<?>>> entry : definitionsByType.entrySet()) {
                Class<?> definedType = entry.getKey();
                
                // Check assignability with caching
                if (isAssignableFrom(type, definedType)) {
                    definitions = entry.getValue();
                    break;
                }
            }
            
            if (definitions == null || definitions.isEmpty()) {
                throw new BeanNotFoundException("No bean found for type: " + type.getName());
            }
        }
        
        @SuppressWarnings("unchecked")
        BeanDefinition<T> definition = (BeanDefinition<T>) definitions.get(0);
        return definition.getInstance(this);
    }
    
    /**
     * Check if one type is assignable from another, with caching for performance.
     * 
     * @param target The target type
     * @param source The source type
     * @return true if target is assignable from source
     */
    private boolean isAssignableFrom(Class<?> target, Class<?> source) {
        String cacheKey = target.getName() + "-" + source.getName();
        return assignableTypeCache.computeIfAbsent(source, k -> {
            return target.isAssignableFrom(source) ? target : null;
        }) != null;
    }

    /**
     * Get a bean by name and type.
     * @param name The name of the bean.
     * @param type The type of the bean.
     * @param <T> The type of the bean.
     * @return The bean.
     * @throws BeanNotFoundException If no bean of the given name and type is found.
     */
    public <T> T get(String name, Class<T> type) {
        BeanDefinition<?> definition = definitionsByName.get(name);
        if (definition == null) {
            throw new BeanNotFoundException("No bean found for name: " + name);
        }
        
        // Check type compatibility with caching
        if (!isAssignableFrom(type, definition.getType())) {
            throw new BeanNotFoundException("Bean found for name: " + name + 
                    " but it is not compatible with required type: " + type.getName());
        }
        
        @SuppressWarnings("unchecked")
        BeanDefinition<T> typedDefinition = (BeanDefinition<T>) definition;
        return typedDefinition.getInstance(this);
    }

    /**
     * Inject dependencies into an object.
     * @param instance The object to inject dependencies into.
     */
    public void injectDependencies(Object instance) {
        if (instance == null) {
            return;
        }

        // Check for circular dependencies
        if (!beingInjected.add(instance)) {
            throw new CircularDependencyException("Circular dependency detected for: " + instance.getClass().getName());
        }

        try {
            // Get cached injectable fields and inject dependencies
            List<Field> injectableFields = ReflectionCache.getInjectableFields(instance.getClass());
            for (Field field : injectableFields) {
                Object dependency = get(field.getType());
                field.set(instance, dependency);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to inject dependencies", e);
        } finally {
            beingInjected.remove(instance);
        }
    }

    /**
     * Create an instance of a class and inject its dependencies.
     * @param clazz The class to instantiate.
     * @param <T> The type of the class.
     * @return The instantiated object with dependencies injected.
     */
    public <T> T createInstance(Class<T> clazz) {
        try {
            // Try to find a constructor annotated with @Inject from cache
            Constructor<T> injectableConstructor = ReflectionCache.getInjectableConstructor(clazz);
            if (injectableConstructor != null) {
                Class<?>[] paramTypes = injectableConstructor.getParameterTypes();
                Object[] params = new Object[paramTypes.length];
                
                for (int i = 0; i < paramTypes.length; i++) {
                    params[i] = get(paramTypes[i]);
                }
                
                T instance = injectableConstructor.newInstance(params);
                injectDependencies(instance);
                return instance;
            }
            
            // If no @Inject constructor, use the default constructor from cache
            Constructor<T> defaultConstructor = ReflectionCache.getDefaultConstructor(clazz);
            if (defaultConstructor == null) {
                throw new NoSuchMethodException("No default constructor found for " + clazz.getName());
            }
            
            T instance = defaultConstructor.newInstance();
            injectDependencies(instance);
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
        }
    }
    
    /**
     * Clear all caches to free memory or prepare for class reloading.
     * This should be called when the container is no longer needed or when
     * the application is being redeployed.
     */
    public void clearCaches() {
        ReflectionCache.clearCaches();
        MethodCache.clearCache();
        ComponentScanner.clearCache();
        assignableTypeCache.clear();
    }
}
