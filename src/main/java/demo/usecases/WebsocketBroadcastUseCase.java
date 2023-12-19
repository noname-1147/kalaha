package demo.usecases;

import demo.model.requests.Action;
import demo.ports.out.BroadcastPort;
import demo.ports.out.PersistencePort;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Predicate;

@Singleton
public class WebsocketBroadcastUseCase implements BroadcastPort {

    private final PersistencePort persistencePort;
    private final ObjectMapper objectMapper;
    private final WebSocketBroadcaster broadcaster;

    public WebsocketBroadcastUseCase(PersistencePort persistencePort, ObjectMapper objectMapper, WebSocketBroadcaster broadcaster) {
        this.persistencePort = persistencePort;
        this.objectMapper = objectMapper;
        this.broadcaster = broadcaster;
    }

    @Override
    public Publisher<String> broadcast(final UUID gameId) {
        return persistencePort.retrieveState(gameId)
                .<String>handle((game, sink) -> {
                    try {
                        sink.next(objectMapper.writeValueAsString(game));
                    } catch (IOException e) {
                        sink.error(e);
                    }
                })
                .flatMapMany(message -> broadcaster.broadcast(message, sessionPredicate(gameId)));
    }

    private static Predicate<WebSocketSession> sessionPredicate(UUID gameId) {
        return s -> gameId.equals(s.getUriVariables().get("gameId", UUID.class, null));
    }
}
