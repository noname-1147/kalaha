package demo.adapters.in;

import demo.model.enums.ActionType;
import demo.model.persistance.GameStateChangeEvent;
import demo.model.requests.Action;
import demo.model.requests.Start;
import demo.model.responses.ErrorResponse;
import demo.ports.in.ActionPort;
import demo.ports.in.SubscribePort;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

@ServerWebSocket("/ws/game/{gameId}")
@Slf4j
public class WebsocketAdapter {
    private final ObjectMapper objectMapper;
    private final WebSocketBroadcaster broadcaster;
    private final SubscribePort subscribePort;
    private final ActionPort actionPort;

    public WebsocketAdapter(ObjectMapper objectMapper, WebSocketBroadcaster broadcaster,
                            SubscribePort subscribe, ActionPort actionPort) {
        this.objectMapper = objectMapper;
        this.broadcaster = broadcaster;
        this.subscribePort = subscribe;
        this.actionPort = actionPort;
    }

    @OnOpen
    public Publisher<String> onOpen(@PathVariable("gameId") UUID gameId, WebSocketSession session) {
        log.debug("User joined game {}, session {}", gameId, session);
        return subscribePort.subscribe(gameId)
                .<String>handle((game, sink) -> {
                    try {
                        sink.next(objectMapper.writeValueAsString(game));
                    } catch (IOException e) {
                        sink.error(e);
                    }
                })
                .flatMapMany(game -> broadcaster.broadcast(game, sessionPredicate(gameId)));
    }

    @OnMessage
    public Publisher<?> onMessage(@PathVariable("gameId") UUID gameId, WebSocketSession session, String message) {
        return Mono.just(message).<Action>handle((m, sink) -> {
                    try {
                        Map<String, Object> map = objectMapper.readValue(message, Map.class);
                        final ActionType actionType = ActionType.ACTION_TYPE_MAP.get(map.get("actionType").toString());
                        final Action action = objectMapper.readValue(message, actionType.getClazz());
                        //Sanitizing the input for start actions
                        if (action instanceof Start) {
                            sink.next(new Start(null));
                        } else {
                            sink.next(action);
                        }
                    } catch (IOException e) {
                        sink.error(new RuntimeException(e));
                    }
                })
                .flatMap(action -> actionPort.applyAction(gameId, action))
                //Temporary solution until I figure out how to make redis Pub/Sub work
                .flatMapMany(game -> broadcaster.broadcast(game, sessionPredicate(gameId)))
                .onErrorResume(IllegalArgumentException.class, e -> {
                     broadcaster.broadcastSync(getErrorResponse(e), sessionPredicate(gameId));
                     return Mono.empty();
                });
    }


    private String getErrorResponse(Exception e) {
        try {
            return objectMapper.writeValueAsString(new ErrorResponse(e.getMessage()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Predicate<WebSocketSession> sessionPredicate(UUID gameId) {
        return s -> gameId.equals(s.getUriVariables().get("gameId", UUID.class, null));
    }
}
