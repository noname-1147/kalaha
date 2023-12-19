package demo.model.responses;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record ErrorResponse(String errorMessage) {
}
