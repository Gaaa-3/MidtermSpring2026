package uno.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;
import java.util.UUID;

public abstract class PersistenceTestSupport {

    protected EntityManagerFactory emf;
    protected EntityManager em;

    @BeforeEach
    void openIsolatedDatabase() {
        String uniqueUrl = "jdbc:h2:mem:test_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        Map<String, String> overrides = Map.of("jakarta.persistence.jdbc.url", uniqueUrl);
        emf = JpaUtil.getFactory("uno-test-pu", overrides);
        em = emf.createEntityManager();
    }

    @AfterEach
    void closeDatabase() {
        if (em != null && em.isOpen()) em.close();
        if (emf != null && emf.isOpen()) emf.close();
    }
}
