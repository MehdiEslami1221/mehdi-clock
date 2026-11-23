package ir.mehdieslami.clock;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.time.Duration;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={31,35},qualifiers="w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AccessibilitySettingsTest {
    @Test public void individualSizesAndColorsSurviveSavingAndStayIndependent(){
        Context c=RuntimeEnvironment.getApplication();Settings a=new Settings();a.numbersSize=180;a.logoSize=130;a.handWidth=190;a.handLength=110;a.titleSize=175;a.subtitleSize=160;a.digitalSize=150;a.dateSize=140;a.marksSize=160;
        a.setColor("numbers",0xffd0a8ff);a.setColor("hour",0xff123456);a.save(c,7100);
        Settings b=Settings.load(c,7100);assertEquals(180,b.numbersSize);assertEquals(130,b.logoSize);assertEquals(190,b.handWidth);assertEquals(110,b.handLength);assertEquals(175,b.titleSize);assertEquals(160,b.subtitleSize);assertEquals(150,b.digitalSize);assertEquals(140,b.dateSize);assertEquals(160,b.marksSize);
        assertEquals(0xff123456,b.rawColor(c,"hour"));assertEquals(100,Settings.load(c,-1).numbersSize);
        b.resetColor("hour");b.save(c,7100);assertFalse(Settings.load(c,7100).colors.containsKey("hour"));Settings.delete(c,7100);
    }
    @Test public void textAndGraphicsContrastRemainsReadableForCustomColors(){
        Context c=RuntimeEnvironment.getApplication();
        for(int theme=0;theme<2;theme++)for(int face:new int[]{0xff000000,0xffffffff,0xff707070,0xff15447d})for(int foreground:new int[]{0xffbcf36a,0xff000000,0xffffffff,0xff707070,0xff234567}){
            Settings s=new Settings();s.theme=theme;s.setColor("dial",face);
            for(String role:Settings.COLOR_ROLES){if(role.equals("accent")||role.equals("dial"))continue;s.setColor(role,foreground);double minimum=role.equals("hour")||role.equals("minute")||role.equals("second")||role.equals("marks")?3:4.5;assertTrue(role+" contrast",AccessibleColors.contrast(s.clockColor(c,role),face)>=minimum-.0001);}
        }
        Settings s=new Settings();s.opacity=0;s.readablePreset();assertEquals(100,s.actualOpacity());assertTrue(s.numbersSize>100);assertTrue(s.handWidth>100);
    }
    @Test public void settingsChangesKeepPreviewFocusAndScrollInsteadOfRecreatingPage(){
        var activity=Robolectric.buildActivity(MainActivity.class).setup().visible();View root=activity.get().getWindow().getDecorView();layout(root,1080,2340);
        ClockView clock=find(root,ClockView.class,null);ScrollView scroll=find(root,ScrollView.class,null);EditText input=find(root,EditText.class,"سطر اول");input.requestFocus();scroll.scrollTo(0,800);int y=scroll.getScrollY();input.setText("متن شخصی");
        button(root,"روشن").performClick();View current=activity.get().getWindow().getDecorView();assertSame(root,current);assertSame(clock,find(current,ClockView.class,null));assertSame(input,find(current,EditText.class,"سطر اول"));assertTrue(input.hasFocus());assertEquals(y,scroll.getScrollY());
        Button plus=find(root,Button.class,"افزایش اندازهٔ اعداد ساعت");plus.performClick();Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(250));
        assertEquals(105,Settings.load(activity.get(),-1).numbersSize);assertEquals("متن شخصی",Settings.load(activity.get(),-1).title);assertSame(clock,find(current,ClockView.class,null));
        activity.pause().stop().destroy();
    }
    @Test public void doubledSystemFontKeepsControlsLargeEnoughAndTextUnclipped(){
        RuntimeEnvironment.setFontScale(2);var activity=Robolectric.buildActivity(MainActivity.class).setup().visible();
        View root=activity.get().getWindow().getDecorView();layout(root,1080,2340);float density=activity.get().getResources().getDisplayMetrics().density;
        assertTrue(activity.get().getResources().getConfiguration().fontScale>=1.99);
        checkControls(root,density);Button readability=button(root,"تنظیم پیشنهادی برای خوانایی بیشتر");assertTrue(readability.getTextSize()>=17*density*1.7f);
        readability.performClick();Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(250));assertEquals(145,Settings.load(activity.get(),-1).numbersSize);
        activity.pause().stop().destroy();
    }
    @Test public void colorBoxOffersSwatchesAndOnlyAppliesConfirmedColor(){
        RuntimeEnvironment.setFontScale(2);var owner=Robolectric.buildActivity(MainActivity.class).setup().visible();int[] result={0};
        ColorPickerDialog picker=new ColorPickerDialog(owner.get(),new Settings(),"اعداد ساعت",Color.RED,v->result[0]=v,()->{});picker.show();View decor=picker.getWindow().getDecorView();layout(decor,980,2100);
        Button blue=find(decor,Button.class,"انتخاب رنگ #039BE5");assertNotNull(blue);blue.performClick();assertEquals(0,result[0]);button(decor,"انتخاب دقیق رنگ").performClick();layout(decor,980,2100);checkControls(decor,owner.get().getResources().getDisplayMetrics().density);picker.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertEquals(0xff039be5,result[0]);owner.pause().stop().destroy();
    }
    @Test public void enlargedRemoteClockAndTextApplyOnBothAndroidVersions(){
        Context c=RuntimeEnvironment.getApplication();Settings s=new Settings();s.readablePreset();s.numbersSize=200;s.logoSize=150;s.titleSize=200;s.subtitleSize=200;s.digitalSize=180;s.dateSize=180;
        for(int mode=0;mode<3;mode++){
            s.clockType=mode;View view=ClockWidget.createViews(c,s,900,7200).apply(c,new FrameLayout(c));layout(view,1200,1400);Bitmap bitmap=Bitmap.createBitmap(1200,1400,Bitmap.Config.ARGB_8888);view.draw(new Canvas(bitmap));bitmap.recycle();
            ClockView preview=new ClockView(c,s);preview.setActive(false);layout(preview,1600,1600);Bitmap frame=Bitmap.createBitmap(1600,1600,Bitmap.Config.ARGB_8888);preview.draw(new Canvas(frame));frame.recycle();
        }
        var full=Robolectric.buildActivity(FullScreenClockActivity.class).setup();assertNotNull(button(full.get().getWindow().getDecorView(),"خروج از ساعت تمام‌صفحه"));full.pause().stop().destroy();
    }
    private static void layout(View v,int w,int h){v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));v.layout(0,0,w,h);}
    private static void checkControls(View view,float density){
        if(view.getVisibility()!=View.VISIBLE)return;
        if(view instanceof Button||view instanceof Switch||view instanceof SeekBar||view instanceof Spinner||view instanceof EditText){assertTrue(view.getClass()+" height "+view.getMeasuredHeight(),view.getMeasuredHeight()>=48*density-1);assertTrue("touch width",view.getMeasuredWidth()>=48*density-1);}
        if(view instanceof Button){TextView t=(TextView)view;if(t.getLayout()!=null)assertTrue("clipped button: "+t.getText(),t.getLayout().getHeight()<=t.getHeight()-t.getCompoundPaddingTop()-t.getCompoundPaddingBottom()+3);}
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)checkControls(group.getChildAt(i),density);}
    }
    private static Button button(View root,String label){Button b=findText(root,label);assertNotNull(label,b);return b;}
    private static Button findText(View v,String text){if(v instanceof Button&&((Button)v).getText().toString().equals(text))return (Button)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){Button b=findText(g.getChildAt(i),text);if(b!=null)return b;}}return null;}
    private static <T extends View>T find(View v,Class<T> type,String description){if(type.isInstance(v)&&(description==null||description.contentEquals(v.getContentDescription()==null?"":v.getContentDescription())))return type.cast(v);if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){T result=find(g.getChildAt(i),type,description);if(result!=null)return result;}}return null;}
}
