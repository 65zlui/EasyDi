package com.test.spring.service;

import com.test.spring.di.annotation.Component;
import com.test.spring.di.annotation.Inject;

import java.util.List;

/**
 * Example service class that depends on UserRepository.
 */
@Component
public class UserService {
    
    @Inject
    private UserRepository userRepository;
    
    public List<String> getAllUsers() {
        return userRepository.getUsers();
    }
    
    public String getGreeting(String userName) {
        return "Hello, " + userName + "!";
    }
}
