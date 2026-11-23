package ir.mehdieslami.clock;

import android.icu.text.SimpleDateFormat;
import android.icu.util.ULocale;
import java.util.Date;

final class CalendarDates {
    private CalendarDates(){}
    static String format(long now,String pattern,String locale,String zone){
        SimpleDateFormat f=new SimpleDateFormat(pattern,new ULocale(locale));
        f.setTimeZone(zone.isEmpty()?android.icu.util.TimeZone.getDefault():android.icu.util.TimeZone.getTimeZone(zone));
        return f.format(new Date(now));
    }
    static String date(long now,Settings s){
        String day=s.weekday?"EEEE، ":"";
        String fa=format(now,day+"d MMMM 'سال' y","fa_IR@calendar=persian",s.zone);
        String en=format(now,day+"d MMMM y","fa_IR@calendar=gregorian",s.zone);
        return normalizeDigits(s.calendar==0?fa:s.calendar==1?en:fa+"\n"+en,s.digits);
    }
    static String time(long now,Settings s){
        String p=(s.hour24?"HH:mm":"hh:mm")+(s.seconds?":ss":"")+(s.hour24?"":" a");
        return ClockArt.digits(format(now,p,"en_US@calendar=gregorian",s.zone),s.digits);
    }
    static String normalizeDigits(String value,boolean persian){
        StringBuilder b=new StringBuilder();
        for(char ch:value.toCharArray())b.append(ch>='۰'&&ch<='۹'?(char)('0'+ch-'۰'):ch);
        return ClockArt.digits(b.toString(),persian);
    }
    static long nextMidnight(long now,String zone){
        java.util.Calendar c=java.util.Calendar.getInstance(zone.isEmpty()?java.util.TimeZone.getDefault():java.util.TimeZone.getTimeZone(zone));
        c.setTimeInMillis(now);c.add(java.util.Calendar.DAY_OF_YEAR,1);c.set(java.util.Calendar.HOUR_OF_DAY,0);c.set(java.util.Calendar.MINUTE,0);c.set(java.util.Calendar.SECOND,0);c.set(java.util.Calendar.MILLISECOND,0);return c.getTimeInMillis();
    }
}
