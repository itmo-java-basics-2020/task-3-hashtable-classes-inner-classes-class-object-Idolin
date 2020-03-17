package ru.itmo.java;

import java.lang.reflect.Array;
import java.util.*;

import static java.lang.Math.max;

public class HashTable<K, V>
        implements Map<K, V> {
    private int size;
    private int maxIndex;
    private int threshold;
    private final double loadFactor;
    private Entry<K, V>[] table;

    public HashTable(int capacity, double loadFactor) {
        size = 0;
        this.loadFactor = loadFactor;
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be greater then 0");
        }
        int MAX_ARRAY_SIZE = (int) Math.pow(2, 30);
        if (capacity > MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException("Max capacity supported is 2 ^ 30");
        }
        if (loadFactor <= 0 || loadFactor > 1) {
            throw new IllegalArgumentException("loadFactor must be in range (0, 1]");
        }
        maxIndex = max(upToClosestPowerOf2(capacity), 4);
        table = createArray(maxIndex + 1); // last element of array is always null
        --maxIndex; // maxIndex = capacity - 1 = 2 ^ x - 1
        threshold = max((int) (loadFactor * maxIndex), 1); // at least one place is always free
    }

    public HashTable(int capacity) {
        this(capacity, 0.7f); // default load factor = 0.7
    }

    public HashTable() {
        this(32); // default initial capacity = 32
    }

    @Override
    public V put(K key, V value) {
        if (size++ == threshold) {
            resize();
        }
        Entry<K, V> entry = new Entry<>(key, value);
        return putEntry(entry);
    }

    @Override
    public V get(Object key) {
        Entry<?, ?> entry = new Entry<>(key);
        int position = getPosition(entry);
        return (position >= 0) ? table[position].getValue() : null;
    }

    @Override
    public V remove(Object key) {
        Entry<?, ?> entry = new Entry<>(key);
        int position = getPosition(entry);

        if (position < 0) {
            return null;
        }

        --size;
        V value = table[position].getValue();
        int shiftToPosition = position;

        while (true) {
            if (table[++position] == null) {
                if (position > maxIndex) {
                    position = 0;
                }
                if (table[position] == null) {
                    table[shiftToPosition] = null;
                    return value;
                }
            }

            // shift deletion algorithm
            int entryPosition = table[position].getPosition(maxIndex);
            if ((position < shiftToPosition) ?
                    (position < entryPosition && entryPosition <= shiftToPosition) :
                    (position < entryPosition || entryPosition <= shiftToPosition)) {
                table[shiftToPosition] = table[position];
                shiftToPosition = position;
            }
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        for (int i = 0; i <= maxIndex; i++) {
            table[i] = null;
        }
        size = 0;
    }

    @Override
    public Set<K> keySet() {
        Set<K> kSet = new HashSet<>();
        for (int i = 0; i <= maxIndex; i++) {
            if (table[i] != null) {
                kSet.add(table[i].getKey());
            }
        }
        return kSet;
    }

    @Override
    public Collection<V> values() {
        Collection<V> values = new ArrayList<>();
        for (int i = 0; i <= maxIndex; i++) {
            if (table[i] != null) {
                values.add(table[i].getValue());
            }
        }
        return values;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> entrySet = new HashSet<>();
        for (int i = 0; i <= maxIndex; i++) {
            if (table[i] != null) {
                entrySet.add(table[i]);
            }
        }
        return entrySet;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return (size == 0);
    }

    @Override
    public boolean containsKey(Object key) {
        int position = getPosition(new Entry<>(key));
        return (position >= 0);
    }

    @Override
    public boolean containsValue(Object value) {
        for (int i = 0; i <= maxIndex; i++) {
            if (table[i] != null) {
                V entryValue = table[i].getValue();
                if (entryValue == null) {
                    if (value == null) {
                        return true;
                    }
                } else if (entryValue.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getPosition(Entry<?, ?> entry) {
        int position = entry.getPosition(maxIndex);
        while (true) {
            if (table[position] == null) {
                if (position > maxIndex) {
                    position = 0;
                    continue;
                }
                return Integer.MIN_VALUE | position; // returns position with sign bit set if key is not in hashtable
            } else {
                if (entry.equals(table[position])) {
                    return position;
                }
                ++position;
            }
        }
    }

    private V putEntry(Entry<K, V> entry) {
        int position = getPosition(entry);
        if (position >= 0) {
            --size;
            return table[position].setValue(entry.getValue());
        }
        position = Integer.MAX_VALUE & position; // getting actual first free place
        table[position] = entry;
        return null;
    }

    private void resize() {
        int oldCapacity = maxIndex + 1;

        // overflow-conscious code
        maxIndex = (oldCapacity << 1);
        if (maxIndex < 0) {
            // keep running with max capacity
            maxIndex = oldCapacity - 1;
            return;
        }

        Entry<K, V>[] oldTable = table;
        table = createArray(maxIndex + 1); // last element of array is always null
        --maxIndex; // maxIndex = capacity - 1 = 2 ^ x - 1
        threshold = max((int) (loadFactor * maxIndex), 1); // at least one place is always free

        for (int i = 0; i < oldCapacity; i++) {
            if (oldTable[i] != null) {
                putEntry(oldTable[i]);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Entry<K, V>[] createArray(int length) {
        return (Entry<K, V>[]) Entry.createArray(length);
    }

    private static int upToClosestPowerOf2(int value) {
        int pow2 = 1;
        while (pow2 < value) {
            pow2 *= 2;
        }
        return pow2;
    }

    private static class Entry<K, V> implements Map.Entry<K, V> {
        final int hash;
        final K key;
        V value;

        protected Entry(K key, V value) {
            hash = key.hashCode();
            this.key = key;
            this.value = value;
        }

        protected Entry(K key) {
            this(key, null);
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        public int getPosition(int maxIndex) {
            /* maxIndex equals to capacity - 1 and capacity equals to the power of 2
             * thus maxIndex can serve as a bit mask, which cuts off the left part of the hash
             * the remaining bits of the hash then represents the initial computed position of the entry
             */
            return maxIndex & hash;
        }

        boolean equals(Entry<?, ?> entry) {
            return (hash == entry.hash) && key.equals(entry.key);
        }

        static protected Object createArray(int length) {
            return Array.newInstance(Entry.class, length);
        }
    }
}
