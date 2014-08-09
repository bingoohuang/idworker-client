package org.n3r.idworker;

import org.junit.Test;
import org.n3r.idworker.utils.IPv4Utils;
import org.n3r.idworker.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BasedIpWorkerIdTest {

    @Test
    public void test1() throws IOException {
        InputStream inputStream = Utils.classResourceToStream("mall2.conf");
        computeUsableWorkerIds(inputStream);
    }

    @Test
    public void test2() throws IOException {
        InputStream inputStream = Utils.classResourceToStream("ecs3.conf");
        computeUsableWorkerIds(inputStream);
    }

    protected void computeUsableWorkerIds(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        String line;

        Set<String> ips = new HashSet<String>();
        Set<Long> availableIds = new HashSet<Long>();
        while ((line = bufferedReader.readLine()) != null) {
            String ip = findIp(line);
            if (ip == null) continue;
            if (ips.contains(ip)) continue;
            ips.add(ip);

            computeWorkerIds(availableIds, ip);
        }
    }

    static long workerIdBits = 10L;
    static long maxWorkerId = -1L ^ (-1L << workerIdBits);

    private void computeWorkerIds(Set<Long> availableIds, String ip) {
        long lip = IPv4Utils.toLong(ip);
        System.out.print(ip);
        System.out.print(',');
        System.out.print(lip);
        System.out.print(',');
        long oid = lip & maxWorkerId;

        long id = oid;
        checkDuplicated(availableIds, id);
        id = id ^ 345L;
        checkDuplicated(availableIds, id);
        id = id ^ 923L;
        checkDuplicated(availableIds, id);
        id = id ^ 832L;
        checkDuplicated(availableIds, id);
        id = id ^ 992L;
        checkDuplicated(availableIds, id);
        System.out.println();
    }

    private void checkDuplicated(Set<Long> availableIds, long id) {
        if (availableIds.contains(id)) {
            System.out.print("duplicated:" + id + ",");
        } else {
            availableIds.add(id);
            System.out.print(id);
            System.out.print(',');
        }

    }

    static Pattern ipv4Pattern = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

    public static String findIp(String line) {
        Matcher matcher = ipv4Pattern.matcher(line);
        if (matcher.find()) return matcher.group();
        return null;
    }
}
