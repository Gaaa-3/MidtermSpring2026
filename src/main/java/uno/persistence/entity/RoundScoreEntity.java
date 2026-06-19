package uno.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "round_scores")
public class RoundScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "round_id", nullable = false)
    private RoundEntity round;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    @Column(name = "points_this_round", nullable = false)
    private int pointsThisRound;

    @Column(name = "cumulative_score", nullable = false)
    private int cumulativeScore;

    protected RoundScoreEntity() {
    }

    public RoundScoreEntity(RoundEntity round, PlayerEntity player, int pointsThisRound, int cumulativeScore) {
        this.round = round;
        this.player = player;
        this.pointsThisRound = pointsThisRound;
        this.cumulativeScore = cumulativeScore;
    }

    public Long getId() { return id; }
    public RoundEntity getRound() { return round; }
    public PlayerEntity getPlayer() { return player; }
    public int getPointsThisRound() { return pointsThisRound; }
    public int getCumulativeScore() { return cumulativeScore; }
}
