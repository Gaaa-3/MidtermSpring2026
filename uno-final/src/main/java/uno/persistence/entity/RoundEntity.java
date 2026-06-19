package uno.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rounds")
public class RoundEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    private GameEntity game;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private PlayerEntity winner;

    @Column(nullable = false)
    private Instant endedAt;

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoundScoreEntity> scores = new ArrayList<>();

    protected RoundEntity() {
    }

    public RoundEntity(GameEntity game, int roundNumber, Instant endedAt) {
        this.game = game;
        this.roundNumber = roundNumber;
        this.endedAt = endedAt;
    }

    public Long getId() { return id; }
    public GameEntity getGame() { return game; }
    public int getRoundNumber() { return roundNumber; }
    public PlayerEntity getWinner() { return winner; }
    public void setWinner(PlayerEntity winner) { this.winner = winner; }
    public Instant getEndedAt() { return endedAt; }
    public List<RoundScoreEntity> getScores() { return scores; }
}
