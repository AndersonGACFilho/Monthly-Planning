package com.planejamentomensal.shared.domain;

import java.lang.reflect.Field;

/**
 * Gives entities an id in unit tests, as the database would (TST-05).
 */
public final class Ids {

    private Ids() {
    }

    /**
     * Sets the {@code id} field of an entity.
     *
     * @param entity entity
     * @param id     id
     * @param <T>    entity type
     * @return the same entity
     */
    public static <T> T withId(T entity, long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
