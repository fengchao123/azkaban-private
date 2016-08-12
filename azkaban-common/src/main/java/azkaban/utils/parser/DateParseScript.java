package azkaban.utils.parser;

import groovy.lang.Script;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by janpychou on 1:13 PM.
 * Mail: janpychou@qq.com
 */


public class DateParseScript extends Script {

    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

    @Override
    public Object run() {
        //show usage
        Method[] methods = DateParseScript.class.getDeclaredMethods();
        StringBuilder sb = new StringBuilder();
        for (Method method : methods) {
            sb.append(method);
        }

        System.out.println(sb);
        return sb.substring(0, sb.length() - 1);
    }

    public static Object nvl(Object str, Object val) {
        return str == null || "".equals(str) ? val : str;
    }

    public static String nowDate() {
        return dateFormat(new Date(), DEFAULT_DATE_FORMAT);
    }

    public static String nowDate(int inc) {
        return nowDate(inc, DEFAULT_DATE_FORMAT);
    }

    public static String nowDate(String formatStr) {
        return dateFormat(new Date(), formatStr);
    }

    public static String nowDate(int inc, String formatStr) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, inc);
        return dateFormat(cal.getTime(), formatStr);
    }

    public static String nowDate(int inc, String unit, String formatStr) {
        Calendar cal = Calendar.getInstance();
        unit = unit.toLowerCase().trim();
        int field = Calendar.DATE;
        if ("month".equals(unit)) {
            field = Calendar.MONTH;
        } else if ("hour".equals(unit)) {
            field = Calendar.HOUR;
        } else if ("minute".equals(unit)) {
            field = Calendar.MINUTE;
        }
        cal.add(field, inc);
        return dateFormat(cal.getTime(), formatStr);
    }

    private static String dateFormat(Date time, String formatStr) {
        SimpleDateFormat format = new SimpleDateFormat(formatStr);
        String formatedStr = format.format(time);
        return formatedStr;
    }


}