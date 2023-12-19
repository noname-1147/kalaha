package demo.usecases;

import demo.model.aggregates.Game;
import demo.ports.out.PersistencePort;
import demo.ports.in.SubscribePort;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

import java.util.UUID;


@Singleton
public class SubscribeUseCase implements SubscribePort {
    private final PersistencePort persistencePort;

    public SubscribeUseCase(PersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    @Override
    public Mono<Game> subscribe(final UUID id) {
        return persistencePort.retrieveState(id);
    }
}
