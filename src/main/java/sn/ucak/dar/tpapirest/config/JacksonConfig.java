package sn.ucak.dar.tpapirest.config; // Déclare le package de configuration de l'application

import com.fasterxml.jackson.databind.DeserializationFeature; // Importe les options de désérialisation Jackson
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer; // Importe l'interface permettant de personnaliser ObjectMapper
import org.springframework.context.annotation.Bean; // Importe l'annotation @Bean de Spring
import org.springframework.context.annotation.Configuration; // Importe l'annotation @Configuration de Spring

@Configuration // Indique à Spring que cette classe définit des beans de configuration
public class JacksonConfig { // Classe de configuration Jackson pour l'API REST

    @Bean // Enregistre le personnalisateur dans le contexte Spring Boot
    public Jackson2ObjectMapperBuilderCustomizer bigDecimalForFloatsCustomizer() { // Personnalise le constructeur ObjectMapper utilisé par Spring
        return builder -> builder.featuresToEnable( // Active une option sur Jackson2ObjectMapperBuilder
                DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS // Lit les nombres décimaux JSON en BigDecimal au lieu de Double
        ); // Ferme l'appel featuresToEnable
    } // Ferme la méthode bigDecimalForFloatsCustomizer
} // Ferme la classe JacksonConfig