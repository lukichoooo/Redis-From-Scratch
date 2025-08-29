package com.khundadze.data_structures;

public class ZSet<V> {

    public static class ZNode<V> {
        SkipList.Node<ZNode<V>> listNode; // pointer in skip list
        HashTable.Node<ZNode<V>> mapNode; // pointer in hash table
        public double score;
        public String name;
        public V value;

        ZNode(String name, double score, V value) {
            this.name = name;
            this.score = score;
            this.value = value;
        }

        @Override
        public String toString() {
            return "(" + name + ", score=" + score + ")";
        }
    }

    private final HashTable<ZNode<V>> ht;
    private final SkipList<ZNode<V>> sl;

    public ZSet() {
        ht = new HashTable<>();
        sl = new SkipList<>();
    }

    // Combine score + name into sortable int key
    private int scoreKey(double score, String name) {
        return (int) score * 31 + name.hashCode();
    }

    // Add or update node
    public void add(String name, double score, V value) {
        int key = name.hashCode();
        ZNode<V> node = ht.get(key);

        if (node != null) {
            // update score/value
            node.value = value;
            if (node.score != score) {
                // remove old skiplist position
                sl.remove(node.listNode.key);

                node.score = score;
                // re-insert in skiplist
                node.listNode = sl.insert(scoreKey(score, name), node);
            }
        } else {
            node = new ZNode<>(name, score, value);
            node.mapNode = ht.put(key, node); // store pointer in hash table
            node.listNode = sl.insert(scoreKey(score, name), node); // store pointer in skiplist
        }
    }

    public boolean remove(String name) {
        int key = name.hashCode();
        ZNode<V> node = ht.get(key);
        if (node == null)
            return false;

        sl.remove(node.listNode.key); // remove via skiplist pointer
        ht.remove(key); // remove via hash key
        return true;
    }

    public ZNode<V> lookup(String name) {
        return ht.get(name.hashCode());
    }

    // Query: first node >= (score, name) and move offset steps forward
    public ZNode<V> query(double score, String name, int offset) {
        if (offset < 0) {
            throw new UnsupportedOperationException("Backward queries are not supported");
        }

        SkipList.Node<ZNode<V>> node = sl.ceiling(scoreKey(score, name));
        for (int i = 0; i < offset && node != null; i++) {
            node = node.next[0];
        }
        return node != null ? node.value : null;
    }

}
