package sn.ucak.dar.tpapirest.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.ucak.dar.tpapirest.dto.AdminUserRequest;
import sn.ucak.dar.tpapirest.dto.RegisterRequest;
import sn.ucak.dar.tpapirest.dto.UserResponse;
import sn.ucak.dar.tpapirest.entities.Role;
import sn.ucak.dar.tpapirest.entities.User;
import sn.ucak.dar.tpapirest.exceptions.BadRequestException;
import sn.ucak.dar.tpapirest.exceptions.ConflictException;
import sn.ucak.dar.tpapirest.exceptions.ResourceNotFoundException;
import sn.ucak.dar.tpapirest.repositories.UserRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ValidationUtils validationUtils;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       ValidationUtils validationUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validationUtils = validationUtils;
    }

    @Transactional
    public UserResponse registerPublicUser(RegisterRequest request) {
        if (request == null) {
            throw new BadRequestException("Corps de requête obligatoire");
        }
        Map<String, String> errors = new LinkedHashMap<>();
        String cleanName = validationUtils.validateAndNormalizeName(request.getName(), errors);
        String cleanEmail = validationUtils.validateAndNormalizeEmail(request.getEmail(), errors);
        validationUtils.validatePassword(request.getPassword(), true, errors);
        validationUtils.throwIfErrors(errors);

        if (userRepository.existsByEmail(cleanEmail)) {
            throw new ConflictException(
                "Conflit sur l'adresse email",
                Map.of("email", "Cette adresse email est déjà utilisée")
            );
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(cleanName, cleanEmail, hashedPassword, Role.USER);
        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
            .stream()
            .map(UserResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse createUserByAdmin(AdminUserRequest request) {
        if (request == null) {
            throw new BadRequestException("Corps de requête obligatoire");
        }
        Map<String, String> errors = new LinkedHashMap<>();
        String cleanName = validationUtils.validateAndNormalizeName(request.getName(), errors);
        String cleanEmail = validationUtils.validateAndNormalizeEmail(request.getEmail(), errors);
        validationUtils.validatePassword(request.getPassword(), true, errors);
        Role validRole = validationUtils.validateRole(request.getRole(), errors);
        validationUtils.throwIfErrors(errors);

        if (userRepository.existsByEmail(cleanEmail)) {
            throw new ConflictException(
                "Conflit sur l'adresse email",
                Map.of("email", "Cette adresse email est déjà utilisée")
            );
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(cleanName, cleanEmail, hashedPassword, validRole);
        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    @Transactional
    public UserResponse updateUserByAdmin(UUID id, AdminUserRequest request, boolean passwordFieldPresent) {
        if (request == null) {
            throw new BadRequestException("Corps de requête obligatoire");
        }
        User existing = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Map<String, String> errors = new LinkedHashMap<>();
        String cleanName = validationUtils.validateAndNormalizeName(request.getName(), errors);
        String cleanEmail = validationUtils.validateAndNormalizeEmail(request.getEmail(), errors);
        Role validRole = validationUtils.validateRole(request.getRole(), errors);

        if (passwordFieldPresent) {
            if (request.getPassword() == null) {
                errors.put("password", "Le mot de passe fourni ne peut pas être null");
            } else {
                validationUtils.validatePassword(request.getPassword(), true, errors);
            }
        }
        validationUtils.throwIfErrors(errors);

        if (userRepository.existsByEmailAndIdNot(cleanEmail, id)) {
            throw new ConflictException(
                "Conflit sur l'adresse email",
                Map.of("email", "Cette adresse email est déjà utilisée")
            );
        }

        existing.setName(cleanName);
        existing.setEmail(cleanEmail);
        existing.setRole(validRole);
        if (passwordFieldPresent) {
            existing.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updated = userRepository.save(existing);
        return UserResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User existing = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        userRepository.delete(existing);
    }
}
