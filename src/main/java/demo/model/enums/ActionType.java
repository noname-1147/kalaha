package demo.model.enums;

import demo.model.aggregates.Game;
import demo.model.requests.Action;
import demo.model.requests.Create;
import demo.model.requests.Forfeit;
import demo.model.requests.Join;
import demo.model.requests.Move;
import demo.model.requests.Start;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Serdeable
public enum ActionType {
    JOIN(Join.class, (game, join) -> game.join(join.userName())),
    START(Start.class, (game, start) -> game.start(start.playerIndex())),
    MOVE(Move.class, (game, move) -> game.move(move.pitIndex())),
    FORFEIT(Forfeit.class, (game, forfeit) -> game.forfeit()),
    CREATE(Create.class, (game, create) -> game);
    public static final Map<String, ActionType> ACTION_TYPE_MAP = Arrays
            .stream(ActionType.values())
            .collect(Collectors.toMap(ActionType::name, Function.identity()));

    @Getter
    private final Class<? extends Action> clazz;
    private final BiFunction<Game, Action, Game> gameAction;

    <T extends Action> ActionType(Class<T> clazz, BiFunction<Game, T, Game> gameAction) {
        this.clazz = clazz;
        this.gameAction = ((game, action) -> gameAction.apply(game, clazz.cast(action)));
    }

    public <T extends Action> Game apply(Game game, Action action) {
        return this.gameAction.apply(game, action);
    }
}
