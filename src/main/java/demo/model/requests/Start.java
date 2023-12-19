package demo.model.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import demo.model.enums.ActionType;
import io.micronaut.serde.annotation.Serdeable;

import java.util.Optional;

@Serdeable
public record Start(Integer playerIndex) implements Action {
    @Override
    @JsonProperty("actionType")
    public ActionType actionType() {
        return ActionType.START;
    }
}
