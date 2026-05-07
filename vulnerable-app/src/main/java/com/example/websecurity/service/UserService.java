package com.example.websecurity.service;

import com.example.websecurity.exception.WebSecMissingDataException;
import com.example.websecurity.persistence.User;
import com.example.websecurity.persistence.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static lombok.AccessLevel.PACKAGE;

@Service
@AllArgsConstructor(access = PACKAGE)
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    public User getUserByEmail(String email) {
        log.info("User Service: Getting user from database by email: {}", email);
        User user = userRepository.findByEmail(email).orElseThrow(() -> new WebSecMissingDataException("User with email " + email + " not found"));
        log.info("User Service: Found user with id: {} and email: {}", user.getId(), user.getEmail());
        return user;
    }
}
