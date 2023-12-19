package demo.model.enums;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static demo.model.enums.ActionType.*;

@Serdeable
public enum GameState {
    CREATED(JOIN, START),
    STARTED(MOVE, FORFEIT),
    FINISHED;
    @Getter
    private final Set<ActionType> permittedActions;

    GameState() {
        this.permittedActions = Collections.emptySet();
    }

    GameState(ActionType actionType, ActionType... rest) {
        this.permittedActions = EnumSet.of(actionType, rest);
    }
}
