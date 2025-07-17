package com.test.spring.service;

import com.test.spring.di.annotation.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Example repository class to demonstrate dependency injection.
 */
@Component
public class UserRepository {
    
    public List<String> getUsers() {
        // In a real application, this would fetch from a database
        return Arrays.asList("User1", "User2", "User3");
    }
}
