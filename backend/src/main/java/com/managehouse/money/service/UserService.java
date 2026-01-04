package com.managehouse.money.service;

import com.managehouse.money.dto.LoginResponse;
import com.managehouse.money.entity.User;
import com.managehouse.money.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public Optional<LoginResponse> login(String email) {
        return userRepository.findByEmail(email)
                .map(user -> new LoginResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getName(),
                        user.getColor()
                ));
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}

