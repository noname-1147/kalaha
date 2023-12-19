package demo.model.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import demo.model.enums.ActionType;

public record Create() implements Action {
    @Override
    @JsonProperty("actionType")
    public ActionType actionType() {
        return ActionType.CREATE;
    }
}
