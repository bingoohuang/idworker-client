package org.n3r.idworker;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.n3r.idworker.strategy.DefaultWorkerIdStrategy;
import org.n3r.idworker.utils.Ip;
import org.n3r.idworker.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class IdTest {
    @BeforeClass
    public static void beforeClass() {
        File dir = Utils.createIdWorkerHome();
        for (File f : dir.listFiles()) {
            f.delete();
        }

        String ipdotlock = Ip.ip + "." + System.getProperty("user.name") + ".lock.0112";
        try {
            new File(dir, ipdotlock).createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Id.configure(new DefaultWorkerIdStrategy());
    }

    @AfterClass
    public static void afterClass() {
        String pathname = System.getProperty("user.home") + File.separator + ".idworkers";
        String ipdotlock = Ip.ip  + "." + System.getProperty("user.name") + ".lock.0112";
        new File(pathname, ipdotlock).delete();
        new File(pathname).delete();
    }

    @Test
    public void test() {
        BigInteger bigInteger = new BigInteger("111111111100000000000", 2);
        long workerMask = bigInteger.longValue();

        long id = Id.next(); // eg. 149346876358656
        System.out.println(id);
        assertThat(((workerMask & id) >> 11), is(112L));
    }
}
