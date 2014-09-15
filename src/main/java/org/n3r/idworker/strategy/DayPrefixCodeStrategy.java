package org.n3r.idworker.strategy;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DayPrefixCodeStrategy extends DefaultCodeStrategy {
    private final String dayFormat;
    private String lastDate;

    public DayPrefixCodeStrategy(String dayFormat) {
        this.dayFormat = dayFormat;
    }

    @Override
    public void init() {
        availableCodes.clear();
        release();

        lastDate = createDate();
        prefixIndex = Integer.parseInt(lastDate);
        if (!tryUsePrefix(prefixIndex)) {
            throw new RuntimeException("prefix is not available " + prefixIndex);
        }
    }

    private String createDate() {
        return new SimpleDateFormat(dayFormat).format(new Date());
    }

    @Override
    public int nextRandomCode() {
        if (!lastDate.equals(createDate())) init();

        return super.nextRandomCode();
    }
}
