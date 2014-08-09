package org.n3r.idworker;

import org.junit.Test;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class IdWorkerTest {
    @Test
    public void generateAnId() {
        IdWorker idWorker = new IdWorker(1);
        assertTrue(idWorker.nextId() > 0);
    }

    // properly mask worker id
    @Test
    public void testWorkerId() {
        BigInteger maxWorker = new BigInteger("1111111111", 2);
        Random random = new Random();
        long randomWorkerId = random.nextInt(maxWorker.intValue());

        IdWorker idWorker = new IdWorker(randomWorkerId);
        BigInteger bigInteger = new BigInteger("111111111100000000000", 2);
        long workerMask = bigInteger.longValue();

        for (int i = 0; i < 1000; ++i)
            assertThat((idWorker.nextId() & workerMask) >> 11, is(randomWorkerId));
    }

    // properly mask timestamp
    @Test
    public void testEpoch() {
        BigInteger maxWorker = new BigInteger("1111111111", 2);
        Random random = new Random();
        long randomWorkerId = random.nextInt(maxWorker.intValue());

        IdWorker idWorker = new IdWorker(randomWorkerId);

        for (int i = 0; i < 10000; ++i) {
            long nextId = idWorker.nextId();
            long value = idWorker.getLastMillis() - idWorker.getEpoch();
            assertThat(nextId >> 21, is(value));
        }
    }

    // "roll over sequence id"
    @Test
    public void testRollover() {
        // put a zero in the low bit so we can detect overflow from the sequence
        long workerId = 4;
        IdWorker worker = new IdWorker(workerId);
        int startSequence = 0xFFFFFF - 20;
        int endSequence = 0xFFFFFF + 20;
        worker.sequence = startSequence;

        BigInteger bigInteger = new BigInteger("111111111100000000000", 2);
        long workerMask = bigInteger.longValue();

        for (int i = startSequence; i <= endSequence; ++i) {
            long id = worker.nextId();
            assertThat((id & workerMask) >> 11, is(workerId));
        }
    }

    @Test
    public void testIncreasing() {
        BigInteger maxWorker = new BigInteger("1111111111", 2);
        Random random = new Random();
        long randomWorkerId = random.nextInt(maxWorker.intValue());

        IdWorker idWorker = new IdWorker(randomWorkerId);

        long lastId = 0L;
        for (int i = 0; i < 100; ++i) {
            long id = idWorker.nextId();
            assertTrue(id > lastId);
            lastId = id;
        }
    }

    @Test
    public void generate1MillionIdsQuickly() {
        BigInteger maxWorker = new BigInteger("1111111111", 2);
        Random random = new Random();
        long randomWorkerId = random.nextInt(maxWorker.intValue());

        IdWorker idWorker = new IdWorker(randomWorkerId);

        long t1 = System.currentTimeMillis();
        for (int i = 0; i < 1000000; ++i) {
            idWorker.nextId();
        }
        long t2 = System.currentTimeMillis();
        System.out.println(String.format("generated 1000000 ids in %d ms, or %,.0f ids/second", t2 - t1, 1000000000.0 / (t2 - t1)));
    }

    static class EasyTimeWorker extends IdWorker {
        public Callable<Long> timeMaker = new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return System.currentTimeMillis();
            }
        };

        public EasyTimeWorker(long workerId) {
            super(workerId);
        }

        @Override
        protected long millisGen() {
            try {
                return timeMaker.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    static class WakingIdWorker extends EasyTimeWorker {
        public int slept = 0;

        public WakingIdWorker(long workerId) {
            super(workerId);
        }

        @Override
        protected long tilNextMillis(long lastMillis) {
            slept += 1;
            return super.tilNextMillis(lastMillis);
        }
    }

    // sleep if we would rollover twice in the same millisecond
    @Test
    public void sameMilli() {
        WakingIdWorker worker = new WakingIdWorker(1);
        final Iterator<Long> iter = new ArrayList<Long>() {{
            add(2L);
            add(2L);
            add(3L);
        }}.iterator();
        worker.timeMaker = new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                return iter.next();
            }
        };
        worker.sequence = 4095;
        worker.nextId();
        worker.sequence = 4095;
        worker.nextId();
        assertThat(worker.slept, is(1));
    }


    // generate only unique ids
    @Test
    public void onlyUniqueIds() {
        IdWorker worker = new IdWorker(31);
        Set<Long> set = new HashSet<Long>();
        int n = 2000000;
        for (int i = 0; i < 2000000; ++i) {
            long id = worker.nextId();
            if (set.contains(id)) {
                System.out.println(id);
            } else {
                set.add(id);
            }
        }

        assertThat(set.size(), is(n));
    }

    static class StaticTimeWorker extends IdWorker {
        public long time = 1L;

        public StaticTimeWorker(long workerId) {
            super(workerId);
        }

        @Override
        protected long millisGen() {
            return time + epoch;
        }
    }

    // generate only unique ids, even when time goes backwards
    @Test
    public void whenTimeGoesBackward() {
        long sequenceMask = -1L ^ (-1L << 11);
        StaticTimeWorker worker = new StaticTimeWorker(0);

        // reported at https://github.com/twitter/snowflake/issues/6
        // first we generate 2 ids with the same time, so that we get the sequqence to 1
        assertThat(worker.sequence, is(0L));
        assertThat(worker.time, is(1L));

        long id1 = worker.nextId();
        assertThat(id1 >> 21, is(1L));
        assertThat(id1 & sequenceMask, is(0L));

        assertThat(worker.sequence, is(0L));
        assertThat(worker.time, is(1L));
        long id2 = worker.nextId();
        assertThat(id2 >> 21, is(1L));
        assertThat(id2 & sequenceMask, is(1L));

        //then we set the time backwards
        worker.time = 0;
        assertThat(worker.sequence, is(1L));
        try {
            worker.nextId();
            fail();
        } catch (InvalidSystemClock e) {

        }
        assertThat(worker.sequence, is(1L)); // this used to get reset to 0, which would cause conflicts

        worker.time = 1;
        long id3 = worker.nextId();
        assertThat(id3 >> 21, is(1L));
        assertThat(id3 & sequenceMask, is(2L));
    }
}
