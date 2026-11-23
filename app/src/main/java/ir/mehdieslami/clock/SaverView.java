package ir.mehdieslami.clock;

import android.content.Context;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;

/** Shared full-screen content for the system dream and manually opened clock. */
final class SaverView extends FrameLayout {
    private final Settings settings;
    private final ClockView clock;
    private final TextView date;
    private final LinearLayout cluster;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean running;
    private final Runnable tick=new Runnable(){public void run(){
        if(!running)return;
        date.setText(CalendarDates.date(System.currentTimeMillis(),settings));
        if(settings.saverDrift){
            long slot=System.currentTimeMillis()/60000;
            cluster.setTranslationX(dp((int)(slot%5)-2)*3);cluster.setTranslationY(dp((int)((slot/5)%5)-2)*3);
        }
        handler.postDelayed(this,1000);
    }};
    SaverView(Context c,Settings s){
        super(c);settings=s;setBackgroundColor(s.saverNight?Color.BLACK:s.background(c));
        cluster=new LinearLayout(c);cluster.setOrientation(LinearLayout.VERTICAL);cluster.setGravity(Gravity.CENTER_HORIZONTAL);
        cluster.setPadding(dp(18),dp(18),dp(18),dp(18));
        LayoutParams cp=new LayoutParams(-1,-2,Gravity.CENTER);addView(cluster,cp);
        clock=new ClockView(c,s);cluster.addView(clock,new LinearLayout.LayoutParams(-1,dp(300)));
        date=new TextView(c);date.setGravity(Gravity.CENTER);date.setTextDirection(View.TEXT_DIRECTION_RTL);date.setTextSize(18);
        date.setTypeface(ClockArt.font(c,false));date.setTextColor(s.ink(c));date.setLineSpacing(dp(6),1);
        date.setVisibility(s.saverDate?VISIBLE:GONE);cluster.addView(date,new LinearLayout.LayoutParams(-1,-2));
        date.setText(CalendarDates.date(System.currentTimeMillis(),s));
        setContentDescription("ساعت تمام‌صفحه و تاریخ");
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){
        super.onSizeChanged(w,h,oldW,oldH);
        int dateSpace=settings.saverDate?dp(settings.calendar==2?112:76):dp(24);
        int side=Math.max(1,Math.min(w-dp(48),h-dateSpace-dp(60)));
        clock.setLayoutParams(new LinearLayout.LayoutParams(-1,side));
    }
    void start(){if(running)return;running=true;clock.setActive(true);handler.post(tick);}
    void stop(){running=false;clock.setActive(false);handler.removeCallbacks(tick);}
    @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
}
