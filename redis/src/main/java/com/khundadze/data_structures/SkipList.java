package com.khundadze.data_structures;

import java.util.concurrent.ThreadLocalRandom;

public class SkipList<V> {

    public static class Node<V> {
        final int key;
        V value;
        final Node<V>[] next;

        @SuppressWarnings("unchecked")
        Node(int key, V value, int level) {
            this.key = key;
            this.value = value;
            this.next = new Node[level + 1];
        }

        @Override
        public String toString() {
            return key + ":" + value;
        }
    }

    private final int MAX_LEVEL = 16;
    private final Node<V> head;
    private int level = 0;
    private int size = 0;

    public SkipList() {
        head = new Node<>(Integer.MIN_VALUE, null, MAX_LEVEL);
    }

    private int randomLevel() {
        int lvl = 0;
        // each iteration has ~50% chance to increase level
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

    public boolean containsKey(int key) {
        return get(key) != null;
    }

    public Node<V> ceiling(int key) {
        Node<V> x = head;
        for (int i = level; i >= 0; i--) {
            while (x.next[i] != null && x.next[i].key < key) {
                x = x.next[i];
            }
        }
        return x.next[0]; // first node >= key
    }

    // Insert or replace value for existing key
    public Node<V> insert(int key, V value) {
        @SuppressWarnings("unchecked")
        Node<V>[] update = new Node[MAX_LEVEL + 1];
        Node<V> u = head;

        for (int i = level; i >= 0; --i) {
            while (u.next[i] != null && u.next[i].key < key) {
                u = u.next[i];
            }
            update[i] = u;
        }

        Node<V> candidate = u.next[0];
        if (candidate != null && candidate.key == key) {
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

        Node<V> newNode = new Node<>(key, value, lvl);

        // link the new node at all its levels
        for (int i = 0; i <= lvl; i++) {
            newNode.next[i] = update[i].next[i];
            update[i].next[i] = newNode;
        }

        size++;
        return newNode;
    }

    public Node<V> remove(int key) {
        @SuppressWarnings("unchecked")
        Node<V>[] update = new Node[MAX_LEVEL + 1];
        Node<V> u = head;

        for (int i = level; i >= 0; i--) {
            while (u.next[i] != null && u.next[i].key < key) {
                u = u.next[i];
            }
            update[i] = u;
        }

        Node<V> target = u.next[0];
        if (target == null || target.key != key) {
            return null; // not found
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

    public Node<V> get(int key) {
        Node<V> u = head;
        for (int i = level; i >= 0; i--) {
            while (u.next[i] != null && u.next[i].key < key) {
                u = u.next[i];
            }
        }
        u = u.next[0];
        if (u != null && u.key == key)
            return u;
        return null;
    }

    // pretty print level-0 list plus metadata
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SkipList(size=").append(size).append(", level=").append(level).append(") [");
        Node<V> u = head.next[0];
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
