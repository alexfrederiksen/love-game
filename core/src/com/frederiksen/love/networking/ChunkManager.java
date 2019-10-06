package com.frederiksen.love.networking;

import com.badlogic.gdx.utils.Pool;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Manages elements in chunks, useful for chunking packets to send over a network
 *
 * @param <E>
 */
public class ChunkManager<E> {

    private int chunkSize;
    private List<Chunk<E>> chunks;
    private Pool<Chunk<E>> chunkPool;

    public static class Chunk<E> extends ArrayList<E> implements Pool.Poolable {
        public Chunk(int initialCapacity) {
            super(initialCapacity);
        }

        @Override
        public void reset() {
            clear();
        }
    }

    public class ChunkPool extends Pool<Chunk<E>> {
        private Supplier<Chunk<E>> chunkFactory;

        public ChunkPool(int initialCapacity, Supplier<Chunk<E>> chunkFactory) {
            super(initialCapacity);
            this.chunkFactory = chunkFactory;
        }

        @Override
        protected Chunk<E> newObject() {
            return chunkFactory.get();
        }
    }

    public ChunkManager(int chunkSize, int initialChunks, Supplier<Chunk<E>> chunkFactory) {
        this.chunkSize = chunkSize;
        this.chunkPool = new ChunkPool(initialChunks, chunkFactory);
        this.chunks = new ArrayList<>();
    }

    public void add(E e) {
        if (chunks.isEmpty())
            chunks.add(chunkPool.obtain());

        Chunk<E> lastChunk = chunks.get(chunks.size() - 1);

        if (lastChunk.size() >= chunkSize) {
            // add new chunk
            chunks.add(lastChunk = chunkPool.obtain());
        }

        // add to last chunk
        lastChunk.add(e);
    }

    private void rangeCheck(int chunkIndex, int eIndex) {
        if (chunkIndex >= chunks.size()) throw new IndexOutOfBoundsException();
        if (eIndex >= chunks.get(chunkIndex).size()) throw new IndexOutOfBoundsException();
    }

    public void set(int index, E e) {
        int chunkIndex = index / chunkSize;
        int eIndex = index % chunkIndex;

        rangeCheck(chunkIndex, eIndex);

        chunks.get(chunkIndex).set(eIndex, e);
    }

    public E get(int index) {
        int chunkIndex = index / chunkSize;
        int eIndex = index % chunkIndex;

        rangeCheck(chunkIndex, eIndex);

        return chunks.get(chunkIndex).get(eIndex);
    }

    public void clear() {
        // free all chunks
        chunks.forEach(chunkPool::free);
        // clear chunks (literally just sets the cells to null)
        chunks.clear();
    }

    public List<Chunk<E>> getChunks() {
        return chunks;
    }
}
