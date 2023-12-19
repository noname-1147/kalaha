package demo.model.requests;

import demo.model.aggregates.Game;
import demo.model.enums.ActionType;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public interface Action {
    public ActionType actionType();

    default Game apply(Game game) {
        return actionType().apply(game, this);
    }
}
