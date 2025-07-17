package com.test.spring.di.core;

import java.util.function.Supplier;

/**
 * Defines a bean that can be injected by the EasyDi framework.
 * @param <T> The type of the bean.
 */
public class BeanDefinition<T> {
    private final String name;
    private final Class<T> type;
    private final Supplier<T> factory;
    private final boolean singleton;
    // volatile ensures visibility across threads
    private volatile T instance;

    public BeanDefinition(String name, Class<T> type, Supplier<T> factory, boolean singleton) {
        this.name = name;
        this.type = type;
        this.factory = factory;
        this.singleton = singleton;
    }

    public String getName() {
        return name;
    }

    public Class<T> getType() {
        return type;
    }

    /**
     * Get an instance of this bean. For singletons, the same instance is returned each time.
     * For non-singletons, a new instance is created each time.
     * Uses double-checked locking for thread safety and performance.
     * 
     * @param container The container to use for injecting dependencies.
     * @return An instance of the bean.
     */
    public T getInstance(EasyDiContainer container) {
        if (singleton) {
            // Double-checked locking pattern for better performance
            if (instance == null) {
                synchronized (this) {
                    if (instance == null) {
                        T newInstance = factory.get();
                        container.injectDependencies(newInstance);
                        instance = newInstance; // Volatile write happens only once
                    }
                }
            }
            return instance;
        } else {
            // For non-singletons, no synchronization needed
            T newInstance = factory.get();
            container.injectDependencies(newInstance);
            return newInstance;
        }
    }
}
