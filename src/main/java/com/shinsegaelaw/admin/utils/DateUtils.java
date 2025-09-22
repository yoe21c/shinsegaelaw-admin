package com.shinsegaelaw.admin.utils;

import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author yoon-yeojin
 */
@Slf4j
public class DateUtils {

    public static final String DF_API_TIMESTAMP = "yyyyMMddHHmmss";

    public static final String DF_IMAGE_POSTFIX_TIMESTAMP = "yyyyMMddHHmmssSSSSSS";

    public static final String DF_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ssXXX";
    
    public static final String DF_SIMPLE_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss";
    
    public static final String DF_NORMAL = "yyyy-MM-dd HH:mm:ss";
    
    public static final String DF_ISO_8601_DATE = "yyyy-MM-dd";

    public static final String DF_ISO_8601_SIMPLE_MONTH = "yyyyMM";

    public static final String DF_ISO_8601_YEAR_MONTH = "yyyy-MM";

    public static final String DF_ISO_8601_DAY = "yyyyMMdd";

    public static final String DF_ISO_8601_SHORT = "yyMMdd";

    public static final String DF_ISO_8601_TIME = "HHmmss";

    public static final long LONG_1M = 60 * 1000; // 60s * 1000ms

    /**
     * ISO8601 형식의 날짜/시간 문자열을 얻는다.
     *
     * @return
     */
    public static String datetimeISO8601() {
        return datetimeISO8601(new Date());
    }

    public static String shortDatetimeISO8601() {
        DateFormat dateformater = new SimpleDateFormat(DF_ISO_8601_SHORT);
        return dateformater.format(new Date());
    }

    public static String simpleTimestamp(Date datetime) {
        if (datetime == null) {
            return null;
        }
        DateFormat dateformater = new SimpleDateFormat(DF_IMAGE_POSTFIX_TIMESTAMP);
        return dateformater.format(datetime);
    }

    public static String bankServerSimpleTimestamp(Date datetime) {
        return blockchainServerSimpleTimestamp(datetime);
    }

    public static String blockchainServerSimpleTimestamp(Date datetime) {
        if (datetime == null) {
            return null;
        }
        DateFormat dateformater = new SimpleDateFormat(DF_API_TIMESTAMP);
        return dateformater.format(datetime);
    }

    public static String simpleDateFormat() {
        DateFormat dateformater = new SimpleDateFormat(DF_API_TIMESTAMP);
        return dateformater.format(new Date());
    }

    public static String datetimeISO8601(Date datetime) {

        if (datetime == null) {
            return null;
        }
        DateFormat dateformater = new SimpleDateFormat(DF_ISO_8601);
        return dateformater.format(datetime);
    }

    public static String datetimeISO8601DayNow() {

        Date datetime = new Date();
        if (datetime == null) {
            return null;
        }
        DateFormat dateformater = new SimpleDateFormat(DF_ISO_8601_DAY);
        return dateformater.format(datetime);
    }

    public static String txDate(LocalDateTime localDateTime, LocalDateTime defaultTxDate) {

        Date datetime = Date.from(localDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant());
        if (datetime == null) {
            datetime = Date.from(defaultTxDate.atZone(ZoneId.of("Asia/Seoul")).toInstant());
        }
        DateFormat dateformater = new SimpleDateFormat(DF_ISO_8601_DAY);
        return dateformater.format(datetime);
    }

    public static String datetimeISO8601TimeNow() {

        Date datetime = new Date();
        if (datetime == null) {
            return null;
        }
        DateFormat dateformater = new SimpleDateFormat(DF_ISO_8601_TIME);
        return dateformater.format(datetime);
    }
    
    public static Date datetimeSimpleISO8601(String dateString) {
        
        if (dateString == null) {
            return null;
        }
        DateFormat dateformater = new SimpleDateFormat(DF_SIMPLE_ISO_8601);
        dateformater.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        Date date = null;
        try {
            date = dateformater.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static LocalDateTime from(String dateString) {
    
        if (dateString == null) {
            return null;
        }
        DateFormat dateformater = new SimpleDateFormat(DF_NORMAL);
        dateformater.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        Date date = null;
        try {
            date = dateformater.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());

        return localDateTime;
    }

    public static LocalDateTime datetimeISO8601(String dateString) {

        if (dateString == null) {
            return null;
        }
        DateFormat dateformater = new SimpleDateFormat(DF_SIMPLE_ISO_8601);
        dateformater.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        Date date = null;
        try {
            date = dateformater.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());

        return localDateTime;
    }

    public static boolean isAcceptableDateFormat(String dateString) {

        if (dateString == null) {
            return false;
        }
        DateFormat dateformater = new SimpleDateFormat(DF_NORMAL);
        dateformater.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        Date date = null;
        try {
            date = dateformater.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }

        LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());

        return true;
    }
    
    public static String from(Date datetime) {
        
        if (datetime == null) {
            return null;
        }
        DateFormat dateformater = new SimpleDateFormat(DF_NORMAL);
        return dateformater.format(datetime);
    }

    public static String from(LocalDateTime localDateTime) {
        Date datetime;
        if(localDateTime != null) {
            datetime = Date.from(localDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant());
        }else {
            datetime = new Date();
        }
        DateFormat dateformater = new SimpleDateFormat(DF_NORMAL);
        return dateformater.format(datetime);
    }
    
    public static String dateISO8601(Date datetime) {
        
        if (datetime == null) {
            return null;
        }
        DateFormat dateformater = new SimpleDateFormat(DF_ISO_8601_DATE);
        return dateformater.format(datetime);
    }
    
    public static String datetimeISO89601FromUnixTime(String unixTime) {
        
        if (isBlank(unixTime)) {
            return null;
        }
        Date date = new Date(Long.valueOf(unixTime) * 1000);
        DateFormat dateformater = new SimpleDateFormat(DF_ISO_8601);
        return dateformater.format(date);
    }

    /**
     * yyyyMM
     * @return
     */
    public static String thisMonth() {

        final Calendar cal = Calendar.getInstance();
        Date today = cal.getTime();

        DateFormat dateformater = new SimpleDateFormat(DF_ISO_8601_SIMPLE_MONTH);
        return dateformater.format(today);
    }

    /**
     * yyyyMM
     * @return
     */
    public static String nextMonth() {

        LocalDate firstDayOfNextMonth = LocalDate.now().plusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
        Date nextMonth = Date.from(firstDayOfNextMonth.atStartOfDay().atZone(ZoneId.of("Asia/Seoul")).toInstant());

        DateFormat dateformater = new SimpleDateFormat(DF_ISO_8601_SIMPLE_MONTH);
        return dateformater.format(nextMonth);
    }

    /**
     * yyyy-MM
     * @return
     */
    public static String yearMonth(int plusMonths) {

        LocalDate firstDayOfNextMonth = LocalDate.now().plusMonths(plusMonths).with(TemporalAdjusters.firstDayOfMonth());
        Date nextMonth = Date.from(firstDayOfNextMonth.atStartOfDay().atZone(ZoneId.of("Asia/Seoul")).toInstant());

        DateFormat dateformater = new SimpleDateFormat(DF_ISO_8601_YEAR_MONTH);
        return dateformater.format(nextMonth);
    }

    public static LocalDateTime convertToInstantToLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Seoul"));
    }

    public static Instant convertToLocalDateTimeToInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.of("Asia/Seoul")).toInstant();
    }

    public static String getTimeDifference(LocalDateTime reservedAt) {
        LocalDateTime now = LocalDateTime.now();

        // 월 차이 계산
        long monthsDifference = ChronoUnit.MONTHS.between(
            now.withDayOfMonth(1),
            reservedAt.withDayOfMonth(1)
        );

        // 나머지 일수와 시간 계산을 위해 월 차이를 뺀 날짜 생성
        LocalDateTime dateAfterMonths = now.plusMonths(monthsDifference);
        Duration duration = Duration.between(dateAfterMonths, reservedAt);

        // 절대값으로 변환하여 계산
        monthsDifference = Math.abs(monthsDifference);
        long totalSeconds = Math.abs(duration.getSeconds());

        // 일, 시간, 분, 초 계산
        long days = totalSeconds / (24 * 3600);
        long hours = (totalSeconds % (24 * 3600)) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        // 남은 시간인지 초과된 시간인지 확인
        boolean isFuture = reservedAt.isAfter(now);

        // 월이 있는 경우와 없는 경우를 구분하여 출력
        if (monthsDifference > 0) {
            return String.format("%s %d달 %d일 %d시간 %d분 %d초",
                isFuture ? "도달까지" : "초과",
                monthsDifference, days, hours, minutes, seconds);
        } else {
            return String.format("%s %d일 %d시간 %d분 %d초",
                isFuture ? "도달까지" : "초과",
                days, hours, minutes, seconds);
        }
    }
}