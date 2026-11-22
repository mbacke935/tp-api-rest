package sn.ucak.dar.tpapirest.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import sn.ucak.dar.tpapirest.exceptions.BadRequestException;

import java.util.Map;

public class RegisterRequest {

    @Schema(example = "Fatou Diop", description = "Nom complet (2 à 100 caractères après trim)")
    private String name;

    @Schema(example = "fatou.diop@example.com", description = "Adresse email unique")
    private String email;

    @Schema(example = "MotDePasse123!", description = "Mot de passe (8 à 72 caractères)")
    private String password;

    @JsonAnySetter
    public void handleUnknownProperty(String key, Object value) {
        if ("role".equalsIgnoreCase(key)) {
            throw new BadRequestException("Données invalides", Map.of("role", "Le rôle ne peut pas être fourni lors de l'inscription publique"));
        }
    }

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
}
