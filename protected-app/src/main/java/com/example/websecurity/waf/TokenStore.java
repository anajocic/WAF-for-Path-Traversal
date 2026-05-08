package com.example.websecurity.waf;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {
    private final Map<String, String> store = new ConcurrentHashMap<>();

    public void save(String username, String token) {
        store.put(username, token);
    }

    public String get(String username) {
        return store.get(username);
    }

    // TODO add logout function that frontend calls and use this
    public void remove(String username) {
        store.remove(username);
    }

    public boolean matches(String username, String token) {
        return token.equals(store.get(username));
    }
}