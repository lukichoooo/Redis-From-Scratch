#include <cstdint>
#include <cstddef>
#include <cstdlib>
#include <assert.h>
#include <string>
#include <vector>
#include <cstring>

const size_t k_max_msg = 32 << 20; // or another appropriate value

// Response::status
enum {
    RES_OK = 0,
    RES_ERR = 1,    // error
    RES_NX = 2,     // key not found
};

template<typename T, typename M>
T* container_of(M* ptr, T* /*unused*/, M T::* member) {
    return reinterpret_cast<T*>(
        reinterpret_cast<char*>(ptr) - offsetof(T, member)
        );
}

// hashtable node, should be embedded into the payload
struct Node {
    Node* next = NULL;
    uint64_t hcode = 0;
};

// a simple fixed-sized hashtable
struct HashTable {
    Node** arr = NULL;
    size_t mask = 0;
    size_t size = 0;
};

// n must be a power of 2
static void h_init(HashTable* ht, size_t n) {
    assert(n > 0 && ((n - 1) & n) == 0);
    ht->arr = (Node**)calloc(sizeof(Node*), n);
    ht->mask = n - 1;
    ht->size = 0;
}
// hashtable insertion
static void h_insert(HashTable* ht, Node* node) {
    size_t pos = node->hcode & ht->mask;
    Node* next = ht->arr[pos];
    node->next = next;
    ht->arr[pos] = node;
    ht->size++;
}

// hashtable look up subroutine.
// Pay attention to the return value. It returns the address of
// the parent pointer that owns the target node,
// which can be used to delete the target node.
static Node** h_lookup(
    HashTable* htab,
    Node* key,
    bool (*cmp)(Node*, Node*))
{
    if (!htab->arr) {
        return NULL;
    }
    size_t pos = key->hcode & htab->mask;
    Node** from = &htab->arr[pos];
    while (*from) {
        if (cmp(*from, key)) {
            return from;
        }
        from = &(*from)->next;
    }
    return NULL;
}

// remove a node from the chain
static Node* h_detach(HashTable* ht, Node** from) {
    Node* node = *from;
    *from = (*from)->next;
    ht->size--;
    return node;
}

// the real hashtable interface.
// it uses 2 hashtables for progressive resizing.
struct HMap {
    HashTable ht1;
    HashTable ht2;
    size_t resizing_pos = 0;
};

const size_t k_resizing_work = 128;
static void hm_help_resizing(HMap* hmap) {
    if (hmap->ht2.arr == NULL) {
        return;
    }
    size_t nwork = 0;
    while (nwork < k_resizing_work && hmap->ht2.size > 0) {
    // scan for nodes from ht2 and move them to ht1
        Node** from = &hmap->ht2.arr[hmap->resizing_pos];
        if (!*from) {
            hmap->resizing_pos++;
            continue;
        }
        h_insert(&hmap->ht1, h_detach(&hmap->ht2, from));
        nwork++;
    }
    if (hmap->ht2.size == 0) {
    // done
        free(hmap->ht2.arr);
        hmap->ht2 = HashTable{};
    }
}


Node* hm_lookup(
    HMap* hmap, Node* key, bool (*cmp)(Node*, Node*))
{
    hm_help_resizing(hmap);
    Node** from = h_lookup(&hmap->ht1, key, cmp);
    if (!from) {
        from = h_lookup(&hmap->ht2, key, cmp);
    }
    return from ? *from : NULL;
}

const size_t k_max_load_factor = 8;
void hm_insert(HMap* hmap, Node* node) {
    if (!hmap->ht1.arr) {
        h_init(&hmap->ht1, 4);
    }
    h_insert(&hmap->ht1, node);
    if (!hmap->ht2.arr) {
    // check whether we need to resize
        size_t load_factor = hmap->ht1.size / (hmap->ht1.mask + 1);
        if (load_factor >= k_max_load_factor) {
            hm_start_resizing(hmap);
        }
    }
    hm_help_resizing(hmap);
}

static void hm_start_resizing(HMap* hmap) {
    assert(hmap->ht2.arr == NULL);
    // create a bigger hashtable and swap them
    hmap->ht2 = hmap->ht1;
    h_init(&hmap->ht1, (hmap->ht1.mask + 1) * 2);
    hmap->resizing_pos = 0;
}

const size_t k_min_load_factor = 4; // TODO: make it dynamically smaller under min load

Node* hm_pop(
    HMap* hmap, Node* key, bool (*cmp)(Node*, Node*))
{
    hm_help_resizing(hmap);
    Node** from = h_lookup(&hmap->ht1, key, cmp);
    if (from) {
        return h_detach(&hmap->ht1, from);
    }
    from = h_lookup(&hmap->ht2, key, cmp);
    if (from) {
        return h_detach(&hmap->ht2, from);
    }
    return NULL;
}


// the structure for the key
struct Entry {
    struct Node node;
    std::string key;
    std::string val;
};

// The data structure for the key space.
static struct {
    HMap db;
} g_data;

// Simple FNV-1a hash implementation for strings
static uint32_t str_hash(const uint8_t* data, size_t len) {
    uint32_t hash = 2166136261u;
    for (size_t i = 0; i < len; ++i) {
        hash ^= data[i];
        hash *= 16777619u;
    }
    return hash;
}

static uint32_t do_get(
    std::vector<std::string>& cmd, uint8_t* res, uint32_t* reslen)
{
    Entry key;
    key.key.swap(cmd[1]);
    key.node.hcode = str_hash((uint8_t*)key.key.data(), key.key.size());
    Node* node = hm_lookup(&g_data.db, &key.node, &entry_eq);
    if (!node) {
        return RES_NX;
    }
    const std::string& val = container_of(static_cast<Node*>(node), static_cast<Entry*>(nullptr), &Entry::node)->val;
    assert(val.size() <= k_max_msg);
    memcpy(res, val.data(), val.size());
    *reslen = (uint32_t)val.size();
    return RES_OK;
}
static bool entry_eq(Node* lhs, Node* rhs) {
    Entry* le = container_of(lhs, static_cast<Entry*>(nullptr), &Entry::node);
    Entry* re = container_of(rhs, static_cast<Entry*>(nullptr), &Entry::node);
    return lhs->hcode == rhs->hcode && le->key == re->key;
}

static uint32_t do_set(
    std::vector<std::string>& cmd, uint8_t* res, uint32_t* reslen)
{
    (void)res;
    (void)reslen;
    Entry key;
    key.key.swap(cmd[1]);
    key.node.hcode = str_hash((uint8_t*)key.key.data(), key.key.size());
    Node* node = hm_lookup(&g_data.db, &key.node, &entry_eq);
    if (node) {
        container_of(node, static_cast<Entry*>(nullptr), &Entry::node)->val.swap(cmd[2]);
    }
    else {
        Entry* ent = new Entry();
        ent->key.swap(key.key);
        ent->node.hcode = key.node.hcode;
        ent->val.swap(cmd[2]);
        hm_insert(&g_data.db, &ent->node);
    }
    return RES_OK;
}
static uint32_t do_del(
    std::vector<std::string>& cmd, uint8_t* res, uint32_t* reslen)
{
    (void)res;
    (void)reslen;
    Entry key;
    key.key.swap(cmd[1]);
    key.node.hcode = str_hash((uint8_t*)key.key.data(), key.key.size());
    Node* node = hm_pop(&g_data.db, &key.node, &entry_eq);
    if (node) {
        delete container_of(node, static_cast<Entry*>(nullptr), &Entry::node);
    }
    return RES_OK;
}