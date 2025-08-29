
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.khundadze.data_structures.ZSet;

import static org.junit.jupiter.api.Assertions.*;

class ZSetTests {

    private ZSet<String> zset;

    @BeforeEach
    void setUp() {
        zset = new ZSet<>();
    }

    @Test
    void testAddAndLookup() {
        zset.add("Alice", 10, "ValueA");
        zset.add("Bob", 20, "ValueB");
        zset.add("Charlie", 15, "ValueC");

        assertNotNull(zset.lookup("Alice"));
        assertEquals(20, zset.lookup("Bob").score);
        assertEquals("Charlie", zset.lookup("Charlie").name);
        assertNull(zset.lookup("NonExistent"));
    }

    @Test
    void testUpdateScore() {
        zset.add("Alice", 10, "ValueA");
        double oldScore = zset.lookup("Alice").score;

        // update Alice's score
        zset.add("Alice", 30, "ValueA_updated");
        assertEquals(30, zset.lookup("Alice").score);
        assertNotEquals(oldScore, zset.lookup("Alice").score);
    }

    @Test
    void testRemove() {
        zset.add("Alice", 10, "ValueA");
        zset.add("Bob", 20, "ValueB");

        assertTrue(zset.remove("Alice"));
        assertNull(zset.lookup("Alice"));

        // Removing non-existent element should return false
        assertFalse(zset.remove("NonExistent"));
    }

    @Test
    void testQueryForward() {
        zset.add("Alice", 10, "ValueA");
        zset.add("Bob", 20, "ValueB");
        zset.add("Charlie", 15, "ValueC");

        // Start at Charlie and move forward by 1 → Bob
        ZSet.ZNode<String> node = zset.query(15, "Charlie", 1);
        assertNotNull(node);
        assertEquals("Bob", node.name);
    }

    @Test
    void testQueryBackwardUnsupported() {
        zset.add("Alice", 10, "ValueA");
        zset.add("Bob", 20, "ValueB");
        zset.add("Charlie", 15, "ValueC");

        // Backward queries should throw
        assertThrows(UnsupportedOperationException.class,
                () -> zset.query(15, "Charlie", -1));
    }

    @Test
    void testQueryEdgeCases() {
        zset.add("Alice", 10, "ValueA");

        // query beyond the end → null
        assertNull(zset.query(10, "Alice", 5));

        // query before the start → exception
        assertThrows(UnsupportedOperationException.class,
                () -> zset.query(10, "Alice", -1));
    }

}
