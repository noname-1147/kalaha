package demo.model.aggregates;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Getter;


@Serdeable
@Getter
public final class Player {
    private final String name;
    private int score;
    private final int rangeStart;
    private final int rangeEnd;


    Player(String name, int score, int rangeStart, int rangeEnd) {
        this.name = name;
        this.score = score;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }

    public Player copy(int[] board) {
        return new Player(name, board[rangeEnd], rangeStart, rangeEnd);
    }
}
