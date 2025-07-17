package com.test.spring.service;

import com.test.spring.di.annotation.Component;
import com.test.spring.di.annotation.Inject;

/**
 * Example service class that demonstrates constructor injection.
 */
@Component
public class NotificationService {
    
    private final UserService userService;
    
    @Inject
    public NotificationService(UserService userService) {
        this.userService = userService;
    }
    
    public void sendNotification(String userName, String message) {
        String greeting = userService.getGreeting(userName);
        System.out.println(greeting + " " + message);
    }
}
