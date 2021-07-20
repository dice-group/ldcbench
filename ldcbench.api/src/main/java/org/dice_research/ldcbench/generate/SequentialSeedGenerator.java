package org.dice_research.ldcbench.generate;

/**
 * A class that is able to generate a sequence of random seeds given a single
 * seed.
 * 
 * <p>
 * Following the arguments in <a href=
 * "https://www.johndcook.com/blog/2016/01/29/random-number-generator-seed-mistakes/">https://www.johndcook.com/blog/2016/01/29/random-number-generator-seed-mistakes/</a>
 * this class is simply generating a sequence of numbers that are not
 * independent of each other but make sure that the same numbers are not used
 * too often.
 * </p>
 * 
 * <p>
 * <b>Note</b> that when using several instances of this class in parallel, the
 * {@link #SequentialSeedGenerator(long, int, int)} constructor should be used.
 * It takes the id of the generator as well as the number of generators running
 * in parallel. This makes sure that all these generators are creating different
 * sequences of seeds.
 * </p>
 */
public class SequentialSeedGenerator implements SeedGenerator {

    /**
     * The next seed which will be returned by this generator.
     */
    private long nextSeed;
    /**
     * The constant which is added to the last seed to create the new seed.
     */
    private long stepSize;

    /**
     * Constructor if only a single instance is used.
     *
     * @param seed the original seed.
     */
    public SequentialSeedGenerator(long seed) {
        this(seed, 0, 1);
    }

    /**
     * Constructor which should be used if multiple generators are used in parallel.
     *
     * @param seed           the single original seed
     * @param generatorId    id of this generator (&lt; generatorCount)
     * @param generatorCount number of generators used in parallel
     */
    public SequentialSeedGenerator(long seed, int generatorId, int generatorCount) {
        this.stepSize = generatorCount;
        this.nextSeed = seed + generatorId;
    }

    @Override
    public long getNextSeed() {
        synchronized (this) {
            long result = nextSeed;
            nextSeed += stepSize;
            return result;
        }
    }
}
