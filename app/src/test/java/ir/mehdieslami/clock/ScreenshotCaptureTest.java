package ir.mehdieslami.clock;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.io.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

/** Captures the app's actual Activity views using native Android graphics. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ScreenshotCaptureTest {
    @Test public void captureReadmeScreenshots() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        Settings settings=new Settings();settings.zone="Asia/Tehran";settings.saverNight=true;settings.save(context,-1);
        var main=Robolectric.buildActivity(MainActivity.class).setup().visible();
        capture(main,"settings.png");main.pause().stop().destroy();
        settings.readablePreset();settings.save(context,-1);
        var full=Robolectric.buildActivity(FullScreenClockActivity.class).setup().visible();
        capture(full,"fullscreen.png");full.pause().stop().destroy();
    }
    private static void capture(ActivityController<? extends Activity> activity,String filename) throws IOException {
        View decor=activity.get().getWindow().getDecorView();
        int width=1080,height=2340;
        decor.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
        decor.layout(0,0,width,height);decor.invalidate();
        Bitmap screenshot=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);decor.draw(new Canvas(screenshot));
        File directory=new File("build/reports/screenshots");assertTrue(directory.exists()||directory.mkdirs());
        try(FileOutputStream out=new FileOutputStream(new File(directory,filename))){assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG,100,out));}
        screenshot.recycle();
    }
}
