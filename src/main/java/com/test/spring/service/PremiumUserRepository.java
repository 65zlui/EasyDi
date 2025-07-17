package com.test.spring.service;

import com.test.spring.di.annotation.Component;

import java.util.List;

/**
 * A specialized implementation of UserRepository for premium users.
 */
@Component(name = "premiumRepo")
public class PremiumUserRepository extends UserRepository {
    
    @Override
    public List<String> getUsers() {
        return List.of("PremiumUser1", "PremiumUser2");
    }
}
