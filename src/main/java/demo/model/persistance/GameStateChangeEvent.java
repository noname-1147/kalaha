package demo.model.persistance;

import demo.model.requests.Action;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;
import io.micronaut.serde.annotation.Serdeable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Serdeable

@MappedEntity("events")
public record GameStateChangeEvent(
        @Id UUID gameId,
        Integer version,
        @DateCreated LocalDateTime dateCreated,
        @TypeDef(type = DataType.JSON) Map<String, Object> action) {

}
