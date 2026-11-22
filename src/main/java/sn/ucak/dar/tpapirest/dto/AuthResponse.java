package sn.ucak.dar.tpapirest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class AuthResponse {

    @Schema(example = "eyJhbGciOiJIUzI1NiJ9...")
    private final String accessToken;

    @Schema(example = "Bearer")
    private final String tokenType;

    @Schema(example = "900")
    private final long expiresIn;

    public AuthResponse(String accessToken, long expiresIn) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }
}
