package demo.usecases;

import com.fasterxml.jackson.databind.ObjectMapper;
import demo.model.aggregates.Game;
import demo.model.enums.ActionType;
import demo.model.persistance.GameStateChangeEvent;
import demo.model.requests.Action;
import demo.model.requests.Start;
import demo.ports.in.ActionPort;
import demo.ports.out.EventPropagationPort;
import demo.ports.out.PersistencePort;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Map;
import java.util.UUID;

@Singleton
public class ActionUseCase implements ActionPort {
    private final PersistencePort persistencePort;
    private final EventPropagationPort eventPropagationPort;
    private final ObjectMapper objectMapper;

    public ActionUseCase(PersistencePort persistencePort, EventPropagationPort eventPropagationPort, ObjectMapper objectMapper) {
        this.persistencePort = persistencePort;
        this.eventPropagationPort = eventPropagationPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Game> applyAction(final UUID gameId, final Action action) {
        return persistencePort.retrieveState(gameId)
                .map(action::apply)
                .map(game -> {
                    final Action serde;
                    if (action.actionType() == ActionType.START) {
                        serde = new Start(game.getCurrentPlayerIndex());
                    } else {
                        serde = action;
                    }
                    return Tuples.of(game, new GameStateChangeEvent(game.getId(), game.getVersion(), null, objectMapper.convertValue(serde, Map.class)));
                })
                .flatMap(tuple2 -> persistencePort.saveState(tuple2.getT1(), tuple2.getT2()))
                .flatMap(tuple2 -> eventPropagationPort.sendEvent(tuple2.getT2()).map(event -> tuple2.getT1()));
    }
}
