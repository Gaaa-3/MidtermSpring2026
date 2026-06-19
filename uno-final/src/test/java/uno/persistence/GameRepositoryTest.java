package uno.persistence;

import org.junit.jupiter.api.Test;
import uno.persistence.entity.GameEntity;
import uno.persistence.entity.RoundEntity;
import uno.persistence.repository.GameRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GameRepositoryTest extends PersistenceTestSupport {

    private final GameRepository repository = new GameRepository();

    private GameEntity startGameWith(String... playerNames) {
        em.getTransaction().begin();
        GameEntity game = repository.startGame(em, List.of(playerNames));
        em.getTransaction().commit();
        return game;
    }

    @Test void startGame_linksAllParticipatingPlayers() {
        GameEntity game = startGameWith("Alice", "Bob", "Cara");
        assertNotNull(game.getId());
        assertEquals(3, game.getPlayers().size());
        assertNotNull(game.getStartedAt());
        assertNull(game.getEndedAt());
        assertNull(game.getWinner());
    }

    @Test void recordRound_persistsRoundAndPerPlayerScores() {
        GameEntity game = startGameWith("Alice", "Bob");
        em.getTransaction().begin();
        GameEntity managed = em.find(GameEntity.class, game.getId());
        RoundEntity round = repository.recordRound(
                em, managed, 1, "Alice",
                Map.of("Alice", 35, "Bob", 0),
                Map.of("Alice", 35, "Bob", 0),
                Instant.now());
        em.getTransaction().commit();
        assertNotNull(round.getId());
        assertEquals("Alice", round.getWinner().getName());
        assertEquals(2, round.getScores().size());
    }

    @Test void recordRound_handlesAbortedRoundWithNullWinner() {
        GameEntity game = startGameWith("Alice", "Bob");
        em.getTransaction().begin();
        GameEntity managed = em.find(GameEntity.class, game.getId());
        RoundEntity round = repository.recordRound(
                em, managed, 1, null,
                Map.of(), Map.of("Alice", 0, "Bob", 0),
                Instant.now());
        em.getTransaction().commit();
        assertNull(round.getWinner());
    }

    @Test void finishGame_setsEndTimeAndOverallWinner() {
        GameEntity game = startGameWith("Alice", "Bob");
        em.getTransaction().begin();
        GameEntity managed = em.find(GameEntity.class, game.getId());
        repository.finishGame(em, managed, "Alice", Instant.now());
        em.getTransaction().commit();
        GameEntity reloaded = em.find(GameEntity.class, game.getId());
        assertNotNull(reloaded.getEndedAt());
        assertEquals("Alice", reloaded.getWinner().getName());
    }

    @Test void findRecentGames_ordersByStartTimeDescending() throws InterruptedException {
        GameEntity older = startGameWith("Alice", "Bob");
        Thread.sleep(5);
        GameEntity newer = startGameWith("Cara", "Dan");
        em.getTransaction().begin();
        repository.finishGame(em, em.find(GameEntity.class, older.getId()), "Alice", Instant.now());
        repository.finishGame(em, em.find(GameEntity.class, newer.getId()), "Cara", Instant.now());
        em.getTransaction().commit();
        List<Object[]> recent = repository.findRecentGames(em, 10);
        assertEquals(2, recent.size());
        assertEquals(newer.getId(), recent.get(0)[0]);
        assertEquals(older.getId(), recent.get(1)[0]);
    }

    @Test void findRecentGames_respectsLimit() {
        startGameWith("Alice", "Bob");
        startGameWith("Cara", "Dan");
        startGameWith("Eve", "Finn");
        assertEquals(2, repository.findRecentGames(em, 2).size());
    }

    @Test void findRecentGames_includesGamesWithoutAWinnerYet() {
        GameEntity inProgress = startGameWith("Alice", "Bob");
        List<Object[]> recent = repository.findRecentGames(em, 10);
        assertEquals(1, recent.size());
        assertEquals(inProgress.getId(), recent.get(0)[0]);
        assertNull(recent.get(0)[3]);
    }

    @Test void findPlayerWinCounts_countsOnlyFinishedGamesWithAWinner() {
        GameEntity g1 = startGameWith("Alice", "Bob");
        GameEntity g2 = startGameWith("Alice", "Bob");
        startGameWith("Alice", "Bob"); // unfinished
        em.getTransaction().begin();
        repository.finishGame(em, em.find(GameEntity.class, g1.getId()), "Alice", Instant.now());
        repository.finishGame(em, em.find(GameEntity.class, g2.getId()), "Alice", Instant.now());
        em.getTransaction().commit();
        List<Object[]> winCounts = repository.findPlayerWinCounts(em);
        assertEquals(1, winCounts.size());
        assertEquals("Alice", winCounts.get(0)[0]);
        assertEquals(2L, winCounts.get(0)[1]);
    }

    @Test void findHighestScores_ordersByScoreDescending() {
        GameEntity game = startGameWith("Alice", "Bob");
        em.getTransaction().begin();
        GameEntity managed = em.find(GameEntity.class, game.getId());
        repository.recordRound(em, managed, 1, "Alice",
                Map.of("Alice", 60, "Bob", 0),
                Map.of("Alice", 60, "Bob", 15),
                Instant.now());
        em.getTransaction().commit();
        List<Object[]> highScores = repository.findHighestScores(em, 10);
        assertEquals(2, highScores.size());
        assertEquals("Alice", highScores.get(0)[0]);
        assertEquals(60, highScores.get(0)[1]);
    }

    @Test void countRoundWinsForPlayer_countsAcrossMultipleRounds() {
        GameEntity game = startGameWith("Alice", "Bob");
        em.getTransaction().begin();
        GameEntity managed = em.find(GameEntity.class, game.getId());
        repository.recordRound(em, managed, 1, "Alice", Map.of("Alice", 20), Map.of("Alice", 20, "Bob", 0), Instant.now());
        repository.recordRound(em, managed, 2, "Alice", Map.of("Alice", 15), Map.of("Alice", 35, "Bob", 0), Instant.now());
        repository.recordRound(em, managed, 3, "Bob", Map.of("Bob", 10), Map.of("Alice", 35, "Bob", 10), Instant.now());
        em.getTransaction().commit();
        assertEquals(2L, repository.countRoundWinsForPlayer(em, "Alice"));
        assertEquals(1L, repository.countRoundWinsForPlayer(em, "Bob"));
    }
}
