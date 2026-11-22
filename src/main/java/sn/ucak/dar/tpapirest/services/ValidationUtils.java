package sn.ucak.dar.tpapirest.services;

import org.springframework.stereotype.Component;
import sn.ucak.dar.tpapirest.entities.Role;
import sn.ucak.dar.tpapirest.exceptions.BadRequestException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^(?=.{1,254}$)(?=.{1,64}@)[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@" +
        "(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$"
    );

    public String validateAndNormalizeName(String rawName, Map<String, String> errors) {
        if (rawName == null) {
            errors.put("name", "Le nom est obligatoire");
            return null;
        }
        String trimmed = rawName.trim();
        if (trimmed.isEmpty()) {
            errors.put("name", "Le nom ne doit pas être vide ni composé uniquement d'espaces");
            return null;
        }
        int codePoints = trimmed.codePointCount(0, trimmed.length());
        if (codePoints < 2 || codePoints > 100) {
            errors.put("name", "Le nom doit contenir entre 2 et 100 caractères après nettoyage");
            return null;
        }
        return trimmed;
    }

    public String validateAndNormalizeEmail(String rawEmail, Map<String, String> errors) {
        if (rawEmail == null) {
            errors.put("email", "L'adresse email est obligatoire");
            return null;
        }
        String normalized = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            errors.put("email", "L'adresse email est obligatoire");
            return null;
        }
        if (normalized.length() > 254 || !EMAIL_PATTERN.matcher(normalized).matches()) {
            errors.put("email", "Le format de l'adresse email est invalide ou dépasse 254 caractères");
            return null;
        }
        return normalized;
    }

    public void validatePassword(String rawPassword, boolean required, Map<String, String> errors) {
        if (rawPassword == null) {
            if (required) {
                errors.put("password", "Le mot de passe est obligatoire");
            }
            return;
        }
        int codePointLength = rawPassword.codePointCount(0, rawPassword.length());
        if (codePointLength < 8) {
            errors.put("password", "Le mot de passe doit contenir au moins 8 caractères");
            return;
        }
        if (codePointLength > 72) {
            errors.put("password", "Le mot de passe ne doit pas dépasser 72 caractères");
        }
    }

    public Role validateRole(String rawRole, Map<String, String> errors) {
        if (rawRole == null || rawRole.trim().isEmpty()) {
            errors.put("role", "Le rôle est obligatoire (USER ou ADMIN)");
            return null;
        }
        try {
            return Role.valueOf(rawRole.trim());
        } catch (IllegalArgumentException ex) {
            errors.put("role", "Le rôle doit être USER ou ADMIN");
            return null;
        }
    }

    public String validateDescription(String rawDescription, Map<String, String> errors) {
        if (rawDescription == null) {
            return null;
        }
        int codePoints = rawDescription.codePointCount(0, rawDescription.length());
        if (codePoints > 1000) {
            errors.put("description", "La description ne doit pas dépasser 1000 caractères");
            return null;
        }
        return rawDescription;
    }

    public BigDecimal validatePrice(BigDecimal price, Map<String, String> errors) {
        if (price == null) {
            errors.put("price", "Le prix est obligatoire");
            return null;
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            errors.put("price", "Le prix doit être strictement positif");
            return null;
        }
        if (price.scale() > 2) {
            errors.put("price", "Le prix doit comporter au plus deux chiffres après la virgule");
            return null;
        }
        return price;
    }

    public void throwIfErrors(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new BadRequestException("Données invalides", new LinkedHashMap<>(errors));
        }
    }
}
