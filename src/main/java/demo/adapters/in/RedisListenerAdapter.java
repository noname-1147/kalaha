package demo.adapters.in;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.model.persistance.GameStateChangeEvent;
import demo.ports.out.BroadcastPort;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.ChannelMessage;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import jakarta.inject.Singleton;

@Singleton
public class RedisListenerAdapter {

    private final BroadcastPort broadcastPort;
    private final RedisPubSubReactiveCommands<String, String> reactiveCommands;
    private final ObjectMapper objectMapper;

    public RedisListenerAdapter(StatefulRedisPubSubConnection<String, String> redisPubSubConnection, BroadcastPort broadcastPort, ObjectMapper objectMapper) {
        this.broadcastPort = broadcastPort;
        this.reactiveCommands = redisPubSubConnection.reactive();
        this.objectMapper = objectMapper;
        reactiveCommands.subscribe("events").subscribe();
        reactiveCommands.observeChannels()
                .map(ChannelMessage::getMessage)
                .<GameStateChangeEvent>handle((str, sink) -> {
                    try {
                        sink.next(objectMapper.readValue(str, GameStateChangeEvent.class));
                    } catch (JsonProcessingException e) {
                        sink.error(e);
                    }
                })
                .flatMap(message -> broadcastPort.broadcast(message.gameId()))
                .subscribe();
    }
}
