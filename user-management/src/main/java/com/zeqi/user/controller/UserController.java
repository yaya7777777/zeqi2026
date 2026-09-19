package com.zeqi.user.controller;

import com.zeqi.user.dto.MessageResponse;
import com.zeqi.user.dto.UpdatePasswordRequest;
import com.zeqi.user.dto.UpdateUsernameRequest;
import com.zeqi.user.dto.UserResponse;
import com.zeqi.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    private Long id(HttpServletRequest r) {
        return (Long) r.getAttribute("userId");
    }

    @GetMapping("/me")
    public UserResponse me(HttpServletRequest r) {
        return service.currentUser(id(r));
    }

    @GetMapping
    public List<UserResponse> list(HttpServletRequest r) {
        return service.listUsers(id(r));
    }

    @PatchMapping("/me/username")
    public UserResponse username(HttpServletRequest r,
                                  @Valid @RequestBody UpdateUsernameRequest b) {
        return service.updateUsername(id(r), b);
    }

    @PatchMapping("/me/password")
    public MessageResponse password(HttpServletRequest r,
                                     @Valid @RequestBody UpdatePasswordRequest b) {
        return service.updatePassword(id(r), b);
    }
}
