import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.khundadze.data_structures.SkipList;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SkipListTests {

    private SkipList<Integer, String> sl;

    @BeforeEach
    void setUp() {
        sl = new SkipList<>();
    }

    @Test
    void testInsertGetReplaceAndSize() {
        sl.insert(10, "ten");
        assertEquals(1, sl.size(), "Size after one insert");
        assertNotNull(sl.get(10), "Get should find inserted key");
        assertTrue(sl.containsKey(10), "ContainsKey should be true after insert");
        assertTrue(sl.toString().contains("10:ten"), "toString should contain 10:ten");

        // Replace value for existing key
        sl.insert(10, "TEN");
        assertEquals(1, sl.size(), "Size should remain 1 after replace");
        assertEquals("TEN", sl.get(10).value, "Value should be updated to TEN");
        assertTrue(sl.toString().contains("10:TEN"), "toString should reflect replaced value");
    }

    @Test
    void testRemoveAndSize() {
        sl.insert(1, "one");
        sl.insert(2, "two");
        assertEquals(2, sl.size());

        var removed = sl.remove(1);
        assertNotNull(removed, "Remove should return non-null for existing key");
        assertNull(sl.get(1), "Get should return null after removal");
        assertEquals(1, sl.size(), "Size should decrement after removal");
        assertFalse(sl.toString().contains("1:one"), "toString should not contain removed entry");
    }

    @Test
    void testRemoveNonexistentAndIsEmpty() {
        assertNull(sl.remove(42), "Removing non-existent key returns null");
        assertTrue(sl.isEmpty(), "Empty after no inserts");

        sl.insert(5, "five");
        assertFalse(sl.isEmpty(), "Not empty after insert");
        assertNull(sl.get(999), "Getting non-existent key returns null");
    }

    @Test
    void testMultipleInsertsOrder() {
        for (int i = 0; i < 20; i++) {
            sl.insert(i, "v" + i);
        }
        assertEquals(20, sl.size());
        String s = sl.toString();
        for (int i = 0; i < 20; i++) {
            assertTrue(s.contains(i + ":v" + i), "toString should contain " + i + ":v" + i);
        }
    }

    @Test
    void testRandomizedOperationsConsistency() {
        SkipList<Integer, Integer> s2 = new SkipList<>();
        Random rnd = new Random(123);
        Set<Integer> reference = new HashSet<>();

        final int OPS = 1000;
        for (int op = 0; op < OPS; op++) {
            int key = rnd.nextInt(200);
            if (rnd.nextBoolean()) {
                // Insert
                s2.insert(key, key);
                reference.add(key);
            } else {
                // Remove
                var removed = s2.remove(key);
                if (reference.contains(key)) {
                    assertNotNull(removed, "Remove should succeed when key present: " + key);
                    reference.remove(key);
                } else {
                    assertNull(removed, "Remove should return null when key absent: " + key);
                }
            }

            // Check size and presence parity
            assertEquals(reference.size(), s2.size(), "Size should match reference set");
            for (int checkKey : new int[] { 0, 1, 50, 100, 150, 199 }) {
                boolean presentInRef = reference.contains(checkKey);
                boolean presentInSkip = s2.containsKey(checkKey);
                assertEquals(presentInRef, presentInSkip, "Presence parity for key " + checkKey);
            }
        }
    }
}
