package org.sfm.map;


import org.sfm.map.impl.FieldKey;

import java.lang.reflect.Type;

public abstract class ColumnDefinition<K extends FieldKey<K>> {
    public abstract K rename(K key);

    public abstract boolean hasCustomSource();

    public abstract Type getCustomSourceReturnType();
}