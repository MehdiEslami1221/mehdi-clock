package ir.mehdieslami.clock;

import android.content.Context;
import android.graphics.*;
import android.view.View;

final class ClockView extends View {
    private Settings settings;
    private Picture face;
    private final Paint paint=new Paint(3);
    private boolean active=true;
    ClockView(Context context,Settings settings){super(context);setContentDescription("ساعت زنده");setSettings(settings);}
    void setSettings(Settings s){
        settings=s;face=new Picture();Canvas recording=face.beginRecording(512,512);
        ClockArt.drawDial(getContext(),s,recording,512);face.endRecording();invalidate();
    }
    void setActive(boolean value){active=value;if(value)invalidate();}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float size=Math.min(getWidth(),getHeight());if(size<=0)return;
        c.save();c.translate((getWidth()-size)/2,(getHeight()-size)/2);c.scale(size/512,size/512);
        long now=System.currentTimeMillis();
        if(settings.clockType==1){
            c.save();ClockArt.prepare(c,512,settings);
            ClockArt.logo(c,196,100,120,settings.ink(getContext()));
            paint.setTypeface(ClockArt.font(getContext(),true));paint.setTextAlign(Paint.Align.CENTER);paint.setColor(settings.accent);paint.setTextSize(settings.hour24?(settings.seconds?70:92):(settings.seconds?50:70));
            c.drawText(CalendarDates.time(now,settings),256,268,paint);
            ClockArt.label(getContext(),c,settings.title,420,315,23,settings.ink(getContext()));
            ClockArt.label(getContext(),c,settings.subtitle,420,353,18,settings.muted(getContext()));c.restore();
        }else{
            c.drawPicture(face);
            float[] a=ClockArt.angles(now,settings.zone,settings.seconds);
            for(int i=0;i<(settings.seconds?3:2);i++){
                c.save();c.rotate(a[i],256,256);ClockArt.prepare(c,512,settings);ClockArt.drawHand(getContext(),settings,c,i);c.restore();
            }
            if(settings.clockType==2){c.save();ClockArt.prepare(c,512,settings);paint.setTypeface(ClockArt.font(getContext(),true));paint.setTextAlign(Paint.Align.CENTER);paint.setColor(settings.accent);paint.setTextSize(22);c.drawText(CalendarDates.time(now,settings),256,300,paint);c.restore();}
        }
        c.restore();if(active && getWindowVisibility()==VISIBLE && isShown())postInvalidateDelayed(settings.seconds&&settings.clockType!=1?33:1000);
    }
}
