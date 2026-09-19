package com.zeqi.user.service;

import com.zeqi.user.dto.LoginRequest;
import com.zeqi.user.dto.LoginResponse;
import com.zeqi.user.dto.MessageResponse;
import com.zeqi.user.dto.RegisterRequest;
import com.zeqi.user.dto.UpdatePasswordRequest;
import com.zeqi.user.dto.UpdateUsernameRequest;
import com.zeqi.user.dto.UserResponse;
import com.zeqi.user.entity.User;
import com.zeqi.user.exception.ApiException;
import com.zeqi.user.repository.UserRepository;
import com.zeqi.user.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository repo;
    private final JwtService jwt;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserService(UserRepository repo, JwtService jwt) {
        this.repo = repo;
        this.jwt = jwt;
    }

    @Transactional
    public UserResponse register(RegisterRequest r) {
        if (repo.existsByUsername(r.getUsername())) {
            throw new ApiException(HttpStatus.CONFLICT, "username already exists");
        }
        User u = repo.save(new User(r.getUsername(), encoder.encode(r.getPassword())));
        log.info("register success: username={}", u.getUsername());
        return UserResponse.from(u);
    }

    public LoginResponse login(LoginRequest r) {
        User u = repo.findByUsername(r.getUsername()).orElse(null);
        if (u == null || !encoder.matches(r.getPassword(), u.getPassword())) {
            log.warn("login failed: username={}", r.getUsername());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid username or password");
        }
        log.info("login success: userId={}", u.getId());
        return new LoginResponse(jwt.generate(u.getId(), u.getUsername()));
    }

    public UserResponse currentUser(Long id) {
        return UserResponse.from(require(id));
    }

    public List<UserResponse> listUsers(Long id) {
        require(id);
        return repo.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse updateUsername(Long id, UpdateUsernameRequest r) {
        User u = require(id);
        if (repo.existsByUsername(r.getUsername()) && !u.getUsername().equals(r.getUsername())) {
            throw new ApiException(HttpStatus.CONFLICT, "username already exists");
        }
        u.setUsername(r.getUsername());
        u = repo.save(u);
        log.info("update username success: userId={}", id);
        return UserResponse.from(u);
    }

    @Transactional
    public MessageResponse updatePassword(Long id, UpdatePasswordRequest r) {
        User u = require(id);
        u.setPassword(encoder.encode(r.getPassword()));
        repo.save(u);
        log.info("update password success: userId={}", id);
        return new MessageResponse("password updated successfully");
    }

    public MessageResponse logout(Long id) {
        require(id);
        log.info("logout success: userId={}", id);
        return new MessageResponse("logout successfully; discard the token on the client");
    }

    private User require(Long id) {
        if (id == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        return repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user not found"));
    }
}
