package com.intellij.util.containers;

import gnu.trove.TObjectHashingStrategy;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class ObjectIntHashMap<K> extends TObjectIntHashMap<K> implements ObjectIntMap<K> {
    public ObjectIntHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public ObjectIntHashMap(@NotNull TObjectHashingStrategy<K> strategy) {
        super(strategy);
    }

    public ObjectIntHashMap(int initialCapacity, @NotNull TObjectHashingStrategy<K> strategy) {
        super(initialCapacity, strategy);
    }

    public ObjectIntHashMap() {
        super();
    }

    public final int get(@NotNull K key) {
        return this.get(key, -1);
    }

    @Override
    public @NotNull
    Set<K> keySet() {
        return Collections.emptySet();
    }

    @NotNull
    @Override
    public int[] values() {
        return Arrays.copyOf(_values, _values.length);
    }

    @Override
    public @NotNull
    Iterable<Entry<K>> entries() {
        return Collections.emptySet();
    }

    public final int get(K key, int defaultValue) {
        int index = this.index(key);
        return index < 0 ? defaultValue : this._values[index];
    }

    public int put(K key, int value, int defaultValue) {
        int index = this.index(key);
        int prev = super.put(key, value);
        return index >= 0 ? prev : defaultValue;
    }
}