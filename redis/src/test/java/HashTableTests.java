import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import com.khundadze.data_structures.HashTable;

public class HashTableTests {

    @Test
    public void testPutAndGet() {
        HashTable<Integer, String> map = new HashTable<>();
        map.put(1, "one");
        map.put(2, "two");

        assertEquals("one", map.get(1));
        assertEquals("two", map.get(2));
        assertNull(map.get(3));
    }

    @Test
    public void testUpdateValue() {
        HashTable<Integer, String> map = new HashTable<>();
        map.put(1, "one");
        map.put(1, "uno"); // update value

        assertEquals("uno", map.get(1));
    }

    @Test
    public void testRemove() {
        HashTable<Integer, String> map = new HashTable<>();
        map.put(1, "one");
        map.put(2, "two");

        assertEquals("one", map.remove(1));
        assertNull(map.get(1));
        assertFalse(map.containsKey(1));

        assertEquals("two", map.get(2));
    }

    @Test
    public void testContainsKey() {
        HashTable<Integer, String> map = new HashTable<>();
        map.put(1, "one");

        assertTrue(map.containsKey(1));
        assertFalse(map.containsKey(2));
    }

    @Test
    public void testSize() {
        HashTable<Integer, String> map = new HashTable<>();
        assertEquals(0, map.size());

        map.put(1, "one");
        map.put(2, "two");
        assertEquals(2, map.size());

        map.remove(1);
        assertEquals(1, map.size());
    }

    @Test
    public void testResize() {
        HashTable<Integer, Integer> map = new HashTable<>(2, 0.5f); // small capacity to trigger resize
        map.put(1, 10);
        map.put(2, 20); // should trigger resize

        assertEquals(10, map.get(1));
        assertEquals(20, map.get(2));

        map.put(3, 30);
        assertEquals(30, map.get(3));
        assertEquals(3, map.size());
    }

    @Test
    public void testKeySet() {
        HashTable<Integer, String> map = new HashTable<>();
        map.put(1, "one");
        map.put(2, "two");

        Object[] keys = map.keySet();

        assertEquals(2, keys.length);
        assertTrue(keys[0].equals(1) || keys[1].equals(1));
        assertTrue(keys[0].equals(2) || keys[1].equals(2));
    }
}
