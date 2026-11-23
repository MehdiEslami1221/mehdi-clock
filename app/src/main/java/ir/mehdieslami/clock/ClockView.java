package ir.mehdieslami.clock;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

final class ClockView extends View {
    private Settings settings;
    private Picture face;
    private final Paint paint=new Paint(3);
    private final Paint handPaint=new Paint(3);
    private long cachedSecond=Long.MIN_VALUE;
    private int timeSignature;
    private String timeText="";
    private boolean active=true;
    ClockView(Context context,Settings settings){super(context);setFocusable(true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);setSettings(settings);}
    void setSettings(Settings s){
        int signature=java.util.Objects.hash(s.zone,s.hour24,s.seconds,s.digits);if(signature!=timeSignature){cachedSecond=Long.MIN_VALUE;timeSignature=signature;}
        settings=s;face=new Picture();Canvas recording=face.beginRecording(512,512);
        ClockArt.drawDial(getContext(),s,recording,512);face.endRecording();invalidate();
    }
    void setActive(boolean value){active=value;if(value)invalidate();}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float size=Math.min(getWidth(),getHeight());if(size<=0)return;
        c.save();c.translate((getWidth()-size)/2,(getHeight()-size)/2);c.scale(size/512,size/512);
        long now=System.currentTimeMillis();
        if(now/1000!=cachedSecond){cachedSecond=now/1000;timeText=CalendarDates.time(now,settings);}
        if(settings.clockType==1){
            c.save();ClockArt.prepare(c,512,settings);
            paint.setColor(settings.face(getContext()));paint.setAlpha(Math.round(settings.actualOpacity()*2.55f));c.drawRoundRect(20,24,492,488,24,24,paint);paint.setAlpha(255);
            float logoWidth=120*settings.logoSize/100f,logoHeight=logoWidth*100/136;
            paint.setTypeface(ClockArt.font(getContext(),true));paint.setTextAlign(Paint.Align.CENTER);paint.setColor(settings.clockColor(getContext(),"digital"));
            float base=settings.hour24?(settings.seconds?70:92):(settings.seconds?50:70);paint.setTextSize(base*settings.digitalSize/100f);
            if(paint.measureText(timeText)>452)paint.setTextSize(paint.getTextSize()*452/paint.measureText(timeText));
            float timeHeight=paint.descent()-paint.ascent();int titleSize=Math.round(23*settings.titleSize/100f),subtitleSize=Math.round(18*settings.subtitleSize/100f);
            float titleHeight=settings.title.isEmpty()?0:titleSize*1.45f,subtitleHeight=settings.subtitle.isEmpty()?0:subtitleSize*1.45f;
            float total=logoHeight+20+timeHeight+18+titleHeight+10+subtitleHeight;
            float fit=Math.min(1,432/total);c.save();c.translate(256,256);c.scale(fit,fit);c.translate(-256,-256);
            float top=(512-total)/2;
            ClockArt.logo(c,256-logoWidth/2,top,logoWidth,AccessibleColors.textOn(settings.face(getContext())));top+=logoHeight+20;
            c.drawText(timeText,256,top-paint.ascent(),paint);top+=timeHeight+18;
            ClockArt.label(getContext(),c,settings.title,420,Math.round(top),titleSize,settings.clockColor(getContext(),"title"));top+=titleHeight+10;
            ClockArt.label(getContext(),c,settings.subtitle,420,Math.round(top),subtitleSize,settings.clockColor(getContext(),"subtitle"));c.restore();c.restore();
        }else{
            c.drawPicture(face);
            float[] a=ClockArt.angles(now,settings.zone,settings.seconds);
            for(int i=0;i<(settings.seconds?3:2);i++){
                c.save();c.rotate(a[i],256,256);ClockArt.prepare(c,512,settings);ClockArt.drawHand(getContext(),settings,c,i,handPaint);c.restore();
            }
            if(settings.clockType==2){c.save();ClockArt.prepare(c,512,settings);paint.setTypeface(ClockArt.font(getContext(),true));paint.setTextAlign(Paint.Align.CENTER);paint.setColor(settings.clockColor(getContext(),"digital"));paint.setTextSize(22*settings.digitalSize/100f);if(paint.measureText(timeText)>316)paint.setTextSize(paint.getTextSize()*316/paint.measureText(timeText));c.drawText(timeText,256,300,paint);c.restore();}
        }
        c.restore();if(active && getWindowVisibility()==VISIBLE && isShown())postInvalidateDelayed(settings.seconds&&settings.clockType!=1?33:1000);
    }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){
        super.onInitializeAccessibilityNodeInfo(info);info.setClassName(android.widget.TextView.class.getName());
        info.setContentDescription("ساعت "+CalendarDates.time(System.currentTimeMillis(),settings)+"، "+CalendarDates.date(System.currentTimeMillis(),settings)+"، "+settings.title+" "+settings.subtitle);
    }
    @Override protected void onDetachedFromWindow(){active=false;super.onDetachedFromWindow();}
}
