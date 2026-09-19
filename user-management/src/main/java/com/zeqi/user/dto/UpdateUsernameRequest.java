package com.zeqi.user.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateUsernameRequest {
    @NotBlank
    private String username;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
