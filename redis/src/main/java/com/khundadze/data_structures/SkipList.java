package com.khundadze.data_structures;

import java.util.concurrent.ThreadLocalRandom;

public class SkipList<K extends Comparable<K>, V> {

    public static class Node<K, V> {
        final public K key;
        public V value;
        final public Node<K, V>[] next;

        @SuppressWarnings("unchecked")
        Node(K key, V value, int level) {
            this.key = key;
            this.value = value;
            this.next = new Node[level + 1];
        }

        @Override
        public String toString() {
            return key + ":" + value;
        }
    }

    private static final int MAX_LEVEL = 16;
    private final Node<K, V> head;
    private int level = 0;
    private int size = 0;

    public SkipList() {
        head = new Node<>(null, null, MAX_LEVEL);
    }

    private int randomLevel() {
        int lvl = 0;
        while (lvl < MAX_LEVEL && ThreadLocalRandom.current().nextBoolean()) {
            lvl++;
        }
        return lvl;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    public Node<K, V> ceiling(K key) {
        Node<K, V> x = head;
        for (int i = level; i >= 0; i--) {
            while (x.next[i] != null && x.next[i].key.compareTo(key) < 0) {
                x = x.next[i];
            }
        }
        return x.next[0]; // first node >= key
    }

    // Insert or replace value
    public Node<K, V> insert(K key, V value) {
        @SuppressWarnings("unchecked")
        Node<K, V>[] update = new Node[MAX_LEVEL + 1];
        Node<K, V> u = head;

        for (int i = level; i >= 0; --i) {
            while (u.next[i] != null && u.next[i].key.compareTo(key) < 0) {
                u = u.next[i];
            }
            update[i] = u;
        }

        Node<K, V> candidate = u.next[0];
        if (candidate != null && candidate.key.equals(key)) {
            candidate.value = value;
            return candidate;
        }

        int lvl = randomLevel();
        if (lvl > level) {
            for (int i = level + 1; i <= lvl; i++) {
                update[i] = head;
            }
            level = lvl;
        }

        Node<K, V> newNode = new Node<>(key, value, lvl);

        for (int i = 0; i <= lvl; i++) {
            newNode.next[i] = update[i].next[i];
            update[i].next[i] = newNode;
        }

        size++;
        return newNode;
    }

    public Node<K, V> remove(K key) {
        @SuppressWarnings("unchecked")
        Node<K, V>[] update = new Node[MAX_LEVEL + 1];
        Node<K, V> u = head;

        for (int i = level; i >= 0; i--) {
            while (u.next[i] != null && u.next[i].key.compareTo(key) < 0) {
                u = u.next[i];
            }
            update[i] = u;
        }

        Node<K, V> target = u.next[0];
        if (target == null || !target.key.equals(key)) {
            return null;
        }

        for (int i = 0; i <= level; i++) {
            if (update[i].next[i] == target) {
                update[i].next[i] = target.next[i];
            }
        }

        while (level > 0 && head.next[level] == null) {
            level--;
        }
        size--;

        for (int i = 0; i < target.next.length; i++) {
            target.next[i] = null;
        }

        return target;
    }

    public Node<K, V> get(K key) {
        Node<K, V> u = head;
        for (int i = level; i >= 0; i--) {
            while (u.next[i] != null && u.next[i].key.compareTo(key) < 0) {
                u = u.next[i];
            }
        }
        u = u.next[0];
        return (u != null && u.key.equals(key)) ? u : null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SkipList(size=").append(size).append(", level=").append(level).append(") [");
        Node<K, V> u = head.next[0];
        while (u != null) {
            sb.append(u);
            u = u.next[0];
            if (u != null)
                sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
