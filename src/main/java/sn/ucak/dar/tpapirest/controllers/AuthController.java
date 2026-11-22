package sn.ucak.dar.tpapirest.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import sn.ucak.dar.tpapirest.dto.*;
import sn.ucak.dar.tpapirest.services.AuthService;
import sn.ucak.dar.tpapirest.services.UserService;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification", description = "Inscription publique et obtention de jeton JWT")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Inscrire un compte USER", description = "Route publique créant un utilisateur avec le rôle USER.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Compte créé", content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides ou rôle fourni", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Email déjà utilisé", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> register(@RequestBody(required = false) RegisterRequest request) {
        UserResponse created = userService.registerPublicUser(request);
        URI location = ServletUriComponentsBuilder
            .fromCurrentContextPath()
            .path("/api/users/{id}")
            .buildAndExpand(created.getId())
            .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PostMapping("/login")
    @Operation(summary = "Authentifier et émettre un JWT", description = "Route publique retournant un jeton Bearer HS256.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentification réussie", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Requête mal formée", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Identifiants invalides", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> login(@RequestBody(required = false) LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
