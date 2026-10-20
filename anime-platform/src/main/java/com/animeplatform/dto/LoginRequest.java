package com.animeplatform.dto;

import jakarta.validation.constraints.NotBlank;

/** Pedido de login. */
public class LoginRequest {

    @NotBlank(message = "O username é obrigatório")
    private String username;

    @NotBlank(message = "A password é obrigatória")
    private String password;

    public LoginRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
