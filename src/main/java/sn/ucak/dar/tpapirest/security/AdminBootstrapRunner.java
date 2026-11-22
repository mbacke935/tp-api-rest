package sn.ucak.dar.tpapirest.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.ucak.dar.tpapirest.entities.Role;
import sn.ucak.dar.tpapirest.entities.User;
import sn.ucak.dar.tpapirest.repositories.UserRepository;
import sn.ucak.dar.tpapirest.services.ValidationUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Profile("bootstrap")
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ValidationUtils validationUtils;
    private final String adminName;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrapRunner(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        ValidationUtils validationUtils,
        @Value("${BOOTSTRAP_ADMIN_NAME:}") String adminName,
        @Value("${BOOTSTRAP_ADMIN_EMAIL:}") String adminEmail,
        @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validationUtils = validationUtils;
        this.adminName = adminName;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("[BOOTSTRAP] Un compte ADMIN existe déjà. Aucune modification effectuée.");
            return;
        }

        Map<String, String> errors = new LinkedHashMap<>();
        String cleanName = validationUtils.validateAndNormalizeName(
            adminName != null && !adminName.isEmpty() ? adminName : null, errors
        );
        String cleanEmail = validationUtils.validateAndNormalizeEmail(
            adminEmail != null && !adminEmail.isEmpty() ? adminEmail : null, errors
        );
        validationUtils.validatePassword(
            adminPassword != null && !adminPassword.isEmpty() ? adminPassword : null, true, errors
        );

        if (!errors.isEmpty()) {
            throw new IllegalStateException(
                "[BOOTSTRAP] Variables BOOTSTRAP_ADMIN_* manquantes ou invalides : " + errors
            );
        }

        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalStateException(
                "[BOOTSTRAP] L'adresse email spécifiée est déjà utilisée par un compte existant. Initialisation arrêtée sans promotion."
            );
        }

        User admin = new User(cleanName, cleanEmail, passwordEncoder.encode(adminPassword), Role.ADMIN);
        userRepository.save(admin);
        log.info("[BOOTSTRAP] Premier compte ADMIN créé avec succès. Arrêtez l'application, retirez le profil bootstrap et redémarrez sous le profil tp.");
    }
}
