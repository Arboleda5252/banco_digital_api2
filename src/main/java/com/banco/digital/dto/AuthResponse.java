package com.banco.digital.dto;

import com.banco.digital.models.User;
import lombok.AllArgsConstructor;
import lombok.Data;

import lombok.Builder;

@Data
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private User user;
    private boolean mfaRequired;
    private String message;

    public AuthResponse(String token, User user) {
        this.token = token;
        this.user = user;
        this.mfaRequired = false;
    }

    public AuthResponse(String token, User user, boolean mfaRequired) {
        this.token = token;
        this.user = user;
        this.mfaRequired = mfaRequired;
    }
}
