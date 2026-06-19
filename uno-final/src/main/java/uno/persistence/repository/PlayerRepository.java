package uno.persistence.repository;

import jakarta.persistence.EntityManager;
import uno.persistence.entity.PlayerEntity;

import java.util.Optional;

public class PlayerRepository {

    public PlayerEntity findOrCreate(EntityManager em, String name) {
        Optional<PlayerEntity> existing = findByName(em, name);
        if (existing.isPresent()) {
            return existing.get();
        }
        PlayerEntity player = new PlayerEntity(name);
        em.persist(player);
        return player;
    }

    public Optional<PlayerEntity> findByName(EntityManager em, String name) {
        return em.createQuery(
                        "SELECT p FROM PlayerEntity p WHERE p.name = :name", PlayerEntity.class)
                .setParameter("name", name)
                .getResultStream()
                .findFirst();
    }
}
