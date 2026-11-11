package ch.zli.m223.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginDto {
    @NotBlank 
    public String email;

    @NotBlank 
    public String password;
}
