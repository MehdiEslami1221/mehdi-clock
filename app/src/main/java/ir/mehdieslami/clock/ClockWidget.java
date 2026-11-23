package ir.mehdieslami.clock;

import android.app.PendingIntent;
import android.app.AlarmManager;
import android.appwidget.*;
import android.content.*;
import android.graphics.drawable.Icon;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import android.util.TypedValue;

public final class ClockWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)update(c,m,id);schedule(c);}
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){update(c,m,id);}
    @Override public void onDeleted(Context c,int[] ids){for(int id:ids)Settings.delete(c,id);schedule(c);}
    @Override public void onReceive(Context c,Intent intent){
        super.onReceive(c,intent);String a=intent.getAction();
        if(Intent.ACTION_TIME_CHANGED.equals(a)||Intent.ACTION_TIMEZONE_CHANGED.equals(a)||Intent.ACTION_LOCALE_CHANGED.equals(a)
                ||Intent.ACTION_CONFIGURATION_CHANGED.equals(a)||Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)||"ir.mehdieslami.clock.DATE_REFRESH".equals(a))refresh(c);
    }
    static void refresh(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);for(int id:m.getAppWidgetIds(new ComponentName(c,ClockWidget.class)))update(c,m,id);schedule(c);}
    static void update(Context c,AppWidgetManager manager,int id){
        Settings s=Settings.load(c,id);
        Bundle options=manager.getAppWidgetOptions(id);
        int width=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,240);
        int height=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,240);
        android.util.DisplayMetrics dm=c.getResources().getDisplayMetrics();
        // One sharp dial plus three narrow hands stays below the RemoteViews image budget.
        int budget=(int)Math.sqrt((double)dm.widthPixels*dm.heightPixels*1.1);
        int pixels=Math.max(96,Math.min(Math.min(2048,budget),Math.round(Math.min(width,height)*dm.density)));
        manager.updateAppWidget(id,createViews(c,s,pixels,id,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,width)));
    }
    static RemoteViews createViews(Context c,Settings s,int pixels,int id){
        return createViews(c,s,pixels,id,pixels/c.getResources().getDisplayMetrics().density);
    }
    private static RemoteViews createViews(Context c,Settings s,int pixels,int id,float widthDp){
        RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.clock_widget);
        v.setIcon(R.id.widget_clock,"setDial",raster(ClockArt.dial(c,s,pixels)));
        v.setIcon(R.id.widget_clock,"setHourHand",raster(ClockArt.hand(c,s,pixels,0)));
        v.setIcon(R.id.widget_clock,"setMinuteHand",raster(ClockArt.hand(c,s,pixels,1)));
        v.setIcon(R.id.widget_clock,"setSecondHand",s.seconds?raster(ClockArt.hand(c,s,pixels,2)):null);
        v.setString(R.id.widget_clock,"setTimeZone",s.zone.isEmpty()?null:s.zone);
        v.setViewVisibility(R.id.widget_clock,s.clockType==1?View.GONE:View.VISIBLE);
        v.setViewVisibility(R.id.widget_digital,s.clockType==1?View.VISIBLE:View.GONE);
        v.setViewVisibility(R.id.widget_hybrid_time,s.clockType==2?View.VISIBLE:View.GONE);
        for(int clock:new int[]{R.id.widget_digital_time,R.id.widget_hybrid_time}){
            String pattern=(s.hour24?"HH:mm":"hh:mm")+(s.seconds?":ss":"")+(s.hour24?"":" a");
            v.setCharSequence(clock,"setFormat12Hour",pattern);v.setCharSequence(clock,"setFormat24Hour",pattern);
            v.setString(clock,"setTimeZone",s.zone.isEmpty()?null:s.zone);v.setTextColor(clock,s.clockColor(c,"digital"));
        }
        v.setTextViewTextSize(R.id.widget_digital_time,TypedValue.COMPLEX_UNIT_SP,fitDigitalSp(c,s,widthDp));
        v.setTextViewTextSize(R.id.widget_hybrid_time,TypedValue.COMPLEX_UNIT_SP,Math.min(26*s.digitalSize/100f,fitDigitalSp(c,s,widthDp)));
        v.setViewLayoutWidth(R.id.widget_logo,56*s.logoSize/100f,TypedValue.COMPLEX_UNIT_DIP);v.setViewLayoutHeight(R.id.widget_logo,42*s.logoSize/100f,TypedValue.COMPLEX_UNIT_DIP);
        v.setTextViewTextSize(R.id.widget_title,TypedValue.COMPLEX_UNIT_SP,14*s.titleSize/100f);v.setTextViewTextSize(R.id.widget_subtitle,TypedValue.COMPLEX_UNIT_SP,12*s.subtitleSize/100f);
        int face=s.face(c);int faceWithAlpha=(Math.round(s.actualOpacity()*2.55f)<<24)|(face&0xffffff);v.setInt(R.id.widget_digital,"setBackgroundColor",faceWithAlpha);
        v.setTextViewText(R.id.widget_title,s.title);v.setTextViewText(R.id.widget_subtitle,s.subtitle);
        v.setTextColor(R.id.widget_title,s.clockColor(c,"title"));v.setTextColor(R.id.widget_subtitle,s.clockColor(c,"subtitle"));
        v.setTextViewText(R.id.widget_date,CalendarDates.date(System.currentTimeMillis(),s));
        v.setTextViewTextSize(R.id.widget_date,TypedValue.COMPLEX_UNIT_SP,14*s.dateSize/100f);
        v.setViewVisibility(R.id.widget_date,s.date?View.VISIBLE:View.GONE);
        v.setTextColor(R.id.widget_date,s.clockColor(c,"numbers"));v.setInt(R.id.widget_date,"setBackgroundColor",s.highContrast?s.face(c):android.graphics.Color.TRANSPARENT);
        Intent open=new Intent(c,MainActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
        PendingIntent pi=PendingIntent.getActivity(c,id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.widget_root,pi);v.setOnClickPendingIntent(R.id.widget_clock,pi);
        return v;
    }
    static float fitDigitalSp(Context c,Settings s,float widthDp){
        android.util.DisplayMetrics dm=c.getResources().getDisplayMetrics();android.text.TextPaint p=new android.text.TextPaint();p.setTypeface(ClockArt.font(c,true));
        float size=36*s.digitalSize/100f;p.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,size,dm));String sample=s.seconds?"88:88:88":"88:88";if(!s.hour24)sample+=" PM";
        float measured=Math.max(p.measureText(sample),p.measureText(ClockArt.digits(sample,true)));float available=Math.max(24,widthDp-12)*dm.density;
        return size*Math.min(1,available/Math.max(1,measured));
    }
    static void schedule(Context c){
        AppWidgetManager manager=AppWidgetManager.getInstance(c);int[] ids=manager.getAppWidgetIds(new ComponentName(c,ClockWidget.class));
        AlarmManager alarms=c.getSystemService(AlarmManager.class);
        PendingIntent pi=PendingIntent.getBroadcast(c,701,new Intent(c,ClockWidget.class).setAction("ir.mehdieslami.clock.DATE_REFRESH"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(ids.length==0){alarms.cancel(pi);return;}
        long now=System.currentTimeMillis(),next=Long.MAX_VALUE;
        for(int id:ids){Settings s=Settings.load(c,id);if(s.date)next=Math.min(next,CalendarDates.nextMidnight(now,s.zone));}
        if(next==Long.MAX_VALUE){alarms.cancel(pi);return;}
        alarms.setAndAllowWhileIdle(AlarmManager.RTC,next,pi);
    }
    private static Icon raster(Bitmap image){
        // Give the native AnalogClock a large intrinsic size without a large binder payload.
        // It scales down to fit both compact and expanded widget bounds.
        image.setDensity(80);return Icon.createWithBitmap(image);
    }
}
