
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.khundadze.data_structures.SkipList;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SkipListTests {

    private SkipList<String> sl;

    @BeforeEach
    void setUp() {
        sl = new SkipList<>();
    }

    @Test
    void testInsertGetReplaceAndSize() {
        sl.insert(10, "ten");
        assertEquals(1, sl.size(), "size after one insert");
        assertNotNull(sl.get(10), "get should find inserted key (node present)");
        assertTrue(sl.containsKey(10), "containsKey should be true after insert");
        assertTrue(sl.toString().contains("10:ten"), "toString should contain 10:ten");

        // replace value for existing key
        sl.insert(10, "TEN");
        assertEquals(1, sl.size(), "size should remain 1 after replace");
        assertNotNull(sl.get(10));
        assertTrue(sl.toString().contains("10:TEN"), "toString should reflect replaced value");
    }

    @Test
    void testRemoveAndSize() {
        sl.insert(1, "one");
        sl.insert(2, "two");
        assertEquals(2, sl.size());

        Object removed = sl.remove(1);
        assertNotNull(removed, "remove should return non-null for existing key");
        assertNull(sl.get(1), "get should return null after removal");
        assertEquals(1, sl.size(), "size should decrement after removal");
        assertFalse(sl.toString().contains("1:one"), "toString should not contain removed entry");
    }

    @Test
    void testRemoveNonexistentAndIsEmpty() {
        assertNull(sl.remove(42), "removing non-existent key returns null");
        assertTrue(sl.isEmpty(), "empty after no inserts");

        sl.insert(5, "five");
        assertFalse(sl.isEmpty());
        assertNull(sl.get(999), "getting non-existent key returns null");
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
        SkipList<Integer> s2 = new SkipList<>();
        Random rnd = new Random(123);
        Set<Integer> reference = new HashSet<>();

        final int OPS = 1000;
        for (int op = 0; op < OPS; op++) {
            int key = rnd.nextInt(200);
            if (rnd.nextBoolean()) {
                // insert
                s2.insert(key, key);
                reference.add(key);
            } else {
                // remove
                Object removed = s2.remove(key);
                if (reference.contains(key)) {
                    assertNotNull(removed, "remove should succeed when key present: " + key);
                    reference.remove(key);
                } else {
                    assertNull(removed, "remove should return null when key absent: " + key);
                }
            }
            // check size and presence parity with reference set
            assertEquals(reference.size(), s2.size(), "size should match reference set");
            for (int checkKey : new int[] { 0, 1, 50, 100, 150, 199 }) {
                boolean presentInRef = reference.contains(checkKey);
                boolean presentInSkip = s2.containsKey(checkKey);
                assertEquals(presentInRef, presentInSkip,
                        "presence parity for key " + checkKey);
            }
        }
    }
}
