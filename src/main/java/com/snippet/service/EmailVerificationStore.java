package com.snippet.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EmailVerificationStore {

    private record CodeEntry(String code, LocalDateTime expiresAt) {}

    private final ConcurrentHashMap<String, CodeEntry> store = new ConcurrentHashMap<>();

    public void save(String email, String code, int expiryMinutes) {
        store.put(email.toLowerCase(), new CodeEntry(code, LocalDateTime.now().plusMinutes(expiryMinutes)));
    }

    public boolean verify(String email, String code) {
        CodeEntry entry = store.get(email.toLowerCase());
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.expiresAt())) {
            store.remove(email.toLowerCase());
            return false;
        }
        return entry.code().equals(code);
    }

    public void remove(String email) {
        store.remove(email.toLowerCase());
    }
}
