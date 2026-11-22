package sn.ucak.dar.tpapirest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import sn.ucak.dar.tpapirest.entities.Role;
import sn.ucak.dar.tpapirest.entities.User;
import sn.ucak.dar.tpapirest.repositories.ProductRepository;
import sn.ucak.dar.tpapirest.repositories.UserRepository;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSecurityAndCrudIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Value("${app.jwt.secret-base64}")
    private String secretBase64;

    private User adminUser;
    private User standardUser;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = userRepository.save(
            new User("Admin Test", "admin@ucak.sn", passwordEncoder.encode("AdminPass123!"), Role.ADMIN)
        );
        standardUser = userRepository.save(
            new User("User Test", "user@ucak.sn", passwordEncoder.encode("UserPass123!"), Role.USER)
        );
    }

    // ─────────────── HELPERS ───────────────

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", password));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    private String createProductAsAdmin(String adminToken, String name, String description, String price) throws Exception {
        String body = description != null
            ? String.format("{\"name\":\"%s\",\"description\":\"%s\",\"price\":%s}", name, description, price)
            : String.format("{\"name\":\"%s\",\"price\":%s}", name, price);
        MvcResult res = mockMvc.perform(post("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asText();
    }

    // ─────────────── SWAGGER PUBLIC ───────────────

    @Test
    void testPublicSwaggerAccess() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk());
    }

    // ─────────────── INSCRIPTION ───────────────

    @Test
    void testPublicRegistrationAndRoleRejection() throws Exception {
        // Inscription valide avec normalisation (trim + lowercase)
        String validPayload = "{\"name\": \"  Awa Sall  \", \"email\": \"  AWA.SALL@Example.COM \", \"password\": \"  Secret 123 \"}";
        MvcResult res = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validPayload))
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andExpect(jsonPath("$.name").value("Awa Sall"))
            .andExpect(jsonPath("$.email").value("awa.sall@example.com"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andReturn();

        // Le mot de passe ne doit jamais apparaître dans la réponse
        assertFalse(res.getResponse().getContentAsString().contains("Secret 123"));

        // Rôle fourni à l'inscription → 400
        String withRolePayload = "{\"name\": \"Cheikh\", \"email\": \"cheikh@example.com\", \"password\": \"Password123\", \"role\": \"ADMIN\"}";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(withRolePayload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));

        // Email dupliqué (après normalisation) → 409
        String duplicateEmailPayload = "{\"name\": \"Awa Bis\", \"email\": \"awa.sall@example.com\", \"password\": \"Password123\"}";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicateEmailPayload))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void testRegistrationValidationErrors() throws Exception {
        // Nom absent → 400
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ok@example.com\",\"password\":\"Password123\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.name").exists());

        // Email format invalide → 400
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Jean\",\"email\":\"pasUnEmail\",\"password\":\"Password123\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.email").exists());

        // Password de 7 chars → 400
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Jean\",\"email\":\"jean@example.com\",\"password\":\"1234567\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.password").exists());

        // Nom de 1 caractère (après trim) → 400
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"A\",\"email\":\"a@example.com\",\"password\":\"Password123\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.name").exists());

        // Nom de 2 caractères (borne basse valide) → 201
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"AB\",\"email\":\"ab@example.com\",\"password\":\"Password123\"}"))
            .andExpect(status().isCreated());

        // Nom de 101 caractères (après trim) → 400
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + "a".repeat(101) + "\",\"email\":\"long101@example.com\",\"password\":\"Password123\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.name").exists());

        // Nom de 100 caractères (borne haute valide) → 201
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + "a".repeat(100) + "\",\"email\":\"long100@example.com\",\"password\":\"Password123\"}"))
            .andExpect(status().isCreated());

        // Corps JSON mal formé → 400
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Jean\","))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    // ─────────────── AUTHENTIFICATION ET TOKENS ───────────────

    @Test
    void testAuthenticationErrorsAndExpiredToken() throws Exception {
        // Email inconnu → 401
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"inconnu@ucak.sn\",\"password\":\"Password123\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));

        // Mauvais mot de passe → 401 (même message générique)
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@ucak.sn\",\"password\":\"MauvaisMotDePasse\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));

        // Requête sans token → 401 JSON
        mockMvc.perform(get("/api/products"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));

        // Token modifié/invalide → 401
        mockMvc.perform(get("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer tokeninvalide.xxx.yyy"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));

        // Token expiré → 401
        var key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretBase64));
        String expiredToken = Jwts.builder()
            .subject(adminUser.getId().toString())
            .issuedAt(Date.from(Instant.now().minusSeconds(600)))
            .expiration(Date.from(Instant.now().minusSeconds(60)))
            .signWith(key, Jwts.SIG.HS256)
            .compact();

        mockMvc.perform(get("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));

        // Corps JSON mal formé à la connexion → 400
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@ucak.sn\""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    // ─────────────── CRUD PRODUITS ───────────────

    @Test
    void testProductCrudAndRoleEnforcement() throws Exception {
        String adminToken = loginAndGetToken("admin@ucak.sn", "AdminPass123!");
        String userToken = loginAndGetToken("user@ucak.sn", "UserPass123!");

        // ADMIN crée un produit → 201 + Location + normalisation du nom
        MvcResult createRes = mockMvc.perform(post("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"  Ordinateur Portable  \",\"description\":\"Core i7\",\"price\":1250.50}"))
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andExpect(jsonPath("$.name").value("Ordinateur Portable"))
            .andExpect(jsonPath("$.price").value(1250.5))
            .andReturn();

        String productId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asText();

        // USER consulte un produit → 200
        mockMvc.perform(get("/api/products/" + productId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(productId));

        // USER liste les produits → 200
        mockMvc.perform(get("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
            .andExpect(status().isOk());

        // USER tente de créer → 403
        mockMvc.perform(post("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Souris\",\"price\":15.00}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));

        // USER tente de modifier → 403
        mockMvc.perform(put("/api/products/" + productId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Souris\",\"price\":15.00}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));

        // USER tente de supprimer → 403
        mockMvc.perform(delete("/api/products/" + productId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));

        // ADMIN modifie avec prix invalide (1.230 → 3 décimales) → 400, produit inchangé
        mockMvc.perform(put("/api/products/" + productId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ordinateur\",\"price\":1.230}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.price").exists());

        // Vérifier que le produit est inchangé
        mockMvc.perform(get("/api/products/" + productId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Ordinateur Portable"));

        // ADMIN modifie correctement → 200
        mockMvc.perform(put("/api/products/" + productId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ordinateur Bureau\",\"price\":999.99}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Ordinateur Bureau"))
            .andExpect(jsonPath("$.price").value(999.99));

        // ADMIN supprime → 204
        mockMvc.perform(delete("/api/products/" + productId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isNoContent());

        // Consultation après suppression → 404
        mockMvc.perform(get("/api/products/" + productId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));

        // Deuxième suppression → 404
        mockMvc.perform(delete("/api/products/" + productId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void testProductValidationErrors() throws Exception {
        String adminToken = loginAndGetToken("admin@ucak.sn", "AdminPass123!");

        // Nom absent → 400
        mockMvc.perform(post("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"price\":10.00}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.name").exists());

        // Prix null → 400
        mockMvc.perform(post("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.price").exists());

        // Prix négatif → 400
        mockMvc.perform(post("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\",\"price\":-1.00}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.price").exists());

        // Prix zéro → 400
        mockMvc.perform(post("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\",\"price\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.price").exists());

        // Description de 1001 caractères → 400
        String desc1001 = "a".repeat(1001);
        mockMvc.perform(post("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\",\"description\":\"" + desc1001 + "\",\"price\":10.00}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.description").exists());

        // Description de 1000 caractères → valide
        String desc1000 = "a".repeat(1000);
        mockMvc.perform(post("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test valide\",\"description\":\"" + desc1000 + "\",\"price\":10.00}"))
            .andExpect(status().isCreated());

        // Nom de produit de 1 caractère (après trim) → 400
        mockMvc.perform(post("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"A\",\"price\":10.00}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.name").exists());

        // Nom de produit de 2 caractères (borne basse valide) → 201
        mockMvc.perform(post("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"AB\",\"price\":10.00}"))
            .andExpect(status().isCreated());

        // Nom de produit de 101 caractères (après trim) → 400
        mockMvc.perform(post("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + "a".repeat(101) + "\",\"price\":10.00}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.name").exists());

        // Nom de produit de 100 caractères (borne haute valide) → 201
        mockMvc.perform(post("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + "a".repeat(100) + "\",\"price\":10.00}"))
            .andExpect(status().isCreated());

        // Prix 0.01 (borne basse valide) → 201
        mockMvc.perform(post("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Produit 0.01\",\"price\":0.01}"))
            .andExpect(status().isCreated());

        // Prix 1.230 (3 décimales) → 400
        mockMvc.perform(post("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\",\"price\":1.230}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.price").exists());

        // Identifiant mal formé (non-UUID) → 400
        mockMvc.perform(get("/api/products/pasuuid")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testProductListEmptyReturns200() throws Exception {
        String userToken = loginAndGetToken("user@ucak.sn", "UserPass123!");
        mockMvc.perform(get("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // ─────────────── CRUD UTILISATEURS ───────────────

    @Test
    void testUserManagementDynamicRoleDowngradeAndAccountDeletion() throws Exception {
        String adminToken = loginAndGetToken("admin@ucak.sn", "AdminPass123!");

        // Création d'un second ADMIN par ADMIN
        MvcResult createAdminRes = mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Second Admin\",\"email\":\"admin2@ucak.sn\",\"password\":\"PassAdmin2!\",\"role\":\"ADMIN\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andReturn();

        String admin2Id = objectMapper.readTree(createAdminRes.getResponse().getContentAsString()).get("id").asText();
        String admin2Token = loginAndGetToken("admin2@ucak.sn", "PassAdmin2!");

        // admin2 avec rôle ADMIN peut lister les utilisateurs
        mockMvc.perform(get("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin2Token))
            .andExpect(status().isOk());

        // Rétrogradation de admin2 → USER
        mockMvc.perform(put("/api/users/" + admin2Id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Second Admin\",\"email\":\"admin2@ucak.sn\",\"role\":\"USER\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("USER"));

        // Avec l'ancien token ADMIN, admin2 reçoit 403 dès la requête suivante
        mockMvc.perform(get("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin2Token))
            .andExpect(status().isForbidden());

        // admin2 peut toujours se connecter (compte existant)
        loginAndGetToken("admin2@ucak.sn", "PassAdmin2!");

        // Suppression de admin2 par admin
        mockMvc.perform(delete("/api/users/" + admin2Id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isNoContent());

        // Après suppression, le token admin2 reçoit 401
        mockMvc.perform(get("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin2Token))
            .andExpect(status().isUnauthorized());

        // Deuxième suppression du même compte → 404
        mockMvc.perform(delete("/api/users/" + admin2Id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void testUserValidationErrors() throws Exception {
        String adminToken = loginAndGetToken("admin@ucak.sn", "AdminPass123!");

        // Rôle invalide → 400
        mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\",\"email\":\"test@example.com\",\"password\":\"Password123\",\"role\":\"MANAGER\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.role").exists());

        // Rôle manquant → 400
        mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\",\"email\":\"test@example.com\",\"password\":\"Password123\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.role").exists());

        // Email de 255 caractères → 400
        String local = "a".repeat(64);
        String domain = "b".repeat(61) + "." + "c".repeat(61) + "." + "d".repeat(61) + ".sn";
        String tooLongEmail = "a" + local + "@" + domain;
        mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\",\"email\":\"" + tooLongEmail + "\",\"password\":\"Password123\",\"role\":\"USER\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.email").exists());

        // Mot de passe de 73 caractères → 400 (borne haute dépassée)
        mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\",\"email\":\"test73chars@example.com\",\"password\":\"" + "a".repeat(73) + "\",\"role\":\"USER\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.password").exists());

        // Mot de passe de 72 caractères → 201 (borne haute valide)
        mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test 72\",\"email\":\"test72chars@example.com\",\"password\":\"" + "a".repeat(72) + "\",\"role\":\"USER\"}"))
            .andExpect(status().isCreated());

        // Identifiant mal formé (non-UUID) → 400
        mockMvc.perform(get("/api/users/pasuuid")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));

        // Utilisateur inexistant → 404
        mockMvc.perform(get("/api/users/00000000-0000-0000-0000-000000000000")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testEmailConflict409OnCreationAndUpdate() throws Exception {
        String adminToken = loginAndGetToken("admin@ucak.sn", "AdminPass123!");

        // Création avec email déjà utilisé → 409
        mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Doublon\",\"email\":\"admin@ucak.sn\",\"password\":\"Password123\",\"role\":\"USER\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.errors.email").exists());

        // 409 après normalisation (majuscules / espaces)
        mockMvc.perform(post("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Doublon\",\"email\":\"  ADMIN@UCAK.SN  \",\"password\":\"Password123\",\"role\":\"USER\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));

        // Un utilisateur peut conserver son propre email en modification → 200
        String userId = standardUser.getId().toString();
        mockMvc.perform(put("/api/users/" + userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"User Test Updated\",\"email\":\"user@ucak.sn\",\"role\":\"USER\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("user@ucak.sn"));

        // Modification vers un email appartenant à un autre compte → 409
        mockMvc.perform(put("/api/users/" + userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"User Test\",\"email\":\"admin@ucak.sn\",\"role\":\"USER\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void testPasswordChangeAndHashPreservation() throws Exception {
        String adminToken = loginAndGetToken("admin@ucak.sn", "AdminPass123!");
        String userId = standardUser.getId().toString();

        // Changement de mot de passe
        mockMvc.perform(put("/api/users/" + userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"User Test\",\"email\":\"user@ucak.sn\",\"role\":\"USER\",\"password\":\"NouveauPass456!\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.password").doesNotExist());

        // Connexion avec le nouveau mot de passe → 200
        loginAndGetToken("user@ucak.sn", "NouveauPass456!");

        // Connexion avec l'ancien mot de passe → 401
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@ucak.sn\",\"password\":\"UserPass123!\"}"))
            .andExpect(status().isUnauthorized());

        // Modification sans password → hash conservé (connexion toujours possible)
        mockMvc.perform(put("/api/users/" + userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"User Test Renamed\",\"email\":\"user@ucak.sn\",\"role\":\"USER\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("User Test Renamed"));

        // Connexion avec l'ancien nouveau mdp → toujours 200
        loginAndGetToken("user@ucak.sn", "NouveauPass456!");
    }

    @Test
    void testPasswordNullOnPutReturns400() throws Exception {
        String adminToken = loginAndGetToken("admin@ucak.sn", "AdminPass123!");
        String userId = standardUser.getId().toString();

        // password fourni mais null → 400
        mockMvc.perform(put("/api/users/" + userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"User Test\",\"email\":\"user@ucak.sn\",\"role\":\"USER\",\"password\":null}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void testUserResponseNeverContainsPassword() throws Exception {
        String adminToken = loginAndGetToken("admin@ucak.sn", "AdminPass123!");

        // GET liste
        MvcResult res = mockMvc.perform(get("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn();
        assertFalse(res.getResponse().getContentAsString().contains("password"),
            "La liste des utilisateurs ne doit pas contenir le champ password");

        // GET par id
        MvcResult resById = mockMvc.perform(get("/api/users/" + adminUser.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn();
        assertFalse(resById.getResponse().getContentAsString().contains("$2a$"),
            "La réponse ne doit pas contenir le hachage BCrypt");
    }

    @Test
    void testUserAccessReservedToAdmin() throws Exception {
        String userToken = loginAndGetToken("user@ucak.sn", "UserPass123!");

        mockMvc.perform(get("/api/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/users/" + adminUser.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    // ─────────────── CONTRAINTE BDD UNIQUE EMAIL ───────────────

    @Test
    void testDatabaseUniqueConstraintDirectly() {
        User u1 = new User("User One", "unique.db@ucak.sn", passwordEncoder.encode("Password123"), Role.USER);
        userRepository.saveAndFlush(u1);

        // Contournement du service → la contrainte PostgreSQL doit refuser
        User u2 = new User("User Two", "unique.db@ucak.sn", passwordEncoder.encode("Password456"), Role.USER);
        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(u2),
            "La contrainte UNIQUE en base doit refuser les doublons d'email");
    }
}
