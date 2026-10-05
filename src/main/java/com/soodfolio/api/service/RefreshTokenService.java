package com.soodfolio.api.service;

import com.soodfolio.api.entity.RefreshToken;
import com.soodfolio.api.entity.User;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user);
    RefreshToken verifyExpiration(String tokenString);
    void revokeToken(String tokenString);
}
