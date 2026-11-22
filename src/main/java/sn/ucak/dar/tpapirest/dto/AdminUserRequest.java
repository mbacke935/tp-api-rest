package sn.ucak.dar.tpapirest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class AdminUserRequest {

    @Schema(example = "Moussa Ndiaye")
    private String name;

    @Schema(example = "moussa.ndiaye@example.com")
    private String email;

    @Schema(example = "SecretPass123!")
    private String password;

    @Schema(example = "USER", allowableValues = {"USER", "ADMIN"})
    private String role;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
