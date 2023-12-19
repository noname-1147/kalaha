package demo.model.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import demo.model.enums.ActionType;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record Join(String userName) implements Action {

    @Override
    @JsonProperty("actionType")
    public ActionType actionType() {
        return ActionType.JOIN;
    }
}
