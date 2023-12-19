package demo.model.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import demo.model.enums.ActionType;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record Move(Integer pitIndex) implements Action {

    @Override
    @JsonProperty("actionType")
    public ActionType actionType() {
        return ActionType.MOVE;
    }
}
