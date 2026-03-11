package com.snippet.service;

import com.snippet.entity.User;
import com.snippet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    @Transactional
    public User create(String email, String password, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(name)
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public User update(Long id, String email, String password, String name) {
        User user = findById(id);
        String encodedPassword = password != null ? passwordEncoder.encode(password) : null;
        user.update(email, encodedPassword, name);
        return user;
    }

    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
