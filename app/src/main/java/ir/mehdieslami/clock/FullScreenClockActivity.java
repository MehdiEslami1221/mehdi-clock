package ir.mehdieslami.clock;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;

public final class FullScreenClockActivity extends Activity {
    private SaverView content;
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);setShowWhenLocked(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Settings s=Settings.load(this,-1);if(s.saverNight){s.theme=0;s.opacity=0;}
        WindowManager.LayoutParams p=getWindow().getAttributes();p.screenBrightness=s.saverBrightness/100f;getWindow().setAttributes(p);
        content=new SaverView(this,s);setContentView(content);
        content.setOnApplyWindowInsetsListener((v,insets)->{
            android.graphics.Insets cutout=insets.getInsets(WindowInsets.Type.displayCutout());v.setPadding(cutout.left,cutout.top,cutout.right,cutout.bottom);return insets;
        });
        content.post(()->{
            WindowInsetsController controller=getWindow().getInsetsController();
            if(controller!=null){controller.hide(WindowInsets.Type.systemBars());controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}
        });
        // Exit is deliberate: a double tap ends the manual clock without dismissing keyguard.
        android.view.GestureDetector gestures=new android.view.GestureDetector(this,new android.view.GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(android.view.MotionEvent e){return true;}
            @Override public boolean onDoubleTap(android.view.MotionEvent e){finish();return true;}
        });
        content.setOnTouchListener((v,event)->gestures.onTouchEvent(event));
    }
    @Override protected void onResume(){super.onResume();content.start();}
    @Override protected void onPause(){content.stop();super.onPause();}
}
