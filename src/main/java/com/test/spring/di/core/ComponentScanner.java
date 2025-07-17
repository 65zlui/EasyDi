package com.test.spring.di.core;

import com.test.spring.di.annotation.Component;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Scanner for finding classes annotated with @Component.
 * Uses caching to improve performance.
 */
public class ComponentScanner {
    // Cache of component classes by package
    private static final Map<String, Set<Class<?>>> componentCache = new ConcurrentHashMap<>();
    
    /**
     * Scan a package for classes annotated with @Component.
     * Results are cached for improved performance.
     *
     * @param packageName The package to scan
     * @return Set of component classes
     */
    public static Set<Class<?>> scanPackage(String packageName) {
        return componentCache.computeIfAbsent(packageName, ComponentScanner::findComponents);
    }
    
    /**
     * Find all classes annotated with @Component in a package.
     *
     * @param packageName The package to scan
     * @return Set of component classes
     */
    private static Set<Class<?>> findComponents(String packageName) {
        Set<Class<?>> components = new HashSet<>();
        String path = packageName.replace('.', '/');
        
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(path);
            
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                
                if (resource.getProtocol().equals("file")) {
                    findComponentsInDirectory(new File(resource.getFile()), packageName, components);
                } else if (resource.getProtocol().equals("jar")) {
                    findComponentsInJar(resource, path, packageName, components);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to scan package: " + packageName, e);
        }
        
        return components;
    }
    
    private static void findComponentsInDirectory(File directory, String packageName, Set<Class<?>> components) 
            throws ClassNotFoundException {
        File[] files = directory.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            String fileName = file.getName();
            
            if (file.isDirectory()) {
                findComponentsInDirectory(file, packageName + "." + fileName, components);
            } else if (fileName.endsWith(".class")) {
                String className = packageName + "." + fileName.substring(0, fileName.length() - 6);
                Class<?> clazz = Class.forName(className);
                
                if (clazz.isAnnotationPresent(Component.class)) {
                    components.add(clazz);
                }
            }
        }
    }
    
    private static void findComponentsInJar(URL resource, String path, String packageName, Set<Class<?>> components) 
            throws IOException, ClassNotFoundException {
        String jarPath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
        
        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jar.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                if (entryName.startsWith(path) && entryName.endsWith(".class")) {
                    String className = entryName.substring(0, entryName.length() - 6).replace('/', '.');
                    Class<?> clazz = Class.forName(className);
                    
                    if (clazz.isAnnotationPresent(Component.class)) {
                        components.add(clazz);
                    }
                }
            }
        }
    }
    
    /**
     * Clear the component cache.
     * This can be useful when testing or when classes might be reloaded.
     */
    public static void clearCache() {
        componentCache.clear();
    }
}
