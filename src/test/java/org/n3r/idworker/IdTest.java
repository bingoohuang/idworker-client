package org.n3r.idworker;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.n3r.idworker.strategy.DefaultWorkerIdStrategy;
import org.n3r.idworker.utils.Ip;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class IdTest {
    @BeforeClass
    public static void beforeClass() {
        String pathname = System.getProperty("user.home") + File.separator + ".idworkers";
        File dir = new File(pathname);
        dir.mkdir();

        for (File f : dir.listFiles()) {
            f.delete();
        }

        String ipdotlock = Ip.ip + "." + System.getProperty("user.name") + ".lock.0112";
        try {
            new File(pathname, ipdotlock).createNewFile();
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
        System.out.println(System.getProperty("user.name"));
        BigInteger bigInteger = new BigInteger("111111111100000000000", 2);
        long workerMask = bigInteger.longValue();

        long id = Id.next(); // eg. 149346876358656
        System.out.println(id);
        assertThat(((workerMask & id) >> 11), is(112L));
    }
}
