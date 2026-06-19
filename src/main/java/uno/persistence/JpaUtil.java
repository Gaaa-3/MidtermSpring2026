package uno.persistence;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

public final class JpaUtil {

    private static final Map<String, EntityManagerFactory> FACTORIES = new HashMap<>();

    private JpaUtil() {
    }

    public static synchronized EntityManagerFactory getFactory(String persistenceUnitName) {
        return FACTORIES.computeIfAbsent(persistenceUnitName, Persistence::createEntityManagerFactory);
    }

    public static synchronized EntityManagerFactory getFactory(String persistenceUnitName, Map<String, String> overrides) {
        return Persistence.createEntityManagerFactory(persistenceUnitName, overrides);
    }

    public static synchronized void closeAll() {
        for (EntityManagerFactory factory : FACTORIES.values()) {
            if (factory.isOpen()) {
                factory.close();
            }
        }
        FACTORIES.clear();
    }
}
