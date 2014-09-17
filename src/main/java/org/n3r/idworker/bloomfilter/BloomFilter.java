package org.n3r.idworker.bloomfilter;


import java.io.Serializable;

/**
 * A probabilistic "shadow" of a set of elements, useful when the set itself
 * would be too expensive to maintain in memory and query directly. A Bloom
 * filter can give false positives, but never false negatives. That is, adding
 * an element to the filter guarantees that {@link #contains} will return
 * {@code true}, but {@link #contains} returning {@code true} does not
 * guarantee that this element was ever actually added to the filter.
 *
 * @author Laurent Pellegrino
 *
 * @version $Id$
 */
public abstract class BloomFilter<E> implements Serializable {

    private static final long serialVersionUID = 1L;

    // the number of elements this filter can support
    // without to transcend the falsePositiveProbability
    protected int capacity;

    // the maximum false positives probability allowed
    protected final double falsePositiveProbability;

    public BloomFilter(int capacity, double falsePositiveProbability) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be strict positive");
        }

        if (falsePositiveProbability <= 0
                || falsePositiveProbability >= 1) {
            throw new IllegalArgumentException("false positive probability must be in ]0;1[");
        }

        this.capacity = capacity;
        this.falsePositiveProbability = falsePositiveProbability;
    }

    /**
     * Adds the specified element to the bloom filter. If the specified
     * {@code elt} already exists in this filter it will return
     * {@code true}, otherwise {@code false}.
     *
     * @param elt
     *            the element to add to the bloom filter.
     *
     * @return {@code true} if the element to add is already contained,
     *         otherwise {@code false}.
     */
    public abstract boolean add(E elt);

    /**
     * Returns {@code true} if it is <i>possible</i> (probability nonzero) that
     * {@code elt} is contained in the set represented by this Bloom filter. If
     * this method returns {@code false}, this element is <i>definitely</i> not
     * present. If it {@code true}, the probability that this element has
     * <i>not</i> actually been added is given by
     * {@link #getFalsePositiveProbability()}.
     *
     * @param elt
     *            the element to check in the Bloom filter.
     *
     * @return {@code true} if it is <i>possible</i> (probability nonzero) that
     *         {@code elt} is contained in the set represented by this Bloom
     *         filter. If this method returns {@code false}, this element is
     *         <i>definitely</i> not present. If it {@code true}, the
     *         probability that this element has <i>not</i> actually been added
     *         is given by {@link #getFalsePositiveProbability()}.
     */
    public abstract boolean contains(E elt);

    /**
     * Returns the number of unique elements which have been added to the bloom
     * filter.
     *
     * @return the number of unique elements which have been added to the bloom
     *         filter.
     */
    public abstract int size();

    /**
     * Returns the capacity of the Bloom filter (i.e. the maximum number of
     * elements the Bloom filter can store without exceed the false positive
     * probability).
     *
     * @return the capacity of the Bloom filter (i.e. the maximum number of
     *         elements the Bloom filter can store without exceed the false
     *         positive probability).
     */
    public int getCapacity() {
        return this.capacity;
    }

    /**
     * Returns the probability that {@link SlicedBloomFilter#contains(String)} will
     * return {@code true} for an element not actually contained in this set.
     *
     * @return the probability that {@link #contains(String)} will return
     *         {@code true} for an element not actually contained in this set.
     */
    public double getFalsePositiveProbability() {
        return this.falsePositiveProbability;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuffer result = new StringBuffer(this.getClass().getSimpleName());
        result.append("[capacity=");
        result.append(this.getCapacity());
        result.append(", falsePositiveProbability=");
        result.append(this.falsePositiveProbability);
        result.append(", size=");
        result.append(this.size());
        result.append("]");
        return result.toString();
    }

}
