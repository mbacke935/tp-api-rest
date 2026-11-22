package sn.ucak.dar.tpapirest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sn.ucak.dar.tpapirest.entities.Role;
import sn.ucak.dar.tpapirest.entities.User;

import java.util.UUID;

public class UserResponse {

    @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private final UUID id;

    @Schema(example = "Fatou Diop")
    private final String name;

    @Schema(example = "fatou.diop@example.com")
    private final String email;

    @Schema(example = "USER")
    private final Role role;

    public UserResponse(UUID id, String name, String email, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public static UserResponse fromEntity(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }
}
