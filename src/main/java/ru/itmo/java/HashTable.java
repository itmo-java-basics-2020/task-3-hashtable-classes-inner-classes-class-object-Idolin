package ru.itmo.java;

import java.lang.reflect.Array;
import java.util.*;

import static java.lang.Math.max;

public class HashTable<K, V>
        implements Map<K, V> {
    private int size;
    private int maxIndex;
    private int threshold;
    private double loadFactor;
    private Entry<K, V>[] table;

    public HashTable(int capacity, double loadFactor) {
        size = 0;
        this.loadFactor = loadFactor;
        if (capacity <= 0)
            throw new IllegalArgumentException("Capacity must be greater then 0");
        if (capacity > (1 << 30))
            throw new IllegalArgumentException("Max capacity supported is 2^30");
        if (loadFactor <= 0 || loadFactor > 1)
            throw new IllegalArgumentException("loadFactor must be in range (0, 1]");
        maxIndex = 4;
        while (maxIndex < capacity) {
            maxIndex *= 2;
        }
        table = createArray(maxIndex + 1); // last element of array is always null
        threshold = max((int) (loadFactor * maxIndex), 1);
        --maxIndex; // maxIndex = 2 ^ x - 1
    }

    public HashTable(int capacity) {
        this(capacity, 0.7f);
    }

    public HashTable() {
        this(65536);
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
        return (position >= 0) ? (table[position]).getValue() : null;
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
            int entrySignedPosition;
            if (table[++position] == null) {
                if (position > maxIndex) {
                    for (position = 0; ; position++) {
                        if (table[position] == null) {
                            break;
                        }
                        entrySignedPosition = table[position].getSignedPosition(maxIndex);
                        if (entrySignedPosition < 0 && (entrySignedPosition & Integer.MAX_VALUE) <= shiftToPosition) {
                            table[shiftToPosition] = table[position].setSignPlus();
                            shiftToPosition = position;
                            break;
                        }
                    }
                }
                if (table[position] == null) {
                    table[shiftToPosition] = null;
                    return value;
                }
            }

            entrySignedPosition = table[position].getSignedPosition(maxIndex);
            if (entrySignedPosition <= shiftToPosition) {
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
                return -1;
            } else {
                if (entry.equals(table[position])) {
                    return position;
                }
                ++position;
            }
        }
    }

    private V putEntry(Entry<K, V> entry) {
        int hashPosition = entry.getPosition(maxIndex);
        int position = hashPosition;
        while (true) {
            if (table[position] != null) {
                if (entry.equals(table[position])) {
                    --size;
                    return (table[position]).setValue(entry.getValue());
                }
                ++position;
            } else {
                if (position > maxIndex) {
                    position = 0;
                } else {
                    if (position < hashPosition) {
                        entry.setSignMinus();
                    }
                    table[position] = entry;
                    return null;
                }
            }
        }
    }

    private void resize() {
        int oldCapacity = maxIndex + 1;
        maxIndex = (oldCapacity << 1);
        if (maxIndex < 0) {
            maxIndex = oldCapacity - 1;
            return;
        }

        Entry<K, V>[] oldTable = table;
        table = createArray(maxIndex + 1);
        threshold = max((int) (loadFactor * maxIndex), 1);
        --maxIndex;

        for (int i = 0; i < oldCapacity; i++) {
            if (oldTable[i] != null) {
                putEntry(oldTable[i].setSignPlus());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Entry<K, V>[] createArray(int length) {
        return (Entry<K, V>[]) Entry.createArray(length);
    }

    private static class Entry<K, V> implements Map.Entry<K, V> {
        final int hash;
        final K key;

        V value;
        int overEnd;

        protected Entry(K key, V value) {
            hash = key.hashCode();
            this.key = key;
            this.value = value;
            overEnd = 0;
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
            return maxIndex & hash;
        }

        public int getSignedPosition(int maxIndex) {
            return overEnd | getPosition(maxIndex);
        }

        protected Entry<K, V> setSignPlus() {
            overEnd = 0;
            return this;
        }

        protected void setSignMinus() {
            overEnd = Integer.MIN_VALUE;
        }

        boolean equals(Entry<?, ?> entry) {
            return (hash == entry.hash) && key.equals(entry.key);
        }

        static protected Object createArray(int length) {
            return Array.newInstance(Entry.class, length);
        }
    }
}
