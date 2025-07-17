package com.test.spring;

import com.test.spring.di.core.EasyDiContainer;
import com.test.spring.di.core.Module;
import com.test.spring.service.NotificationService;
import com.test.spring.service.PremiumUserRepository;
import com.test.spring.service.UserRepository;
import com.test.spring.service.UserService;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting EasyDi demonstration...");
        
        // 方法1: 使用组件扫描自动发现和注册所有带有 @Component 注解的类
        // 这种方式更接近 Spring 和 Koin 等框架的工作方式
        EasyDiContainer container = EasyDiContainer.startKoinWithScan(
            new String[]{"com.test.spring.service"}, // 扫描这个包下的所有类
            new Module() // 可以添加额外的模块，但这里我们不需要
        );
        
        // 获取服务并使用它们
        UserService userService = container.get(UserService.class);
        List<String> users = userService.getAllUsers();
        System.out.println("Users from UserService: " + users);
        
        NotificationService notificationService = container.get(NotificationService.class);
        notificationService.sendNotification("Alice", "Your account has been created!");
        
        // 方法2: 直接创建实例并注入依赖
        System.out.println("\nDemonstrating direct instance creation...");
        UserService anotherService = container.createInstance(UserService.class);
        System.out.println("Users from directly created service: " + anotherService.getAllUsers());
        
        // 方法3: 使用命名的依赖
        System.out.println("\nDemonstrating named dependencies...");
        Module namedModule = new Module()
            .singleton("premiumRepo", UserRepository.class, PremiumUserRepository::new);
        
        container.loadModules(namedModule);
        UserRepository premiumRepo = container.get("premiumRepo", UserRepository.class);
        System.out.println("Premium users: " + premiumRepo.getUsers());
        
        // 方法4: 使用工厂方法（每次获取新实例）
        System.out.println("\nDemonstrating factory beans...");
        Module factoryModule = new Module()
            .factory("counter", Counter.class, () -> new Counter());
        
        container.loadModules(factoryModule);
        Counter counter1 = container.get("counter", Counter.class);
        counter1.increment();
        counter1.increment();
        System.out.println("Counter1 value: " + counter1.getValue()); // 应该是 2
        
        Counter counter2 = container.get("counter", Counter.class);
        System.out.println("Counter2 value: " + counter2.getValue()); // 应该是 0，因为是新实例
        
        System.out.println("\nEasyDi demonstration completed.");
    }
    
    // 简单的计数器类，用于演示工厂方法
    static class Counter {
        private int value = 0;
        
        public void increment() {
            value++;
        }
        
        public int getValue() {
            return value;
        }
    }
}