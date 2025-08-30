package com.khundadze.data_structures;

public class HashTable<K, V> {

    public static class Node<K, V> {
        final K key;
        V value;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return key + ":" + value;
        }
    }

    private Node<K, V>[] table;
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

    @SuppressWarnings("unchecked")
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

    private int hash(Object key) {
        int h = key.hashCode();
        h ^= (h >>> 16);
        return h & (capacity - 1);
    }

    public Node<K, V> put(K key, V value) {
        if (size >= capacity * loadFactor) {
            resize();
        }
        int index = hash(key);
        for (Node<K, V> node = table[index]; node != null; node = node.next) {
            if (node.key.equals(key)) {
                node.value = value;
                return node;
            }
        }
        Node<K, V> newNode = new Node<>(key, value);
        newNode.next = table[index];
        table[index] = newNode;
        size++;
        return newNode;
    }

    public V get(K key) {
        int index = hash(key);
        for (Node<K, V> node = table[index]; node != null; node = node.next) {
            if (node.key.equals(key)) {
                return node.value;
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    public V remove(K key) {
        int index = hash(key);
        Node<K, V> node = table[index];
        Node<K, V> prev = null;

        while (node != null) {
            if (node.key.equals(key)) {
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

    @SuppressWarnings("unchecked")
    private void resize() {
        int oldCapacity = capacity;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            return;
        }

        capacity = oldCapacity << 1;
        Node<K, V>[] oldTable = table;
        table = new Node[capacity];
        size = 0;

        for (Node<K, V> head : oldTable) {
            Node<K, V> node = head;
            while (node != null) {
                put(node.key, node.value);
                node = node.next;
            }
        }
    }

    public int size() {
        return size;
    }

    public String[] keySet() {
        String[] keys = new String[size];
        int index = 0;
        for (Node<K, V> head : table) {
            Node<K, V> node = head;
            while (node != null) {
                keys[index++] = (String) node.key;
                node = node.next;
            }
        }
        return keys;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Node<K, V> head : table) {
            Node<K, V> node = head;
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
