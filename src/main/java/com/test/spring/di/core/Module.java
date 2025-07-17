package com.test.spring.di.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A module for defining beans in the EasyDi framework.
 * Similar to Koin modules.
 */
public class Module {
    private final List<BeanDefinition<?>> definitions = new ArrayList<>();

    /**
     * Define a singleton bean.
     * @param type The class of the bean.
     * @param factory A supplier function that creates the bean.
     * @param <T> The type of the bean.
     * @return This module for chaining.
     */
    public <T> Module singleton(Class<T> type, Supplier<T> factory) {
        return singleton("", type, factory);
    }

    /**
     * Define a singleton bean with a specific name.
     * @param name The name of the bean.
     * @param type The class of the bean.
     * @param factory A supplier function that creates the bean.
     * @param <T> The type of the bean.
     * @return This module for chaining.
     */
    public <T> Module singleton(String name, Class<T> type, Supplier<T> factory) {
        definitions.add(new BeanDefinition<>(name, type, factory, true));
        return this;
    }

    /**
     * Define a factory bean (non-singleton).
     * @param type The class of the bean.
     * @param factory A supplier function that creates the bean.
     * @param <T> The type of the bean.
     * @return This module for chaining.
     */
    public <T> Module factory(Class<T> type, Supplier<T> factory) {
        return factory("", type, factory);
    }

    /**
     * Define a factory bean (non-singleton) with a specific name.
     * @param name The name of the bean.
     * @param type The class of the bean.
     * @param factory A supplier function that creates the bean.
     * @param <T> The type of the bean.
     * @return This module for chaining.
     */
    public <T> Module factory(String name, Class<T> type, Supplier<T> factory) {
        definitions.add(new BeanDefinition<>(name, type, factory, false));
        return this;
    }

    List<BeanDefinition<?>> getDefinitions() {
        return definitions;
    }
}
