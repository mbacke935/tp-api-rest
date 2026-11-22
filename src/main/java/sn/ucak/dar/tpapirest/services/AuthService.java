package sn.ucak.dar.tpapirest.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.ucak.dar.tpapirest.dto.AuthResponse;
import sn.ucak.dar.tpapirest.dto.LoginRequest;
import sn.ucak.dar.tpapirest.entities.User;
import sn.ucak.dar.tpapirest.exceptions.BadRequestException;
import sn.ucak.dar.tpapirest.exceptions.UnauthorizedException;
import sn.ucak.dar.tpapirest.repositories.UserRepository;
import sn.ucak.dar.tpapirest.security.JwtService;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MSG = "Identifiants invalides";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ValidationUtils validationUtils;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       ValidationUtils validationUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.validationUtils = validationUtils;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        if (request == null) {
            throw new BadRequestException("Corps de requête obligatoire");
        }
        Map<String, String> errors = new LinkedHashMap<>();
        String normalizedEmail = validationUtils.validateAndNormalizeEmail(request.getEmail(), errors);
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            errors.put("password", "Le mot de passe est obligatoire");
        }
        validationUtils.throwIfErrors(errors);

        User user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS_MSG));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException(INVALID_CREDENTIALS_MSG);
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, jwtService.getTtlSeconds());
    }
}
