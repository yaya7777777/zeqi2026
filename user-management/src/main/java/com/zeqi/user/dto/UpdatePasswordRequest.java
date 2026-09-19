package com.zeqi.user.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdatePasswordRequest {
    @NotBlank
    private String password;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
