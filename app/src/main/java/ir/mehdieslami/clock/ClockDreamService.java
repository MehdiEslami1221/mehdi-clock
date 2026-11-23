package ir.mehdieslami.clock;

import android.service.dreams.DreamService;
import android.view.WindowManager;

public final class ClockDreamService extends DreamService {
    private SaverView content;
    @Override public void onAttachedToWindow(){
        super.onAttachedToWindow();setInteractive(false);setFullscreen(true);setScreenBright(false);
        Settings s=Settings.load(this,-1);
        if(s.saverNight){s.theme=0;s.opacity=0;}
        content=new SaverView(this,s);setContentView(content);
        WindowManager.LayoutParams p=getWindow().getAttributes();p.screenBrightness=s.saverBrightness/100f;getWindow().setAttributes(p);
    }
    @Override public void onDreamingStarted(){super.onDreamingStarted();if(content!=null)content.start();}
    @Override public void onDreamingStopped(){if(content!=null)content.stop();super.onDreamingStopped();}
    @Override public void onDetachedFromWindow(){if(content!=null)content.stop();super.onDetachedFromWindow();}
}
