package com.frederiksen.love.utils;

/**
 * Very simple interface for vector space like objects, yes I know there are more
 * properties than this, just shhhh.
 *
 * @param <V> vector type
 * @param <F> field type
 */
public interface VectorSpace<V, F> {

    default V add(final V other) {
        return mulAdd(other, getMulIdentity());
    }

    default V sub(final V other) {
        return mulAdd(other, negate(getMulIdentity()));
    }

    V scl(final F scalar);

    V mulAdd(final V other, final F scalar);

    F getMulIdentity();

    F invert(F scalar);

    F sqrt(F scalar);

    F negate(F scalar);
}
