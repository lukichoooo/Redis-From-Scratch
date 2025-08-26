package hashtable;

public class HashTable<T> {

    private static class Node<T> {
        final int key;
        T value;
        Node<T> next;

        Node(int key, T value) {
            this.key = key;
            this.value = value;
        }
    }

    private Node<T>[] table;
    private int size;
    private int capacity;
    private final float loadFactor;
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    public HashTable() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    public HashTable(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public HashTable(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        }
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        }

        int cap = 1;
        while (cap < initialCapacity) {
            cap <<= 1;
        }
        this.capacity = (cap > MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : cap;

        this.loadFactor = loadFactor;
        this.table = new Node[this.capacity];
    }

    private int hash(int key) {
        int h = key;
        h ^= (h >>> 16);
        return h & (capacity - 1);
    }

    public void put(int key, T value) {
        if (size >= capacity * loadFactor) {
            resize();
        }
        int index = hash(key);
        for (Node<T> node = table[index]; node != null; node = node.next) {
            if (node.key == key) {
                node.value = value;
                return;
            }
        }
        Node<T> newNode = new Node<>(key, value);
        newNode.next = table[index];
        table[index] = newNode;
        size++;
    }

    public T get(int key) {
        int index = hash(key);
        for (Node<T> node = table[index]; node != null; node = node.next) {
            if (node.key == key) {
                return node.value;
            }
        }
        return null;
    }

    public boolean containsKey(int key) {
        return get(key) != null;
    }

    public T remove(int key) {
        int index = hash(key);
        Node<T> node = table[index];
        Node<T> prev = null;

        while (node != null) {
            if (node.key == key) {
                if (prev == null) {
                    table[index] = node.next;
                } else {
                    prev.next = node.next;
                }
                size--;
                return node.value;
            }
            prev = node;
            node = node.next;
        }
        return null;
    }

    private void resize() {
        int oldCapacity = capacity;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            return;
        }

        capacity = oldCapacity << 1;
        Node<T>[] oldTable = table;
        table = new Node[capacity];
        size = 0;

        for (Node<T> head : oldTable) {
            Node<T> node = head;
            while (node != null) {
                put(node.key, node.value);
                node = node.next;
            }
        }
    }

    public int size() {
        return size;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Node<T> head : table) {
            Node<T> node = head;
            while (node != null) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(node.key).append("=").append(node.value);
                first = false;
                node = node.next;
            }
        }
        sb.append("}");
        return sb.toString();
    }
}