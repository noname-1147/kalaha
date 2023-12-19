package demo.model.aggregates;

import com.fasterxml.jackson.annotation.JsonIgnore;
import demo.model.enums.ActionType;
import demo.model.enums.GameState;
import io.micronaut.serde.annotation.Serdeable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import static demo.model.enums.GameState.CREATED;
import static demo.model.enums.GameState.FINISHED;
import static demo.model.enums.GameState.STARTED;

@Serdeable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public final class Game {
    private static final int PITS_COUNT = 6;
    private static final int STONES_COUNT = 6;
    private static final int PLAYER_COUNT = 2;
    private final static int[] STARTING_BOARD;

    static {
        int totalPits = (PITS_COUNT + 1) * PLAYER_COUNT;
        STARTING_BOARD = new int[totalPits];
        for (int i = 0; i < STARTING_BOARD.length; i++) {
            if (i % (PITS_COUNT + 1) != 0) {
                STARTING_BOARD[STARTING_BOARD.length - i - 1] = STONES_COUNT;
            }
        }
    }

    @Getter
    private UUID id;
    @Getter
    private GameState state;
    @Getter
    private Integer version;
    @Getter
    private int turnCount;
    @Getter
    private int[] board;
    @Getter
    private List<Player> players;
    private Integer currentPlayerIndex;
    private Integer winnerIndex;


    public Game(final UUID id) {
        this.id = id;
        this.state = CREATED;
        this.version = 0;
        this.turnCount = 0;
        this.board = getStartingBoard();
        this.players = new ArrayList<>(PLAYER_COUNT);
        this.currentPlayerIndex = null;
        this.winnerIndex = null;
    }

    public Integer getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public Player getCurrentPlayer() {
        if (currentPlayerIndex == null) {
            return null;
        }
        return players.get(currentPlayerIndex);
    }

    public Player getWinner() {
        if (winnerIndex == null) {
            return null;
        }
        return players.get(winnerIndex);
    }

    public void validateAction(ActionType actionType) {
        if (this.state == null) {
            this.state = CREATED;
        }
        if (!state.getPermittedActions().contains(actionType)) {
            throw new IllegalArgumentException(String.format("State cannot be applied to this Game, correct actions are %s", state.getPermittedActions()));
        }
    }

    public Game copy() {
        return Game.builder()
                .id(id)
                .state(state)
                .turnCount(turnCount)
                .board(board)
                .players(copyPlayers(board))
                .currentPlayerIndex(currentPlayerIndex)
                .version(version + 1)
                .winnerIndex(winnerIndex)
                .build();
    }

    private List<Player> copyPlayers(int[] board) {
        return players.stream().map(pl -> pl.copy(board)).collect(Collectors.toList());
    }

    public Game join(String playerName) {
        this.validateAction(ActionType.JOIN);
        if (players == null) {
            players = new ArrayList<>(PLAYER_COUNT);
        } else if (players.size() == PLAYER_COUNT) {
            throw new IllegalArgumentException(String.format("Player count exceeded, %d is the maximum number", PLAYER_COUNT));
        } else if (!players.isEmpty() && players.getFirst().getName().equals(playerName)) {
            throw new IllegalArgumentException("Player name must be unique");
        }
        int playerIndex = players.size();
        int rangeStart = (PITS_COUNT + 1) * playerIndex;
        int rangeEnd = rangeStart + PITS_COUNT;
        Player player = new Player(playerName, 0, rangeStart, rangeEnd);
        players.add(playerIndex, player);
        return copy();
    }

    public Game start(final Integer playerIndex) {
        this.validateAction(ActionType.START);
        if (players.size() != PLAYER_COUNT) {
            throw new IllegalArgumentException(String.format("Not enough players, %d needed", PLAYER_COUNT));
        }
        currentPlayerIndex = (playerIndex == null) ? getRandomPlayer() : playerIndex;
        this.state = STARTED;
        return copy();
    }

    public Game forfeit() {
        this.validateAction(ActionType.FORFEIT);
        this.winnerIndex = nextPlayer(currentPlayerIndex);
        this.state = FINISHED;
        return copy();
    }

    public Game move(int pitIndex) {
        this.validateAction(ActionType.MOVE);
        final Player player = players.get(currentPlayerIndex);
        if (pitIndex < player.getRangeStart() || pitIndex >= player.getRangeEnd()) {
            throw new IllegalArgumentException(String.format("Wrong pit index, available indexes for the move are %d - %d", player.getRangeStart(), player.getRangeEnd() - 1));
        }
        if (board[pitIndex] == 0) {
            throw new IllegalArgumentException("Wrong pit index, pit is already empty");
        }
        final Player opponent = players.get(nextPlayer(currentPlayerIndex));
        Integer winner = null;
        Integer nextPlayer = currentPlayerIndex;
        GameState resultState = state;
        int[] resultBoard = Arrays.copyOf(board, board.length);
        int lastIndex = moveStones(resultBoard, pitIndex, opponent.getRangeEnd());
        capture(resultBoard, lastIndex, player.getRangeStart(), player.getRangeEnd());
        boolean gameOver = isGameOver(resultBoard, player, opponent);
        if (gameOver) {
            resultState = FINISHED;
            winner = getWinnerIndex(resultBoard, currentPlayerIndex, player.getRangeEnd(), opponent.getRangeEnd());
        } else if (lastIndex != player.getRangeEnd()) {
            nextPlayer = nextPlayer(currentPlayerIndex);
        }
        Game result = Game.builder()
                .id(id)
                .state(resultState)
                .version(version + 1)
                .turnCount(turnCount + 1)
                .board(resultBoard)
                .players(copyPlayers(resultBoard))
                .currentPlayerIndex(nextPlayer)
                .winnerIndex(winner)
                .build();
        return result;
    }
    private static int[] getStartingBoard() {
        return Arrays.copyOf(STARTING_BOARD, STARTING_BOARD.length);
    }

    static int moveStones(final int[] board, int pitIndex, int opponentPit) {
        int stones = board[pitIndex];
        board[pitIndex] = 0;
        int lastIndex = pitIndex + 1;
        for (; stones > 0; lastIndex++) {
            if (lastIndex == board.length) {
                lastIndex = 0;
            }
            if (lastIndex != opponentPit) {
                board[lastIndex]++;
                stones--;
            }
        }
        return --lastIndex;
    }

    static void capture(int[] board, int lastIndex, int rangeStart, int rangeEnd) {
        if (lastIndex >= rangeStart && lastIndex < rangeEnd && board[lastIndex] == 1) {
            int oppositeIndex = (PLAYER_COUNT * PITS_COUNT) - lastIndex;
            board[rangeEnd] += board[oppositeIndex] + 1;
            board[lastIndex] = 0;
            board[oppositeIndex] = 0;
        }
    }

    static boolean isGameOver(int[] board, final Player player, final Player opponent) {
        int remainingStonesPlayer = 0, remainingStonesOpponent = 0;
        for (int i = 0; i < PITS_COUNT; i++) {
            remainingStonesPlayer += board[i + player.getRangeStart()];
            remainingStonesOpponent += board[i + opponent.getRangeStart()];
        }
        if (remainingStonesPlayer == 0 || remainingStonesOpponent == 0) {
            board[player.getRangeEnd()] += remainingStonesPlayer;
            board[opponent.getRangeEnd()] += remainingStonesOpponent;
            return true;
        }
        return false;
    }

    static Integer getWinnerIndex(int[] board, int currentPlayer, int playerPit, int opponentPit) {
        if (board[playerPit] == board[opponentPit]) {
            return null;
        } else if (board[playerPit] > board[opponentPit]) {
            return currentPlayer;
        } else {
            return nextPlayer(currentPlayer);
        }
    }


    private static int nextPlayer(final int currentPlayer) {
        return (currentPlayer + 1 == PLAYER_COUNT) ? 0 : currentPlayer + 1;
    }

    private static int getRandomPlayer() {
        return new Random().nextInt(PLAYER_COUNT);
    }
}
