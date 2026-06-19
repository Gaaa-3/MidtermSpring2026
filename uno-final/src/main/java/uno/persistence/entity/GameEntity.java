package uno.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "games")
public class GameEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant endedAt;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private PlayerEntity winner;

    @ManyToMany
    @JoinTable(
            name = "game_players",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "player_id"))
    private List<PlayerEntity> players = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoundEntity> rounds = new ArrayList<>();

    protected GameEntity() {
    }

    public GameEntity(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Long getId() { return id; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public PlayerEntity getWinner() { return winner; }
    public void setWinner(PlayerEntity winner) { this.winner = winner; }
    public List<PlayerEntity> getPlayers() { return players; }
    public List<RoundEntity> getRounds() { return rounds; }
}
