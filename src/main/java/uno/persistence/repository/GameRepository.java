package uno.persistence.repository;

import jakarta.persistence.EntityManager;
import uno.persistence.entity.GameEntity;
import uno.persistence.entity.PlayerEntity;
import uno.persistence.entity.RoundEntity;
import uno.persistence.entity.RoundScoreEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class GameRepository {

    private final PlayerRepository playerRepository = new PlayerRepository();

    public GameEntity startGame(EntityManager em, List<String> playerNames) {
        GameEntity game = new GameEntity(Instant.now());
        for (String name : playerNames) {
            game.getPlayers().add(playerRepository.findOrCreate(em, name));
        }
        em.persist(game);
        return game;
    }

    public RoundEntity recordRound(EntityManager em,
                                    GameEntity game,
                                    int roundNumber,
                                    String winnerName,
                                    Map<String, Integer> pointsThisRoundByPlayer,
                                    Map<String, Integer> cumulativeScoreByPlayer,
                                    Instant endedAt) {
        RoundEntity round = new RoundEntity(game, roundNumber, endedAt);
        if (winnerName != null) {
            round.setWinner(playerRepository.findOrCreate(em, winnerName));
        }
        for (Map.Entry<String, Integer> entry : cumulativeScoreByPlayer.entrySet()) {
            String playerName = entry.getKey();
            int cumulative = entry.getValue();
            int pointsThisRound = pointsThisRoundByPlayer.getOrDefault(playerName, 0);
            PlayerEntity player = playerRepository.findOrCreate(em, playerName);
            round.getScores().add(new RoundScoreEntity(round, player, pointsThisRound, cumulative));
        }
        game.getRounds().add(round);
        em.persist(round);
        return round;
    }

    public void finishGame(EntityManager em, GameEntity game, String winnerName, Instant endedAt) {
        game.setEndedAt(endedAt);
        if (winnerName != null) {
            game.setWinner(playerRepository.findOrCreate(em, winnerName));
        }
        em.merge(game);
    }

    public List<Object[]> findRecentGames(EntityManager em, int limit) {
        return em.createQuery(
                        "SELECT g.id, g.startedAt, g.endedAt, w.name " +
                                "FROM GameEntity g LEFT JOIN g.winner w " +
                                "ORDER BY g.startedAt DESC",
                        Object[].class)
                .setMaxResults(limit)
                .getResultList();
    }

    public List<Object[]> findPlayerWinCounts(EntityManager em) {
        return em.createQuery(
                        "SELECT g.winner.name, COUNT(g) " +
                                "FROM GameEntity g " +
                                "WHERE g.winner IS NOT NULL " +
                                "GROUP BY g.winner.name " +
                                "ORDER BY COUNT(g) DESC",
                        Object[].class)
                .getResultList();
    }

    public List<Object[]> findHighestScores(EntityManager em, int limit) {
        return em.createQuery(
                        "SELECT rs.player.name, rs.cumulativeScore, rs.round.game.id " +
                                "FROM RoundScoreEntity rs " +
                                "ORDER BY rs.cumulativeScore DESC",
                        Object[].class)
                .setMaxResults(limit)
                .getResultList();
    }

    public long countRoundWinsForPlayer(EntityManager em, String playerName) {
        return em.createQuery(
                        "SELECT COUNT(r) FROM RoundEntity r WHERE r.winner.name = :name",
                        Long.class)
                .setParameter("name", playerName)
                .getSingleResult();
    }
}
