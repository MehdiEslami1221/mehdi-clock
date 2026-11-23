package ir.mehdieslami.clock;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.io.FileOutputStream;

/** Device integration checks: framework RemoteViews actions, fonts and saved widget isolation. */
public final class SmokeInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    @Override public void onStart(){
        Bundle report=new Bundle();
        try{
            Context c=getTargetContext();
            Settings a=Settings.load(c,-1);int original=a.accent;
            a.accent=Color.MAGENTA;a.theme=1;a.save(c,31001);
            Settings b=Settings.load(c,-1);b.accent=Color.CYAN;b.theme=0;b.save(c,31002);
            check(Settings.load(c,31001).accent==Color.MAGENTA,"First widget settings lost");
            check(Settings.load(c,31002).accent==Color.CYAN,"Second widget settings lost");
            check(Settings.load(c,-1).accent==original,"Widget changed app defaults");
            runOnMainSync(()->{
                for(int theme=0;theme<2;theme++)for(int face=0;face<3;face++)for(boolean seconds:new boolean[]{true,false}){
                    Settings s=new Settings();s.theme=theme;s.style=face;s.seconds=seconds;s.date=true;s.opacity=70;s.zone="Asia/Tehran";
                    View v=ClockWidget.createViews(c,s,240,31001).apply(c,new FrameLayout(c));
                    check(v.findViewById(R.id.widget_clock) instanceof AnalogClock,"Clock failed to inflate");
                    check(v.findViewById(R.id.widget_date).getVisibility()==View.VISIBLE,"Date hidden");
                    for(int[] size:new int[][]{{300,300},{700,350},{350,700}}){
                        v.measure(View.MeasureSpec.makeMeasureSpec(size[0],View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(size[1],View.MeasureSpec.EXACTLY));
                        v.layout(0,0,size[0],size[1]);Bitmap frame=Bitmap.createBitmap(size[0],size[1],Bitmap.Config.ARGB_8888);v.draw(new Canvas(frame));
                        View clock=v.findViewById(R.id.widget_clock);
                        check(frame.getPixel(clock.getLeft()+clock.getWidth()/2,clock.getTop()+clock.getHeight()/2)!=0,"Empty widget rendering");
                        frame.recycle();
                    }
                }
                check(ClockArt.font(c,false)!=null && ClockArt.font(c,true)!=null,"IRANSansX font missing");
            });
            Settings.delete(c,31001);Settings.delete(c,31002);
            Activity activity=startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            runOnMainSync(()->{
                View decor=activity.getWindow().getDecorView();check(decor.getWidth()>0,"Activity has no content");
                Bitmap image=Bitmap.createBitmap(decor.getWidth(),decor.getHeight(),Bitmap.Config.ARGB_8888);decor.draw(new Canvas(image));
                try(FileOutputStream out=new FileOutputStream(new File(c.getFilesDir(),"smoke-app.png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}catch(Exception e){throw new RuntimeException(e);}
            });
            report.putString("stream","PASS: independent widget preferences, 12 RemoteViews variants across three sizes, both fonts, main activity launch.\n");
            finish(Activity.RESULT_OK,report);
        }catch(Throwable e){report.putString("stream","FAIL: "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,report);}
    }
}
