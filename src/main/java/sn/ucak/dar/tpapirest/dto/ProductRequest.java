package sn.ucak.dar.tpapirest.dto; // Déclare le package DTO

import com.fasterxml.jackson.core.JsonParser; // Importe JsonParser pour accéder au texte brut du token JSON
import com.fasterxml.jackson.databind.DeserializationContext; // Contexte de désérialisation Jackson
import com.fasterxml.jackson.databind.annotation.JsonDeserialize; // Lie un deserializer custom au champ
import com.fasterxml.jackson.databind.deser.std.StdDeserializer; // Classe de base des deserializers Jackson
import io.swagger.v3.oas.annotations.media.Schema; // Importe @Schema pour la documentation OpenAPI
import sn.ucak.dar.tpapirest.exceptions.BadRequestException; // Importe l'exception métier 400

import java.io.IOException; // Exception d'entrée/sortie pour le deserializer
import java.math.BigDecimal; // Importe BigDecimal pour la précision décimale exacte
import java.util.Map; // Importe Map pour structurer les erreurs par champ

public class ProductRequest { // DTO représentant une création ou modification de produit

    @Schema(example = "Clavier mécanique") // Exemple OpenAPI pour le nom du produit
    private String name; // Nom du produit

    @Schema(example = "Clavier rétroéclairé AZERTY") // Exemple OpenAPI pour la description
    private String description; // Description facultative du produit

    @Schema(example = "45.99") // Exemple OpenAPI pour le prix
    @JsonDeserialize(using = StrictPriceDeserializer.class) // Désérialisation stricte préservant le scale exact du JSON
    private BigDecimal price; // Prix unitaire exact

    public String getName() { // Retourne le nom du produit
        return name; // Valeur actuelle de name
    }

    public void setName(String name) { // Définit le nom du produit
        this.name = name; // Affecte name
    }

    public String getDescription() { // Retourne la description du produit
        return description; // Valeur actuelle de description
    }

    public void setDescription(String description) { // Définit la description du produit
        this.description = description; // Affecte description
    }

    public BigDecimal getPrice() { // Retourne le prix du produit
        return price; // Valeur actuelle de price
    }

    public void setPrice(BigDecimal price) { // Setter standard utile pour les appels directs en Java/tests
        this.price = price; // Affecte price
    }

    /**
     * Deserializer custom qui lit le token JSON brut via JsonParser.getText(),
     * préservant le scale original (1.230 → scale=3, 10.00 → scale=2).
     * Cela évite la normalisation opérée par JsonNode.asText() ou node.decimalValue().
     */
    public static class StrictPriceDeserializer extends StdDeserializer<BigDecimal> {

        public StrictPriceDeserializer() {
            super(BigDecimal.class);
        }

        @Override
        public BigDecimal deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            // Récupère le token courant
            com.fasterxml.jackson.core.JsonToken token = p.currentToken();

            // null explicite → retourne null (ValidationUtils signalera l'erreur)
            if (token == com.fasterxml.jackson.core.JsonToken.VALUE_NULL) {
                return null;
            }

            // Refuse tout ce qui n'est pas un nombre JSON (string, boolean, objet, tableau)
            if (token != com.fasterxml.jackson.core.JsonToken.VALUE_NUMBER_FLOAT
                    && token != com.fasterxml.jackson.core.JsonToken.VALUE_NUMBER_INT) {
                throw new BadRequestException(
                        "Données invalides",
                        Map.of("price", "Le prix doit être un nombre décimal valide")
                );
            }

            // getText() retourne la représentation EXACTE du token dans le JSON source
            // ex. "1.230" pour 1.230, "10.00" pour 10.00, "1e2" pour 1e2
            String rawText = p.getText();

            // Interdit la notation scientifique
            if (rawText.indexOf('e') >= 0 || rawText.indexOf('E') >= 0) {
                throw new BadRequestException(
                        "Données invalides",
                        Map.of("price", "Le prix ne doit pas utiliser la notation scientifique")
                );
            }

            // Construit le BigDecimal depuis le texte brut pour conserver le scale exact
            BigDecimal value;
            try {
                value = new BigDecimal(rawText);
            } catch (NumberFormatException e) {
                throw new BadRequestException(
                        "Données invalides",
                        Map.of("price", "Le prix doit être un nombre décimal valide")
                );
            }

            // Refuse plus de 2 décimales (1.230 → scale=3 → rejeté)
            if (value.scale() > 2) {
                throw new BadRequestException(
                        "Données invalides",
                        Map.of("price", "Le prix doit comporter au plus deux chiffres après la virgule")
                );
            }

            return value;
        }

        @Override
        public BigDecimal getNullValue(DeserializationContext ctx) {
            return null; // null JSON → null BigDecimal → ValidationUtils gère l'erreur
        }
    }
}