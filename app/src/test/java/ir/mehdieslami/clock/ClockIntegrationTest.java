package ir.mehdieslami.clock;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.time.Instant;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},qualifiers="w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ClockIntegrationTest {
    private Context context;
    @Before public void setup(){context=RuntimeEnvironment.getApplication();}
    @Test public void persianCalendarMatchesRequestedDateAndTehranRollover(){
        Settings s=new Settings();s.zone="Asia/Tehran";
        long monday=Instant.parse("2026-11-23T10:00:00Z").toEpochMilli();
        String date=CalendarDates.date(monday,s);
        assertTrue(date,date.contains("دوشنبه"));assertTrue(date,date.contains("۲ آذر سال ۱۴۰۵"));
        s.digits=false;assertTrue(CalendarDates.date(monday,s).contains("2 آذر سال 1405"));
        s.weekday=false;assertFalse(CalendarDates.date(monday,s).contains("دوشنبه"));
        long next=CalendarDates.nextMidnight(monday,s.zone);
        assertEquals(Instant.parse("2026-11-23T20:30:00Z").toEpochMilli(),next);
        s.weekday=true;assertTrue(CalendarDates.date(next+1000,s).contains("سه‌شنبه")||CalendarDates.date(next+1000,s).contains("سه"));
        s.calendar=2;assertTrue(CalendarDates.date(monday,s).contains("\n"));
    }
    @Test public void customizationStaysIndependentAcrossWidgets(){
        Settings defaults=Settings.load(context,-1);
        Settings a=new Settings();a.title="ساعت من";a.subtitle="";a.clockType=2;a.save(context,7001);
        Settings b=new Settings();b.title="Work";b.calendar=1;b.save(context,7002);
        assertEquals("ساعت من",Settings.load(context,7001).title);assertEquals("",Settings.load(context,7001).subtitle);
        assertEquals("Work",Settings.load(context,7002).title);assertEquals(defaults.title,Settings.load(context,-1).title);
        assertTrue(Settings.load(context,7001).logo);
        Settings.delete(context,7001);Settings.delete(context,7002);
    }
    @Test public void remoteViewsApplyAllClockModesAndDateUsesPersianCalendar(){
        for(int mode=0;mode<3;mode++)for(int theme=0;theme<2;theme++)for(int style=0;style<3;style++){
            Settings s=new Settings();s.clockType=mode;s.theme=theme;s.style=style;s.title="متن دلخواه";s.date=true;s.zone="Asia/Tehran";
            View root=ClockWidget.createViews(context,s,800,7003).apply(context,new FrameLayout(context));
            assertEquals(mode==1?View.GONE:View.VISIBLE,root.findViewById(R.id.widget_clock).getVisibility());
            assertEquals(mode==1?View.VISIBLE:View.GONE,root.findViewById(R.id.widget_digital).getVisibility());
            assertEquals("متن دلخواه",((TextView)root.findViewById(R.id.widget_title)).getText().toString());
            assertTrue(((TextView)root.findViewById(R.id.widget_date)).getText().toString().contains("سال"));
            root.measure(View.MeasureSpec.makeMeasureSpec(1200,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1400,View.MeasureSpec.EXACTLY));root.layout(0,0,1200,1400);
            Bitmap frame=Bitmap.createBitmap(1200,1400,Bitmap.Config.ARGB_8888);root.draw(new Canvas(frame));frame.recycle();
        }
    }
    @Test public void sharpLargeRenderingAndNarrowHandsRespectImageBudget(){
        Settings s=new Settings();s.title="ساعت مهدی";
        Bitmap dial=ClockArt.dial(context,s,1600);assertEquals(1600,dial.getWidth());
        int memory=dial.getAllocationByteCount();
        for(int i=0;i<3;i++){Bitmap hand=ClockArt.hand(context,s,1600,i);assertTrue(hand.getWidth()<120);assertEquals(1600,hand.getHeight());memory+=hand.getAllocationByteCount();hand.recycle();}
        assertTrue(memory<1600*1600*4*1.25);dial.recycle();
        for(int mode=0;mode<3;mode++){
            s.clockType=mode;ClockView v=new ClockView(context,s);v.setActive(false);v.layout(0,0,1800,1800);
            Bitmap frame=Bitmap.createBitmap(1800,1800,Bitmap.Config.ARGB_8888);v.draw(new Canvas(frame));
            if(RuntimeEnvironment.getApiLevel()==35){
                java.io.File dir=new java.io.File("build/reports/clock-previews");dir.mkdirs();
                try(var out=new java.io.FileOutputStream(new java.io.File(dir,"clock-mode-"+mode+".png"))){frame.compress(Bitmap.CompressFormat.PNG,100,out);}catch(java.io.IOException e){throw new RuntimeException(e);}
            }
            frame.recycle();
        }
    }
    @Test public void mainAndFullscreenActivitiesLaunchWithScreenKeptOn(){
        var main=Robolectric.buildActivity(MainActivity.class).setup();assertNotNull(main.get().getWindow().getDecorView());main.pause().stop().destroy();
        var full=Robolectric.buildActivity(FullScreenClockActivity.class).setup();
        assertTrue((full.get().getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)!=0);
        full.pause().stop().destroy();
    }
    @Test public void systemServicesRequireTheirPlatformBindingPermissions() throws Exception {
        var pm=context.getPackageManager();
        assertEquals("android.permission.BIND_DREAM_SERVICE",pm.getServiceInfo(new android.content.ComponentName(context,ClockDreamService.class),0).permission);
        assertEquals("android.permission.BIND_WALLPAPER",pm.getServiceInfo(new android.content.ComponentName(context,ClockWallpaperService.class),0).permission);
    }
    @Test public void launcherAndWidgetConfigurationExposeTheRightControls(){
        Settings legacy=new Settings();legacy.title="My saved clock";legacy.save(context,0);
        var app=Robolectric.buildActivity(MainActivity.class).setup();
        View root=app.get().getWindow().getDecorView();
        assertTrue(hasText(root,"ساعت مهدی"));assertTrue(hasText(root,"افزودن ویجت به صفحهٔ اصلی"));assertTrue(hasText(root,"اجرای ساعت تمام‌صفحه"));
        assertFalse(hasText(root,"ذخیرهٔ تنظیمات ویجت"));assertEquals("My saved clock",Settings.load(context,-1).title);
        app.pause().stop().destroy();
        android.content.Intent configure=new android.content.Intent(context,MainActivity.class).putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,42);
        var widget=Robolectric.buildActivity(MainActivity.class,configure).setup();
        View widgetRoot=widget.get().getWindow().getDecorView();
        assertTrue(hasText(widgetRoot,"تنظیم ویجت"));assertTrue(hasText(widgetRoot,"ذخیرهٔ تنظیمات ویجت"));assertFalse(hasText(widgetRoot,"افزودن ویجت به صفحهٔ اصلی"));
        widget.pause().stop().destroy();
    }
    private static boolean hasText(View view,String expected){
        if(view instanceof TextView && ((TextView)view).getText().toString().equals(expected))return true;
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)if(hasText(g.getChildAt(i),expected))return true;}
        return false;
    }
}
