package ir.mehdieslami.clock;

import android.content.SharedPreferences;
import android.graphics.*;
import android.os.*;
import android.service.wallpaper.WallpaperService;
import android.text.*;
import android.view.*;

/** Optional live wallpaper. The system chooser controls home/lock-screen availability. */
public final class ClockWallpaperService extends WallpaperService {
    @Override public Engine onCreateEngine(){return new ClockEngine();}
    private final class ClockEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {
        private final Handler handler=new Handler(Looper.getMainLooper());
        private final Runnable render=this::draw;
        private boolean visible;
        private Settings settings;
        private ClockView clock;
        private SharedPreferences preferences;
        @Override public void onCreate(SurfaceHolder holder){
            super.onCreate(holder);preferences=getSharedPreferences("clocks",0);preferences.registerOnSharedPreferenceChangeListener(this);reload();setTouchEventsEnabled(false);
        }
        private void reload(){settings=Settings.load(ClockWallpaperService.this,-1);clock=new ClockView(ClockWallpaperService.this,settings);clock.setActive(false);}
        @Override public void onSharedPreferenceChanged(SharedPreferences p,String key){
            if(key.startsWith("default.")){reload();if(visible){handler.removeCallbacks(render);handler.post(render);}}
        }
        @Override public void onVisibilityChanged(boolean value){visible=value;handler.removeCallbacks(render);if(value){reload();handler.post(render);}}
        @Override public void onSurfaceChanged(SurfaceHolder h,int format,int width,int height){super.onSurfaceChanged(h,format,width,height);if(visible){handler.removeCallbacks(render);handler.post(render);}}
        private void draw(){
            if(!visible)return;Canvas canvas=null;
            try{
                canvas=getSurfaceHolder().lockCanvas();if(canvas==null)return;
                canvas.drawColor(settings.background(ClockWallpaperService.this));
                int w=canvas.getWidth(),h=canvas.getHeight(),side=(int)Math.min(w*.9,h*.62);
                int x=(w-side)/2,y=(h-side)/2-(settings.saverDate?(int)(h*.04):0);
                if(settings.saverDrift){long slot=System.currentTimeMillis()/60000;x+=(int)(slot%5)-2;y+=(int)((slot/5)%5)-2;}
                clock.layout(0,0,side,side);canvas.save();canvas.translate(x,y);clock.draw(canvas);canvas.restore();
                if(settings.saverDate){
                    TextPaint p=new TextPaint(Paint.ANTI_ALIAS_FLAG);p.setTypeface(ClockArt.font(ClockWallpaperService.this,false));p.setColor(settings.ink(ClockWallpaperService.this));p.setTextSize(Math.max(16,w/26f));
                    String value=CalendarDates.date(System.currentTimeMillis(),settings);int width=(int)(w*.9);
                    StaticLayout date=StaticLayout.Builder.obtain(value,0,value.length(),p,width).setAlignment(Layout.Alignment.ALIGN_CENTER).setTextDirection(TextDirectionHeuristics.RTL).setIncludePad(false).build();
                    canvas.save();canvas.translate((w-width)/2f,y+side+12);date.draw(canvas);canvas.restore();
                }
            }catch(IllegalArgumentException|IllegalStateException unavailable){
                // A surface can disappear between visibility and rendering callbacks.
            }finally{if(canvas!=null)getSurfaceHolder().unlockCanvasAndPost(canvas);if(visible)handler.postDelayed(render,1000-System.currentTimeMillis()%1000);}
        }
        @Override public void onSurfaceDestroyed(SurfaceHolder h){visible=false;handler.removeCallbacks(render);super.onSurfaceDestroyed(h);}
        @Override public void onDestroy(){visible=false;handler.removeCallbacks(render);if(preferences!=null)preferences.unregisterOnSharedPreferenceChangeListener(this);super.onDestroy();}
    }
}
