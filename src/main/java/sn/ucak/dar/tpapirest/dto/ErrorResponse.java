package sn.ucak.dar.tpapirest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Schema(example = "400")
    private final int status;

    @Schema(example = "Données invalides")
    private final String message;

    @Schema(example = "{\"price\": \"Le prix doit être strictement positif\"}")
    private final Map<String, String> errors;

    @Schema(example = "2026-11-20T12:00:00Z")
    private final Instant timestamp;

    @Schema(example = "/api/products")
    private final String path;

    public ErrorResponse(int status, String message, Map<String, String> errors, String path) {
        this.status = status;
        this.message = message;
        this.errors = (errors == null || errors.isEmpty()) ? null : errors;
        this.timestamp = Instant.now();
        this.path = path;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }
}
