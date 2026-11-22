package sn.ucak.dar.tpapirest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import sn.ucak.dar.tpapirest.entities.Product;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductResponse {

    @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private final UUID id;

    @Schema(example = "Clavier mécanique")
    private final String name;

    @Schema(example = "Clavier rétroéclairé AZERTY")
    private final String description;

    @Schema(example = "45.99")
    private final BigDecimal price;

    public ProductResponse(UUID id, String name, String description, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
    }

    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice()
        );
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
