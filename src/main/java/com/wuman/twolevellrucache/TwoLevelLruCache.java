/*                                                                                                                                                                    
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wuman.twolevellrucache;

import com.jakewharton.disklrucache.DiskLruCache;
import com.jakewharton.disklrucache.DiskLruCache.Editor;
import com.jakewharton.disklrucache.DiskLruCache.Snapshot;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * A two-level LRU cache composed of a smaller, first level {@code LruCache} in
 * memory and a larger, second level {@code DiskLruCache}.
 * 
 * The keys must be of {@code String} type. The values must be convertible to
 * and from a byte stream using a {@code Converter}.
 * 
 * @author wuman
 * 
 * @param <V>
 */
public class TwoLevelLruCache<V> {

    private static final int INDEX_VALUE = 0; // allow only one value per entry

    private final LruCache<String, V> mMemCache;
    private final DiskLruCache mDiskCache;
    private final Converter<V> mConverter;

    /**
     * Constructor for TwoLevelLruCache. Use this constructor if only the first
     * level memory cache is needed.
     * 
     * @param maxSizeMem
     */
    public TwoLevelLruCache(int maxSizeMem) {
        super();

        mDiskCache = null;
        mConverter = null;
        mMemCache = new LruCache<String, V>(maxSizeMem) {

            @Override
            protected void entryRemoved(boolean evicted, String key,
                    V oldValue, V newValue) {
                wrapEntryRemoved(evicted, key, oldValue, newValue);
            }

            @Override
            protected V create(String key) {
                return wrapCreate(key);
            }

            @Override
            protected int sizeOf(String key, V value) {
                return wrapSizeOf(key, value);
            }

        };
    }

    /**
     * Constructor for TwoLevelLruCache. Use this constructor if the second
     * level disk cache is to be enabled.
     * 
     * @param directory
     *            a writable directory for the L2 disk cache.
     * @param appVersion
     * @param maxSizeMem
     *            the maximum sum of the sizes of the entries in the L1 mem
     *            cache.
     * @param maxSizeDisk
     *            the maximum number of bytes the L2 disk cache should use to
     *            store.
     * @param converter
     *            a {@code Converter} that is able to convert a byte stream to
     *            and from type {@code V}.
     * @throws IOException
     */
    public TwoLevelLruCache(File directory, int appVersion, int maxSizeMem,
            long maxSizeDisk, Converter<V> converter) throws IOException {
        super();

        if (maxSizeMem >= maxSizeDisk) {
            throw new IllegalArgumentException(
                    "It makes more sense to have a larger second-level disk cache.");
        }

        if (converter == null) {
            throw new IllegalArgumentException("A converter must be submitted.");
        }

        mConverter = converter;

        mMemCache = new LruCache<String, V>(maxSizeMem) {

            @Override
            protected void entryRemoved(boolean evicted, String key,
                    V oldValue, V newValue) {
                wrapEntryRemoved(evicted, key, oldValue, newValue);
            }

            @Override
            protected V create(String key) {
                return wrapCreate(key);
            }

            @Override
            protected int sizeOf(String key, V value) {
                return wrapSizeOf(key, value);
            }

        };
        mDiskCache = DiskLruCache.open(directory, appVersion, 1, maxSizeDisk);
    }

    /**
     * Returns the value for {@code key} if it exists in the cache or can be
     * created by {@code #create(String)}.
     * 
     * @param key
     * @return value
     */
    @SuppressWarnings("unchecked")
    public final V get(String key) {
        V value = mMemCache.get(key);
        if (mDiskCache != null && value == null) {
            Snapshot snapshot = null;
            InputStream in = null;
            try {
                snapshot = mDiskCache.get(key);
                if (snapshot != null) {
                    in = snapshot.getInputStream(INDEX_VALUE);
                    byte[] bytes = IOUtils.toByteArray(in);
                    value = mConverter.from(bytes);
                }
            } catch (IOException e) {
                System.out.println("Unable to get entry from disk cache. key: "
                        + key);
            } catch (Exception e) {
                System.out.println("Unable to get entry from disk cache. key: "
                        + key);
            } catch (OutOfMemoryError e) {
                System.out.println("Unable to get entry from disk cache. key: "
                    + key);
            } finally {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(snapshot);
            }
            if (value != null) {
                // write back to mem cache
                mMemCache.put(key, value);
            }
        }
        return value;
    }

    /**
     * Caches {@code newValue} for {@code key}.
     * 
     * @param key
     * @param newValue
     * @return oldValue
     */
    public final V put(String key, V newValue) {
        V oldValue = mMemCache.put(key, newValue);
        putToDiskQuietly(key, newValue);
        return oldValue;
    }

    private void removeFromDiskQuietly(String key) {
        if (mDiskCache == null) {
            return;
        }
        try {
            mDiskCache.remove(key);
        } catch (IOException e) {
            System.out.println("Unable to remove entry from disk cache. key: "
                    + key);
        }
    }

    private void putToDiskQuietly(String key, V newValue) {
        if (mDiskCache == null) {
            return;
        }
        Editor editor = null;
        OutputStream out = null;
        try {
            editor = mDiskCache.edit(key);
            if (editor != null) {
                out = editor.newOutputStream(INDEX_VALUE);
                mConverter.toStream(newValue, out);
                editor.commit();
            }
        } catch (IOException e) {
            System.out
                    .println("Unable to put entry to disk cache. key: " + key);
        } finally {
            IOUtils.closeQuietly(out);
            quietlyAbortUnlessCommitted(editor);
        }
    }

    private static void quietlyAbortUnlessCommitted(DiskLruCache.Editor editor) {
        // Give up because the cache cannot be written.
        try {
            if (editor != null) {
                editor.abortUnlessCommitted();
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Removes the entry for {@code key} if it exists.
     * 
     * @param key
     * @return oldValue
     */
    public final V remove(String key) {
        V oldValue = mMemCache.remove(key);
        removeFromDiskQuietly(key);
        return oldValue;
    }

    private void wrapEntryRemoved(boolean evicted, String key, V oldValue,
            V newValue) {
        entryRemoved(evicted, key, oldValue, newValue);
        if (!evicted) {
            removeFromDiskQuietly(key);
        }
    }

    /**
     * Called for entries that have been evicted or removed. This method is
     * invoked when a value is evicted to make space, removed by a call to
     * {@link #remove}, or replaced by a call to {@link #put}. The default
     * implementation does nothing.
     * 
     * <p>
     * The method is called without synchronization: other threads may access
     * the cache while this method is executing.
     * 
     * @param evicted
     *            true if the entry is being removed to make space, false if the
     *            removal was caused by a {@link #put} or {@link #remove}.
     * @param key
     * @param oldValue
     * @param newValue
     *            the new value for {@code key}, if it exists. If non-null,this
     *            removal was caused by a {@link #put}. Otherwise it was caused
     *            by an eviction or a {@link #remove}.
     */
    protected void entryRemoved(boolean evicted, String key, V oldValue,
            V newValue) {
    }

    private V wrapCreate(String key) {
        V createdValue = create(key);
        if (createdValue == null) {
            return null;
        }
        putToDiskQuietly(key, createdValue);
        return createdValue;
    }

    /**
     * Called after a cache miss to compute a value for the corresponding key.
     * Returns the computed value or null if no value can be computed. The
     * default implementation returns null.
     * 
     * <p>
     * The method is called without synchronization: other threads may access
     * the cache while this method is executing.
     * 
     * <p>
     * If a value for {@code key} exists in the cache when this method returns,
     * the created value will be released with {@link #entryRemoved} and
     * discarded. This can occur when multiple threads request the same key at
     * the same time (causing multiple values to be created), or when one thread
     * calls {@link #put} while another is creating a value for the same key.
     * 
     * @param key
     * @return createdValue
     */
    protected V create(String key) {
        return null;
    }

    private int wrapSizeOf(String key, V value) {
        return sizeOf(key, value);
    }

    /**
     * Returns the size of the entry for {@code key} and {@code value} in
     * user-defined units. The default implementation returns 1 so that size is
     * the number of entries and max size is the maximum number of entries.
     * 
     * <p>
     * An entry's size must not change while it is in the cache.
     * 
     * @param key
     * @param value
     * @return sizeOfEntry
     */
    protected int sizeOf(String key, V value) {
        return 1;
    }

    /**
     * Returns the sum of the sizes of the entries in the L1 mem cache.
     * 
     * @return size
     */
    public synchronized final int sizeMem() {
        return mMemCache.size();
    }

    /**
     * Returns the number of bytes currently being used to store the values in
     * the L2 disk cache. This may be greater than the max size if a background
     * deletion is pending.
     * 
     * @return size
     */
    public synchronized final long sizeDisk() {
        return mDiskCache == null ? 0 : mDiskCache.size();
    }

    /**
     * Returns the maximum sum of the sizes of the entries in the L1 mem cache.
     * 
     * @return maxSize
     */
    public synchronized final int maxSizeMem() {
        return mMemCache.maxSize();
    }

    /**
     * Returns the maximum number of bytes that the L2 disk cache should use to
     * store its data.
     * 
     * @return maxSize
     */
    public synchronized final long maxSizeDisk() {
        return mDiskCache == null ? 0L : mDiskCache.getMaxSize();
    }

    /**
     * Clear both mem and disk caches. Internally this method calls both
     * {@link #evictAllMem()} and {@link #evictAllDisk()}.
     * 
     * @throws IOException
     */
    public final void evictAll() throws IOException {
        evictAllMem();
        evictAllDisk();
    }

    /**
     * Clear the L1 mem cache, calling {@link #entryRemoved} on each removed
     * entry.
     */
    public final void evictAllMem() {
        mMemCache.evictAll();
    }

    /**
     * Closes the L2 disk cache and deletes all of its stored values. This will
     * delete all files in the cache directory including files that weren't
     * created by the cache.
     * 
     * @throws IOException
     */
    public final void evictAllDisk() throws IOException {
        if (mDiskCache != null) {
            mDiskCache.delete();
        }
    }

    /**
     * Returns the number of times {@link #get} returned a value.
     * 
     * @return count
     */
    public synchronized final int hitCount() {
        return mMemCache.hitCount();
    }

    /**
     * Returns the number of times {@link #get} returned null or required a new
     * value to be created.
     * 
     * @return count
     */
    public synchronized final int missCount() {
        return mMemCache.missCount();
    }

    /**
     * Returns the number of times {@link #create(String)} returned a value.
     * 
     * @return count
     */
    public synchronized final int createCount() {
        return mMemCache.createCount();
    }

    /**
     * Returns the number of times {@link #put} was called.
     * 
     * @return count
     */
    public synchronized final int putCount() {
        return mMemCache.putCount();
    }

    /**
     * Returns the number of values that have been evicted.
     * 
     * @return count
     */
    public synchronized final int evictionCount() {
        return mMemCache.evictionCount();
    }

    /**
     * Returns a copy of the current contents of the L1 mem cache, ordered from
     * least recently accessed to most recently accessed.
     * 
     * @return snapshot
     */
    public synchronized final Map<String, V> snapshot() {
        return mMemCache.snapshot();
    }

    @Override
    public synchronized final String toString() {
        return mMemCache.toString();
    }

    /**
     * Returns the directory where the disk cache stores its data.
     * 
     * @return directory
     */
    public final File getDirectory() {
        return mDiskCache == null ? null : mDiskCache.getDirectory();
    }

    /**
     * Returns true if the disk cache has been closed.
     * 
     * @return closed
     */
    public final boolean isClosed() {
        return mDiskCache == null ? true : mDiskCache.isClosed();
    }

    /**
     * Force buffered operations to the file system.
     * 
     * @throws IOException
     */
    public synchronized final void flush() throws IOException {
        if (mDiskCache != null) {
            mDiskCache.flush();
        }
    }

    /**
     * Closes the disk cache. Stored values will remain on the file system.
     * 
     * @throws IOException
     */
    public synchronized final void close() throws IOException {
        if (mDiskCache != null) {
            mDiskCache.close();
        }
    }

    /**
     * Convert a byte stream to and from a concrete type.
     * 
     * @param <T>
     *            Object type.
     */
    public static interface Converter<T> {
        /** Converts bytes to an object. */
        T from(byte[] bytes) throws IOException;

        /** Converts o to bytes written to the specified stream. */
        void toStream(T o, OutputStream bytes) throws IOException;
    }

}
