package demo.ports.in;

import demo.model.aggregates.Game;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SubscribePort {
    public Mono<Game> subscribe(UUID id);
}
