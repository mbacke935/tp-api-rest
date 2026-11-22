package sn.ucak.dar.tpapirest; // Déclare le package de test

import org.junit.jupiter.api.Test; // Importe l'annotation @Test de JUnit 5
import sn.ucak.dar.tpapirest.entities.Role; // Importe l'énumération des rôles
import sn.ucak.dar.tpapirest.services.ValidationUtils; // Importe la classe utilitaire de validation à tester

import java.math.BigDecimal; // Importe BigDecimal pour tester les prix
import java.util.LinkedHashMap; // Importe LinkedHashMap pour conserver l'ordre des erreurs
import java.util.Map; // Importe l'interface Map

import static org.junit.jupiter.api.Assertions.*; // Importe les assertions statiques JUnit

class ValidationUtilsTest { // Classe de tests unitaires pour ValidationUtils

    private final ValidationUtils validator = new ValidationUtils(); // Instance de validation testée

    // ─────────────────────────── NOM ───────────────────────────

    @Test // Teste les cas valides et les bornes de longueur du nom
    void testNameValidationBoundaries() { // Vérifie le nettoyage des espaces et les tailles 2 et 100
        Map<String, String> errors = new LinkedHashMap<>(); // Map recevant les éventuelles erreurs

        // Valides et aux limites
        assertEquals("Clavier", validator.validateAndNormalizeName("  Clavier  ", errors)); // Retire les espaces périphériques
        assertTrue(errors.isEmpty(), "Espaces périphériques retirés"); // Aucune erreur attendue

        assertEquals("ab", validator.validateAndNormalizeName("ab", errors)); // Longueur minimale valide : 2
        assertEquals("a".repeat(100), validator.validateAndNormalizeName("a".repeat(100), errors)); // Longueur maximale valide : 100
        assertTrue(errors.isEmpty(), "2 et 100 caractères valides"); // Aucune erreur attendue
    }

    @Test // Teste un nom composé uniquement d'espaces
    void testNameInvalidEmpty() { // Vérifie le rejet des espaces seuls
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        validator.validateAndNormalizeName("   ", errors); // Valide une chaîne d'espaces
        assertTrue(errors.containsKey("name"), "Espaces seuls → invalide"); // Vérifie la présence d'une erreur sur 'name'
    }

    @Test // Teste un nom null
    void testNameInvalidNull() { // Vérifie le rejet de null
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        validator.validateAndNormalizeName(null, errors); // Valide null
        assertTrue(errors.containsKey("name"), "null → invalide"); // Vérifie l'erreur sur 'name'
    }

    @Test // Teste un nom d'un seul caractère après nettoyage
    void testNameTooShort() { // Vérifie le rejet de la longueur 1
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        validator.validateAndNormalizeName("a", errors); // Valide une chaîne de 1 caractère
        assertTrue(errors.containsKey("name"), "1 caractère → invalide"); // Vérifie l'erreur sur 'name'
    }

    @Test // Teste un nom de 101 caractères
    void testNameTooLong() { // Vérifie le rejet au-delà de 100 caractères
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        validator.validateAndNormalizeName("a".repeat(101), errors); // Valide une chaîne de 101 caractères
        assertTrue(errors.containsKey("name"), "101 caractères → invalide"); // Vérifie l'erreur sur 'name'
    }

    // ─────────────────────────── EMAIL ───────────────────────────

    @Test // Teste la normalisation et la limite exacte de 254 caractères
    void testEmailNormalizationAndBoundaries() { // Vérifie trim, minuscules et longueur 254
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs

        // Normalisation : trim + lowercase
        assertEquals("user.test@example.com", // Résultat attendu normalisé
                validator.validateAndNormalizeEmail("  User.Test@EXAMPLE.COM  ", errors)); // Entrée avec espaces et majuscules
        assertTrue(errors.isEmpty()); // Aucune erreur attendue

        // Email de 254 caractères (64 local + 1 '@' + 189 domaine = 254)
        String local = "a".repeat(64); // Partie locale de 64 caractères
        String domain = "b".repeat(62) + "." + "c".repeat(61) + "." + "d".repeat(61) + ".sn"; // Domaine de 189 caractères
        String maxEmail = local + "@" + domain; // Concaténation formant l'email complet
        assertEquals(254, maxEmail.length()); // Vérifie la longueur exacte de 254 caractères
        String result = validator.validateAndNormalizeEmail(maxEmail, errors); // Exécute la validation
        assertEquals(maxEmail, result); // Vérifie la valeur retournée
        assertTrue(errors.isEmpty(), "Email de 254 caractères → valide"); // Aucune erreur attendue
    }

    @Test // Teste un email dépassant la limite de 254 caractères (255 caractères)
    void testEmailTooLong() { // Vérifie le rejet d'un email de 255 caractères
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        // 255 caractères → invalide
        String local = "a".repeat(64); // Partie locale de 64 caractères
        String domain = "b".repeat(62) + "." + "c".repeat(61) + "." + "d".repeat(61) + ".sn"; // Domaine de 189 caractères
        String maxEmail = local + "@" + domain; // Email de 254 caractères
        validator.validateAndNormalizeEmail("a" + maxEmail, errors); // Ajoute 1 caractère pour atteindre 255 caractères
        assertTrue(errors.containsKey("email"), "Email de 255 chars → invalide"); // Vérifie l'erreur sur 'email'
    }

    @Test // Teste un email au format invalide
    void testEmailInvalidFormat() { // Vérifie le rejet d'une chaîne sans '@' ni domaine
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        validator.validateAndNormalizeEmail("pasUnEmail", errors); // Valide une chaîne mal formée
        assertTrue(errors.containsKey("email"), "Format invalide → erreur"); // Vérifie l'erreur sur 'email'
    }

    @Test // Teste un email null
    void testEmailNull() { // Vérifie le rejet de null
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        validator.validateAndNormalizeEmail(null, errors); // Valide null
        assertTrue(errors.containsKey("email"), "null → invalide"); // Vérifie l'erreur sur 'email'
    }

    @Test // Teste un email vide
    void testEmailEmpty() { // Vérifie le rejet d'une chaîne vide
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        validator.validateAndNormalizeEmail("", errors); // Valide une chaîne vide
        assertTrue(errors.containsKey("email"), "Vide → invalide"); // Vérifie l'erreur sur 'email'
    }

    // ─────────────────────────── MOT DE PASSE ───────────────────────────

    @Test // Teste la conservation des espaces et les bornes 8 et 72 caractères
    void testPasswordPreservesSpacesAndChecksBounds() { // Vérifie les longueurs valides du mot de passe
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs

        // 8 caractères avec espaces conservés
        validator.validatePassword("  1234  ", true, errors); // 2 espaces + 4 chiffres + 2 espaces = 8 caractères
        assertTrue(errors.isEmpty(), "8 chars (avec espaces) → valide"); // Aucune erreur attendue

        // 72 caractères → limite haute valide
        validator.validatePassword("a".repeat(72), true, errors); // Longueur maximale de 72 caractères
        assertTrue(errors.isEmpty(), "72 chars → valide"); // Aucune erreur attendue
    }

    @Test // Teste un mot de passe trop court (7 caractères)
    void testPasswordTooShort() { // Vérifie le rejet à 7 caractères
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        validator.validatePassword("1234567", true, errors); // 7 caractères
        assertTrue(errors.containsKey("password"), "7 chars → invalide"); // Vérifie l'erreur sur 'password'
    }

    @Test // Teste un mot de passe trop long (73 caractères)
    void testPasswordTooLong() { // Vérifie le rejet à 73 caractères
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        validator.validatePassword("a".repeat(73), true, errors); // 73 caractères
        assertTrue(errors.containsKey("password"), "73 chars → invalide"); // Vérifie l'erreur sur 'password'
    }

    @Test // Teste un mot de passe null lorsqu'il est obligatoire
    void testPasswordNullWhenRequired() { // Vérifie le rejet de null à la création
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        validator.validatePassword(null, true, errors); // Mot de passe obligatoire absent
        assertTrue(errors.containsKey("password"), "null + obligatoire → invalide"); // Vérifie l'erreur sur 'password'
    }

    @Test // Teste un mot de passe null lorsqu'il est facultatif (modification)
    void testPasswordNullWhenOptional() { // Vérifie que null est accepté si le mot de passe est omis en mise à jour
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        validator.validatePassword(null, false, errors); // Mot de passe facultatif absent
        assertFalse(errors.containsKey("password"), "null + optionnel → OK"); // Aucune erreur sur 'password'
    }

    // ─────────────────────────── RÔLE ───────────────────────────

    @Test // Teste les valeurs valides de l'énumération Role
    void testRoleValidValues() { // Vérifie USER et ADMIN
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        assertEquals(Role.USER, validator.validateRole("USER", errors)); // Vérifie le rôle USER
        assertEquals(Role.ADMIN, validator.validateRole("ADMIN", errors)); // Vérifie le rôle ADMIN
        assertTrue(errors.isEmpty(), "USER et ADMIN → valides"); // Aucune erreur attendue
    }

    @Test // Teste une valeur de rôle inconnue
    void testRoleInvalidValue() { // Vérifie le rejet d'une valeur hors énumération
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        validator.validateRole("SUPERADMIN", errors); // Rôle non autorisé
        assertTrue(errors.containsKey("role"), "Valeur hors énumération → invalide"); // Vérifie l'erreur sur 'role'
    }

    @Test // Teste un rôle null
    void testRoleNull() { // Vérifie le rejet de null
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        validator.validateRole(null, errors); // Rôle null
        assertTrue(errors.containsKey("role"), "null → invalide"); // Vérifie l'erreur sur 'role'
    }

    @Test // Teste un rôle vide
    void testRoleEmpty() { // Vérifie le rejet d'une chaîne vide
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        validator.validateRole("", errors); // Rôle vide
        assertTrue(errors.containsKey("role"), "Vide → invalide"); // Vérifie l'erreur sur 'role'
    }

    // ─────────────────────────── DESCRIPTION ───────────────────────────

    @Test // Teste les cas valides de description (null, vide, 1000 caractères)
    void testDescriptionValidCases() { // Vérifie les bornes de la description produit
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs

        // null → autorisé (description facultative)
        assertNull(validator.validateDescription(null, errors)); // Retourne null sans erreur
        assertTrue(errors.isEmpty(), "null → autorisé"); // Aucune erreur attendue

        // vide → autorisé
        assertEquals("", validator.validateDescription("", errors)); // Retourne chaîne vide
        assertTrue(errors.isEmpty(), "Vide → autorisé"); // Aucune erreur attendue

        // Exactement 1000 caractères → valide
        String desc1000 = "a".repeat(1000); // Chaîne de 1000 caractères
        assertEquals(desc1000, validator.validateDescription(desc1000, errors)); // Vérifie la valeur retournée
        assertTrue(errors.isEmpty(), "1000 chars → valide"); // Aucune erreur attendue
    }

    @Test // Teste une description de 1001 caractères
    void testDescriptionTooLong() { // Vérifie le rejet au-delà de 1000 caractères
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        validator.validateDescription("a".repeat(1001), errors); // Chaîne de 1001 caractères
        assertTrue(errors.containsKey("description"), "1001 chars → invalide"); // Vérifie l'erreur sur 'description'
    }

    // ─────────────────────────── PRIX ───────────────────────────

    @Test // Teste les valeurs de prix valides
    void testPriceValidCases() { // Vérifie 0.01, 1 et 12.34
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        assertEquals(new BigDecimal("0.01"), validator.validatePrice(new BigDecimal("0.01"), errors)); // Prix minimal valide
        assertEquals(new BigDecimal("1"), validator.validatePrice(new BigDecimal("1"), errors)); // Entier positif valide
        assertEquals(new BigDecimal("12.34"), validator.validatePrice(new BigDecimal("12.34"), errors)); // Décimal à 2 chiffres valide
        assertTrue(errors.isEmpty(), "0.01, 1 et 12.34 → valides"); // Aucune erreur attendue
    }

    @Test // Teste un prix nul (0)
    void testPriceZero() { // Vérifie le rejet de zéro
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        validator.validatePrice(BigDecimal.ZERO, errors); // Prix égal à 0
        assertTrue(errors.containsKey("price"), "0 → invalide"); // Vérifie l'erreur sur 'price'
    }

    @Test // Teste un prix négatif
    void testPriceNegative() { // Vérifie le rejet d'un montant négatif
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        validator.validatePrice(new BigDecimal("-5.00"), errors); // Prix négatif
        assertTrue(errors.containsKey("price"), "Négatif → invalide"); // Vérifie l'erreur sur 'price'
    }

    @Test // Teste un prix avec 3 décimales non nulles (1.234)
    void testPriceTooManyDecimals_1234() { // Vérifie le rejet de 1.234
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        validator.validatePrice(new BigDecimal("1.234"), errors); // 3 décimales
        assertTrue(errors.containsKey("price"), "1.234 → invalide (3 décimales)"); // Vérifie l'erreur sur 'price'
    }

    @Test // Teste un prix avec un zéro trailing à la 3e décimale (1.230)
    void testPriceTooManyDecimals_1230() { // Vérifie le rejet de 1.230 sans arrondi
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        // 1.230 a un scale de 3 → doit être refusé sans arrondi
        validator.validatePrice(new BigDecimal("1.230"), errors); // Scale égal à 3
        assertTrue(errors.containsKey("price"), "1.230 → invalide (scale 3)"); // Vérifie l'erreur sur 'price'
    }

    @Test // Teste un prix null
    void testPriceNull() { // Vérifie le rejet de null
        Map<String, String> errors = new LinkedHashMap<>(); // Map d'erreurs
        validator.validatePrice(null, errors); // Prix absent
        assertTrue(errors.containsKey("price"), "null → invalide"); // Vérifie l'erreur sur 'price'
    }
}