package org.n3r.idworker.bloomfilter;


import org.n3r.idworker.bloomfilter.hash.Murmur2;

import java.io.UnsupportedEncodingException;
import java.util.BitSet;


/**
 * This bloom filter is a variant of a classical bloom filter as explained in
 * the <a href=
 * "http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.153.6902&rep=rep1&type=pdf"
 * >Approximate caches for packet classification</a>. It consists of
 * partitioning the {@code M} bits among the {@code k} hash functions, thus
 * creating {@code k} slices of {@code m = M / k} bits.
 * <p>
 * Using slices result in a more robust filter, with no element specially
 * sensitive to false positives.
 * <p>
 * This class is <strong>not thread-safe</strong>. Moreover, when an element is
 * added into the Bloom filter, it is based on the uniqueness of this object
 * which is defined by the {@link #hashCode()} method. Therefore it is really
 * important to provide a correct {@link #hashCode()} method for elements which
 * have to be passed to the {@link #add} method.
 *
 * @author Laurent Pellegrino
 *
 * @version $Id$
 */
public class SlicedBloomFilter<E> extends BloomFilter<E> {

    private static final long serialVersionUID = 1L;

    // the number of slices to use (equals to the number
    // of hash function to use)
    private final int slicesCount;

    // the number of bits per slice
    private final int bitsPerSlice;

    // the set containing the values for each slice
    private final BitSet filter;

    // the number of elements added in the Bloom filter
    private int count;

    /**
     * This BloomFilter must be able to store at least {@code capacity} elements
     * while maintaining no more than {@code falsePositiveProbability} chance of
     * false positives.
     *
     * @param capacity
     *            the maximum number of elements the Bloom filter can contain
     *            without to transcend the {@code falsePositiveProbability}.
     *
     * @param falsePositiveProbability
     *            the maximum false positives rate allowed by this filter.
     */
    public SlicedBloomFilter(int capacity, double falsePositiveProbability) {
        super(capacity, falsePositiveProbability);

        if (capacity < 4 / falsePositiveProbability) {
            throw new IllegalArgumentException(
                    "the capacity must be >= inferior than 4 / falsePositiveProbability (rule of thumb).");
        }

        int[] bounds = this.computeBounds(capacity, falsePositiveProbability);

        super.capacity = bounds[2];
        this.slicesCount = bounds[0];
        this.bitsPerSlice = bounds[1];
        this.filter = new BitSet(this.slicesCount * this.bitsPerSlice);
    }

    public SlicedBloomFilter(int capacity, double falsePositiveProbability, int slicesCount) {
        super(capacity, falsePositiveProbability);
        double k = Math.ceil(Math.log(1 / (double) falsePositiveProbability) / Math.log(2));
        double p = Math.pow(falsePositiveProbability, 1 / (double) k);
        int mb = slicesCount;

        int m = 1 << mb;

        int n =
                (int) (Math.log(1 - p) / Math.log(1 - 1 / (double) m));

        super.capacity = (int) n;
        this.slicesCount = slicesCount;
        this.bitsPerSlice = 1 << slicesCount;
        this.filter = new BitSet(this.slicesCount * this.bitsPerSlice);
    }

    private int[] computeBounds(int capacity, double falsePositiveProbability) {
        double k =
                Math.ceil(
                        Math.log(1 / (double) falsePositiveProbability) / Math.log(2));

        double p =
                Math.pow(falsePositiveProbability, 1 / (double) k);

        int mb =
                (int) Math.ceil(
                        - (Math.log(1 - Math.pow(1 - p, 1 / (double) capacity)) / Math.log(2)));

        int m = 1 << mb;
        int n = (int) (Math.log(1 - p) / Math.log(1 - 1 / (double) m));

        return new int[] {mb, m, n};
    }

    /**
     * {@inheritDoc}
     */
    public boolean add(E elt) {
        if (this.contains(elt)) {
            return true;
        }

        this.addWithoutCheck(elt);

        return false;
    }

    /**
     * Adds the specified element without verifying that the element is
     * contained by the Bloom filter. The size of the Bloom filter is
     * incremented by one even if the element is already contained by the
     * filter. Therefore, this method should only be used if you know what you
     * do.
     *
     * @param elt
     *            the element to add to the Bloom filter.
     */
    public void addWithoutCheck(E elt) {
        if (this.isFull()) {
            throw new IllegalStateException("bloom filter is at capacity");
        }

        int[] hashes =
                getHashBuckets(
                        Integer.toString(elt.hashCode()),
                        this.slicesCount, this.bitsPerSlice);

        int offset = 0;
        for (int k : hashes) {
            this.filter.set(offset + k);
            offset += this.bitsPerSlice;
        }

        this.count++;
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(E elt) {
        int[] hashes =
                getHashBuckets(
                        Integer.toString(elt.hashCode()),
                        this.slicesCount, this.bitsPerSlice);

        int offset = 0;
        for (int k : hashes) {
            if (!this.filter.get(offset + k)) {
                return false;
            }
            offset += this.bitsPerSlice;
        }

        return true;
    }

    /**
     * Returns a boolean indicating if the Bloom filter has reached its maximal
     * capacity.
     *
     * @return {@code true} whether the Bloom filter has reached its maximal
     *         capacity, {@code false} otherwise.
     */
    public boolean isFull() {
        return this.count >= this.capacity;
    }

    /**
     * Returns the number of elements added in this Bloom filter.
     *
     * @return the number of elements added in this Bloom filter.
     */
    public int size() {
        return this.count;
    }

    /**
     * Returns {@code hashCount} hashes for the specified {@code key} by
     * using only one hash function. Indeed, we can derive as many hash values
     * as you need as a linear combination of two without compromising the
     * performance of a Bloom filter. This is explained in the paper entitled <a
     * href="http://www.eecs.harvard.edu/~kirsch/pubs/bbbf/esa06.pdf">Less
     * Hashing, Same Performance: Building a Better Bloom Filter</a> by <em>Adam
     * Kirsch</em> and <em>Michael Mitzenmacher</em>.
     * <p>
     * The hash function used for it is the Murmur 2 hash function which is
     * reputed for being really fast.
     *
     * @param key
     *            the value to hash.
     *
     * @param hashCount
     *            the number of hashes wished.
     *
     * @param max
     *            value used to restrict the hash values obtained in the [0;
     *            max[ range.
     *
     * @return {@code hashCount} hashes for the specified {@code key} by
     *         using only one hash function.
     */
    public static int[] getHashBuckets(String key, int hashCount, int max) {
        byte[] b;
        try {
            b = key.getBytes("UTF-16");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        int[] result = new int[hashCount];
        int hash1 = Murmur2.hash32(b, 0);
        int hash2 = Murmur2.hash32(b, hash1);
        for (int i = 0; i < hashCount; i++) {
            result[i] = Math.abs((hash1 + i * hash2) % max);
        }
        return result;
    }

    public int getBitsPerSlice() {
        return bitsPerSlice;
    }

    public int getSlicesCount() {
        return slicesCount;
    }

    @Override
    public String toString() {
        return super.toString() + "[slicesCount=" + this.slicesCount + ", bitsPerSlice=" + this.bitsPerSlice + "]";
    }

}