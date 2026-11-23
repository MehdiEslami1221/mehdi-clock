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
    private Button exit;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean running;
    private final Runnable tick=new Runnable(){public void run(){
        if(!running)return;
        String value=CalendarDates.date(System.currentTimeMillis(),settings);if(!date.getText().toString().equals(value))date.setText(value);
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
        date=new TextView(c);date.setGravity(Gravity.CENTER);date.setTextDirection(View.TEXT_DIRECTION_RTL);date.setTextSize(18*s.dateSize/100f);
        date.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        date.setTypeface(ClockArt.font(c,false));date.setTextColor(s.ink(c));date.setLineSpacing(dp(6),1);
        date.setVisibility(s.saverDate?VISIBLE:GONE);cluster.addView(date,new LinearLayout.LayoutParams(-1,-2));
        date.setText(CalendarDates.date(System.currentTimeMillis(),s));
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int w=MeasureSpec.getSize(widthSpec),h=MeasureSpec.getSize(heightSpec);
        int width=Math.max(1,w-getPaddingLeft()-getPaddingRight()-dp(48));
        date.measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.AT_MOST),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));
        int exitHeight=0;
        if(exit!=null){exit.measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));exitHeight=exit.getMeasuredHeight()+dp(24);}
        int side=Math.max(1,Math.min(width,h-getPaddingTop()-getPaddingBottom()-exitHeight-(settings.saverDate?date.getMeasuredHeight():0)-dp(48)));
        clock.getLayoutParams().height=side;((LayoutParams)cluster.getLayoutParams()).bottomMargin=exitHeight;
        super.onMeasure(widthSpec,heightSpec);
    }
    void addExitAction(Runnable action){
        exit=new Button(getContext());exit.setText("خروج از ساعت تمام‌صفحه");exit.setTextSize(18);exit.setTypeface(ClockArt.font(getContext(),true));exit.setAllCaps(false);exit.setMinHeight(dp(60));
        exit.setTextColor(AccessibleColors.textOn(settings.accent));exit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(settings.accent));exit.setPadding(dp(12),dp(12),dp(12),dp(12));exit.setOnClickListener(v->action.run());
        LayoutParams p=new LayoutParams(-1,-2,Gravity.BOTTOM);p.setMargins(dp(16),0,dp(16),dp(12));addView(exit,p);
    }
    void start(){if(running)return;running=true;clock.setActive(true);handler.post(tick);}
    void stop(){running=false;clock.setActive(false);handler.removeCallbacks(tick);}
    @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
}
