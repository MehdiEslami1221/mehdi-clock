package ir.mehdieslami.clock;

import android.content.Context;
import android.graphics.*;
import android.text.*;
import java.util.Calendar;
import java.util.TimeZone;

final class ClockArt {
    private ClockArt() {}
    static String digits(String value,boolean persian) {
        if(!persian)return value;
        StringBuilder b=new StringBuilder();
        for(char c:value.toCharArray()) b.append(c>='0'&&c<='9'?(char)('۰'+c-'0'):c);
        return b.toString();
    }
    static Typeface font(Context c,boolean bold) {return c.getResources().getFont(bold?R.font.iran_sans_bold:R.font.iran_sans);}
    static void logo(Canvas c,float x,float y,float width,int ink) {
        c.save();c.translate(x,y);c.scale(width/136f,width/136f);
        polygon(c,ink,new float[]{8,12,20,12,52,44,84,12,108,12,52,68,24,40,24,88,20,88,8,76});
        polygon(c,Color.rgb(188,243,106),new float[]{108,12,124,12,100,36,84,36});
        polygon(c,Color.rgb(188,243,106),new float[]{84,44,128,44,112,60,88,60,76,72,124,72,108,88,40,88});c.restore();
    }
    private static void polygon(Canvas c,int color,float[] pts) {
        Path path=new Path();path.moveTo(pts[0],pts[1]);
        for(int i=2;i<pts.length;i+=2)path.lineTo(pts[i],pts[i+1]);path.close();
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);c.drawPath(path,p);
    }
    static Bitmap bitmap(int size) {return Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);}
    static void prepare(Canvas c,int size,Settings s) {
        c.scale(size/512f,size/512f);c.translate(256,256);c.scale(s.scale/100f,s.scale/100f);c.translate(-256,-256);
    }
    static Bitmap dial(Context context,Settings s,int size) {
        Bitmap b=bitmap(size);Canvas c=new Canvas(b);drawDial(context,s,c,size);return b;
    }
    static void drawDial(Context context,Settings s,Canvas c,int size) {
        c.save();prepare(c,size,s);
        Paint p=new Paint(3);int ink=s.ink(context), muted=s.muted(context);
        p.setColor(s.surface(context));p.setAlpha(Math.round(s.opacity*2.55f));c.drawCircle(256,256,244,p);
        p.setAlpha(255);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);
        p.setColor(context.getResources().getColor(android.R.color.transparent,null));
        p.setColor((ink & 0xffffff)|0x22000000);c.drawCircle(256,256,243,p);
        if(s.style==2){p.setColor(s.accent);p.setAlpha(95);p.setStrokeWidth(2);c.drawCircle(256,256,222,p);p.setAlpha(255);}
        p.setStyle(Paint.Style.FILL);p.setStrokeCap(Paint.Cap.ROUND);
        for(int i=0;i<60;i++) {
            if(s.style==1 && i%5!=0)continue;
            c.save();c.rotate(i*6,256,256);p.setColor(i%5==0?s.accent:muted);
            p.setStrokeWidth(i%5==0?4:1.5f);c.drawLine(256,37,256,i%5==0?51:44,p);c.restore();
        }
        p.setTypeface(font(context,true));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(29);p.setColor(ink);
        for(int i=1;i<=12;i++) {
            if(s.style==1 && i%3!=0) continue;
            double a=Math.toRadians(i*30-90);float x=256+(float)Math.cos(a)*182;
            float y=256+(float)Math.sin(a)*182-(p.ascent()+p.descent())/2;
            c.drawText(digits(Integer.toString(i),s.digits),x,y,p);
        }
        logo(c,218,157,76,ink);
        p.setTypeface(font(context,false));p.setTextSize(17);p.setColor(muted);
        label(context,c,s.title,316,320,17,muted);
        label(context,c,s.subtitle,316,347,13,muted);c.restore();
    }
    static Bitmap hand(Context context,Settings s,int size,int kind) {
        int width=Math.max(4,(int)Math.ceil(size*36f/512));
        Bitmap b=Bitmap.createBitmap(width,size,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);
        c.translate(-(size-width)/2f,0);prepare(c,size,s);drawHand(context,s,c,kind);return b;
    }
    static void drawHand(Context context,Settings s,Canvas c,int kind) {
        Paint p=new Paint(3);p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(kind==0?s.ink(context):s.accent);p.setStrokeWidth(kind==0?13:kind==1?8:2.5f);
        c.drawLine(256,kind==2?290:274,256,kind==0?143:kind==1?91:61,p);
        if(kind==2){p.setColor(s.accent);c.drawCircle(256,256,7,p);p.setColor(s.surface(context));c.drawCircle(256,256,3,p);}
    }
    static void label(Context context,Canvas c,String value,int width,int top,int sp,int color){
        if(value.isEmpty())return;
        TextPaint p=new TextPaint(Paint.ANTI_ALIAS_FLAG);p.setTypeface(font(context,false));p.setTextSize(sp);p.setColor(color);
        StaticLayout line=StaticLayout.Builder.obtain(value,0,value.length(),p,width).setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR).setIncludePad(false).setMaxLines(1).setEllipsize(TextUtils.TruncateAt.END).build();
        c.save();c.translate((512-width)/2f,top);line.draw(c);c.restore();
    }
    static float[] angles(long time,String zone,boolean seconds) {
        Calendar c=Calendar.getInstance(zone.isEmpty()?TimeZone.getDefault():TimeZone.getTimeZone(zone));c.setTimeInMillis(time);
        float sec=seconds?c.get(Calendar.SECOND)+c.get(Calendar.MILLISECOND)/1000f:0;
        float min=c.get(Calendar.MINUTE)+sec/60f;
        return new float[]{c.get(Calendar.HOUR)*30+min/2f,min*6,sec*6};
    }
}
