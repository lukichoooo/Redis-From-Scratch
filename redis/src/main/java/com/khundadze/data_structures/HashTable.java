package com.khundadze.data_structures;

public class HashTable<V> {

    public static class Node<V> {
        final int key;
        V value;
        Node<V> next;

        Node(int key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return key + ":" + value;
        }
    }

    private Node<V>[] table;
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

    private int hash(int key) {
        int h = key;
        h ^= (h >>> 16);
        return h & (capacity - 1);
    }

    public Node<V> put(int key, V value) {
        if (size >= capacity * loadFactor) {
            resize();
        }
        int index = hash(key);
        for (Node<V> node = table[index]; node != null; node = node.next) {
            if (node.key == key) {
                node.value = value;
                return null;
            }
        }
        Node<V> newNode = new Node<>(key, value);
        newNode.next = table[index];
        table[index] = newNode;
        size++;
        return newNode;
    }

    public V get(int key) {
        int index = hash(key);
        for (Node<V> node = table[index]; node != null; node = node.next) {
            if (node.key == key) {
                return node.value;
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean containsKey(int key) {
        return get(key) != null;
    }

    public V remove(int key) {
        int index = hash(key);
        Node<V> node = table[index];
        Node<V> prev = null;

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

    @SuppressWarnings("unchecked")
    private void resize() {
        int oldCapacity = capacity;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            return;
        }

        capacity = oldCapacity << 1;
        Node<V>[] oldTable = table;
        table = new Node[capacity];
        size = 0;

        for (Node<V> head : oldTable) {
            Node<V> node = head;
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
        for (Node<V> head : table) {
            Node<V> node = head;
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

    public Object keySet() {
        Object[] keys = new Object[size];
        int index = 0;
        for (Node<V> head : table) {
            Node<V> node = head;
            while (node != null) {
                keys[index++] = node.key;
                node = node.next;
            }
        }
        return keys;
    }
}