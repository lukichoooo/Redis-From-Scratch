package com.khundadze.data_structures;

public class ZSet<V> {

    /** Sort by score, then by name (like Redis ZSET) */
    private static class ScoreKey implements Comparable<ScoreKey> {
        final double score;
        final String name;

        ScoreKey(double score, String name) {
            this.score = score;
            this.name = name;
        }

        @Override
        public int compareTo(ScoreKey other) {
            int c = Double.compare(this.score, other.score);
            if (c != 0)
                return c;
            return this.name.compareTo(other.name);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ScoreKey k))
                return false;
            return Double.compare(score, k.score) == 0 && name.equals(k.name);
        }

        @Override
        public int hashCode() {
            return 31 * Double.hashCode(score) + name.hashCode();
        }

        @Override
        public String toString() {
            return "(" + score + ", " + name + ")";
        }
    }

    public static class ZNode<V> {
        SkipList.Node<ScoreKey, ZNode<V>> listNode; // pointer in skip list
        HashTable.Node<String, ZNode<V>> mapNode; // pointer in hash table
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
            return "(" + name + ", score=" + score + ", value=" + value + ")";
        }
    }

    private final HashTable<String, ZNode<V>> ht;
    private final SkipList<ScoreKey, ZNode<V>> sl;

    public ZSet() {
        this.ht = new HashTable<>();
        this.sl = new SkipList<>();
    }

    /** Add or update node */
    public void add(String name, double score, V value) {
        ZNode<V> node = ht.get(name);

        if (node != null) {
            node.value = value;
            if (Double.compare(node.score, score) != 0) {
                // remove old position and reinsert with new score
                sl.remove(node.listNode.key);
                node.score = score;
                node.listNode = sl.insert(new ScoreKey(score, name), node);
            }
        } else {
            node = new ZNode<>(name, score, value);
            node.mapNode = ht.put(name, node); // store in hash table by real key
            node.listNode = sl.insert(new ScoreKey(score, name), node); // store in skiplist by (score,name)
        }
    }

    public boolean remove(String name) {
        ZNode<V> node = ht.get(name);
        if (node == null)
            return false;
        sl.remove(node.listNode.key);
        ht.remove(name);
        return true;
    }

    public ZNode<V> get(String name) {
        return ht.get(name);
    }

    public int size() {
        return ht.size();
    }

    public boolean isEmpty() {
        return ht.isEmpty();
    }

    public String[] keySet() {
        return ht.keySet(); // returns the actual String keys now
    }

    /** First node >= (score,name), then move 'offset' steps forward */
    public ZNode<V> query(double score, String name, int offset) {
        if (offset < 0)
            throw new UnsupportedOperationException("Backward queries are not supported");
        SkipList.Node<ScoreKey, ZNode<V>> n = sl.ceiling(new ScoreKey(score, name));
        for (int i = 0; i < offset && n != null; i++)
            n = n.next[0];
        return n != null ? n.value : null;
    }
}
