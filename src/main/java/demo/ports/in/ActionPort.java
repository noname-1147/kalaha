package demo.ports.in;

import demo.model.aggregates.Game;
import demo.model.persistance.GameStateChangeEvent;
import demo.model.requests.Action;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ActionPort {
    Mono<Game> applyAction(UUID gameId, Action action);

}
