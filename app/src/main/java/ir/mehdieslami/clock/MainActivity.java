package ir.mehdieslami.clock;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.icu.text.SimpleDateFormat;
import android.icu.util.ULocale;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.Date;
import java.util.TimeZone;
import java.util.function.Consumer;

public final class MainActivity extends Activity {
    private Settings settings;
    private int widgetId=-1;
    private LinearLayout page;
    private ScrollView scroll;
    private ClockView preview;
    private TextView digital,date;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable widgetRefresh=()->ClockWidget.refresh(this);
    private final Runnable ticker=new Runnable(){public void run(){updateTime();handler.postDelayed(this,1000);}};
    private boolean about=false;

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
        if(widgetId==AppWidgetManager.INVALID_APPWIDGET_ID)widgetId=-1;
        if(widgetId<0)Settings.migrateLegacyAppDefaults(this);
        if(widgetId>=0)setResult(RESULT_CANCELED);
        settings=Settings.load(this,widgetId);
        if(saved!=null){settings.theme=saved.getInt("theme");settings.accent=saved.getInt("accent");settings.style=saved.getInt("style");
            settings.scale=saved.getInt("scale");settings.opacity=saved.getInt("opacity");settings.seconds=saved.getBoolean("seconds");
            settings.logo=true;settings.digits=saved.getBoolean("digits");settings.date=saved.getBoolean("date");settings.zone=saved.getString("zone","");about=saved.getBoolean("about");
            settings.title=saved.getString("title",settings.title);settings.subtitle=saved.getString("subtitle",settings.subtitle);
            settings.clockType=saved.getInt("clockType",0);settings.calendar=saved.getInt("calendar",0);settings.weekday=saved.getBoolean("weekday",true);
            settings.hour24=saved.getBoolean("hour24",true);settings.saverDate=saved.getBoolean("saverDate",true);settings.saverNight=saved.getBoolean("saverNight",true);
            settings.saverDrift=saved.getBoolean("saverDrift",true);settings.saverBrightness=saved.getInt("saverBrightness",12);}
        render(false);
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);
        out.putInt("theme",settings.theme);out.putInt("accent",settings.accent);out.putInt("style",settings.style);out.putInt("scale",settings.scale);out.putInt("opacity",settings.opacity);
        out.putBoolean("seconds",settings.seconds);out.putBoolean("digits",settings.digits);out.putBoolean("date",settings.date);out.putString("zone",settings.zone);out.putBoolean("about",about);
        out.putString("title",settings.title);out.putString("subtitle",settings.subtitle);out.putInt("clockType",settings.clockType);out.putInt("calendar",settings.calendar);
        out.putBoolean("weekday",settings.weekday);out.putBoolean("hour24",settings.hour24);out.putBoolean("saverDate",settings.saverDate);out.putBoolean("saverNight",settings.saverNight);
        out.putBoolean("saverDrift",settings.saverDrift);out.putInt("saverBrightness",settings.saverBrightness);}
    @Override protected void onResume(){super.onResume();handler.post(ticker);if(preview!=null)preview.invalidate();}
    @Override protected void onPause(){handler.removeCallbacks(ticker);super.onPause();}
    @Override public void onBackPressed(){if(about){about=false;render(false);}else super.onBackPressed();}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int sp,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(sp);t.setTextColor(settings.ink(this));
        t.setTypeface(ClockArt.font(this,bold));t.setGravity(Gravity.START);t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        t.setLineSpacing(dp(3),1.05f);return t;
    }
    private GradientDrawable background(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private void gap(LinearLayout p,int n){Space v=new Space(this);p.addView(v,new LinearLayout.LayoutParams(1,dp(n)));}
    private LinearLayout card(String title){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(19),dp(18),dp(19),dp(18));
        c.setBackground(background(settings.surface(this),24));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(14);page.addView(c,cp);
        if(title!=null){c.addView(text(title,17,true));gap(c,12);}return c;
    }
    private void note(LinearLayout parent,String value){TextView t=text(value,12,false);t.setTextColor(settings.muted(this));parent.addView(t);}
    private Button button(String label,boolean primary,Runnable action){
        Button b=new Button(this);b.setAllCaps(false);b.setText(label);b.setTextSize(14);b.setTypeface(ClockArt.font(this,true));
        b.setTextColor(primary?(Color.luminance(settings.accent)>0.35?Color.rgb(12,23,11):Color.WHITE):settings.ink(this));
        b.setBackgroundTintList(ColorStateList.valueOf(primary?settings.accent:settings.surface(this)));
        b.setMinHeight(dp(48));b.setOnClickListener(v->action.run());return b;
    }
    private void choices(LinearLayout p,String[] labels,int selected,Consumer<Integer> choose){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER);p.addView(row,new LinearLayout.LayoutParams(-1,-2));
        for(int i=0;i<labels.length;i++){final int n=i;
            Button b=button(labels[i],i==selected,()->{choose.accept(n);changed(true);});b.setTextSize(12);b.setPadding(dp(3),0,dp(3),0);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),1);lp.setMargins(dp(2),0,dp(2),0);row.addView(b,lp);
        }
    }
    private void toggle(LinearLayout p,String title,boolean checked,Consumer<Boolean> update){
        Switch sw=new Switch(this);sw.setText(title);sw.setTextSize(14);sw.setTextColor(settings.ink(this));sw.setTypeface(ClockArt.font(this,false));
        sw.setPadding(0,dp(10),0,dp(10));sw.setChecked(checked);sw.setMinHeight(dp(48));
        sw.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{settings.accent,settings.muted(this)}));
        p.addView(sw,new LinearLayout.LayoutParams(-1,-2));sw.setOnCheckedChangeListener((v,value)->{update.accept(value);changed(false);});
    }
    private void slider(LinearLayout p,String title,int value,int min,int max,Consumer<Integer> set){
        TextView label=text(title+" · "+ClockArt.digits(value+"٪",true),14,false);p.addView(label);
        SeekBar bar=new SeekBar(this);bar.setMin(min);bar.setMax(max);bar.setProgress(value);
        bar.setProgressTintList(ColorStateList.valueOf(settings.accent));bar.setThumbTintList(ColorStateList.valueOf(settings.accent));
        p.addView(bar,new LinearLayout.LayoutParams(-1,dp(48)));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int n,boolean fromUser){if(fromUser){set.accept(n);label.setText(title+" · "+ClockArt.digits(n+"٪",true));preview.setSettings(settings);}}
            public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){changed(false);}
        });
    }
    private void changed(boolean redraw){
        if(widgetId<0){settings.save(this,-1);handler.removeCallbacks(widgetRefresh);handler.postDelayed(widgetRefresh,500);}
        if(redraw)render(true);else{if(preview!=null)preview.setSettings(settings);updateTime();}
    }
    private void render(boolean keepScroll){
        int previous=keepScroll && scroll!=null?scroll.getScrollY():0;
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(settings.background(this));root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
        getWindow().setNavigationBarColor(settings.background(this));getWindow().setStatusBarColor(settings.background(this));
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(20),dp(22),dp(20),dp(16));scroll.addView(page,new ScrollView.LayoutParams(-1,-2));
        setContentView(root);root.requestApplyInsets();
        root.post(()->{
            WindowInsetsController controller=getWindow().getInsetsController();
            if(controller!=null)controller.setSystemBarsAppearance(settings.light(this)?WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS:0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        });
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);page.addView(header,new LinearLayout.LayoutParams(-1,-2));
        TextView title=text(about?"دربارهٔ برنامه":widgetId>=0?"تنظیم ویجت":"ساعت مهدی",26,true);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button info=button(about?"بازگشت":"درباره",false,()->{about=!about;render(false);});header.addView(info,new LinearLayout.LayoutParams(dp(88),dp(48)));gap(page,14);
        if(about){aboutPage();return;}
        note(page,widgetId>=0?"ظاهر این ویجت را به سلیقهٔ خودت تنظیم کن.":"زمان، به سبک خودت.");gap(page,18);
        LinearLayout hero=card(null);hero.setGravity(Gravity.CENTER_HORIZONTAL);
        preview=new ClockView(this,settings);
        int width=getResources().getDisplayMetrics().widthPixels-dp(86);int side=Math.max(180,Math.min(330,Math.round(width/getResources().getDisplayMetrics().density)));
        hero.addView(preview,new LinearLayout.LayoutParams(-1,dp(side)));
        digital=text("",32,true);digital.setGravity(Gravity.CENTER);hero.addView(digital,new LinearLayout.LayoutParams(-1,-2));
        digital.setVisibility(settings.clockType==0?View.VISIBLE:View.GONE);
        date=text("",12,false);date.setGravity(Gravity.CENTER);date.setTextColor(settings.muted(this));hero.addView(date,new LinearLayout.LayoutParams(-1,-2));gap(hero,6);updateTime();
        LinearLayout themes=card("تم و صفحهٔ ساعت");
        choices(themes,new String[]{"عقربه‌ای","دیجیتال","ترکیبی"},settings.clockType,v->settings.clockType=v);gap(themes,12);
        choices(themes,new String[]{"تیره","روشن"},settings.theme,v->settings.theme=v);gap(themes,12);
        if(settings.clockType!=1)choices(themes,new String[]{"کلاسیک","ساده","مداری"},settings.style,v->settings.style=v);
        LinearLayout wording=card("متن روی ساعت");
        textInput(wording,"سطر اول",settings.title,32,v->settings.title=v);
        textInput(wording,"سطر دوم",settings.subtitle,48,v->settings.subtitle=v);
        note(wording,"متن می‌تواند فارسی یا انگلیسی باشد. برای پنهان کردن هر سطر، متنش را پاک کن. لوگوی اصلی همیشه ثابت است.");
        LinearLayout colors=card("رنگ عقربه‌ها");
        LinearLayout palette=new LinearLayout(this);palette.setGravity(Gravity.CENTER);colors.addView(palette,new LinearLayout.LayoutParams(-1,dp(56)));
        int[] accents={0xffbcf36a,0xff72d4ff,0xffffbc70,0xffd0a8ff,0xffff8f9e};
        String[] names={"سبز","آبی","طلایی","بنفش","صورتی"};
        for(int i=0;i<accents.length;i++){final int color=accents[i];TextView chip=text(settings.accent==color?"●":"",24,true);chip.setGravity(Gravity.CENTER);chip.setTextColor(Color.rgb(20,30,20));chip.setContentDescription("رنگ "+names[i]);chip.setBackground(background(color,24));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(44),1);lp.setMargins(dp(5),0,dp(5),0);palette.addView(chip,lp);chip.setOnClickListener(v->{settings.accent=color;changed(true);});}
        colors.addView(button("رنگ دلخواه",false,this::customColor),new LinearLayout.LayoutParams(-1,dp(48)));
        LinearLayout sizing=card("اندازه و شفافیت ویجت");
        slider(sizing,"اندازهٔ ساعت داخل قاب",settings.scale,60,100,v->settings.scale=v);
        slider(sizing,"وضوح پس‌زمینه",settings.opacity,0,100,v->settings.opacity=v);
        note(sizing,"برای بزرگ و کوچک کردن قاب، روی ویجت نگه دار و دستگیره‌های اطراف آن را بکش. اندازه‌های قابل انتخاب به صفحهٔ اصلی گوشی بستگی دارد.");
        LinearLayout details=card("جزئیات");
        toggle(details,"نمایش ثانیه‌شمار",settings.seconds,v->settings.seconds=v);
        toggle(details,"اعداد فارسی روی صفحهٔ عقربه‌ای و تاریخ",settings.digits,v->settings.digits=v);
        toggle(details,"ساعت دیجیتال ۲۴ ساعته",settings.hour24,v->settings.hour24=v);
        toggle(details,"نمایش تاریخ در ویجت",settings.date,v->settings.date=v);
        toggle(details,"نمایش روز هفته",settings.weekday,v->settings.weekday=v);
        choices(details,new String[]{"شمسی","میلادی","هر دو"},settings.calendar,v->settings.calendar=v);
        note(details,"تاریخ با تقویم گوشی محاسبه می‌شود و به جدول سالانه نیاز ندارد. اعداد ساعت دیجیتالِ ویجت از زبان گوشی پیروی می‌کنند.");gap(details,14);
        details.addView(text("منطقهٔ زمانی",14,true));gap(details,6);
        choices(details,new String[]{"زمان گوشی","تهران","UTC"},settings.zone.equals("Asia/Tehran")?1:settings.zone.equals("UTC")?2:0,
            v->settings.zone=v==1?"Asia/Tehran":v==2?"UTC":"");
        if(widgetId<0){
            LinearLayout saver=card("اسکرین‌سیور و ساعت تمام‌صفحه");
            toggle(saver,"نمایش تاریخ در اسکرین‌سیور",settings.saverDate,v->settings.saverDate=v);
            toggle(saver,"پس‌زمینهٔ مشکی برای شب",settings.saverNight,v->settings.saverNight=v);
            toggle(saver,"جابه‌جایی آرام ساعت روی صفحه",settings.saverDrift,v->settings.saverDrift=v);
            slider(saver,"روشنایی اسکرین‌سیور",settings.saverBrightness,5,100,v->settings.saverBrightness=v);
            saver.addView(button("اجرای ساعت تمام‌صفحه",true,()->{settings.save(this,-1);startActivity(new Intent(this,FullScreenClockActivity.class));}),new LinearLayout.LayoutParams(-1,dp(54)));
            saver.addView(button("انتخاب اسکرین‌سیور در تنظیمات گوشی",false,this::openScreensaverSettings),new LinearLayout.LayoutParams(-1,dp(60)));
            saver.addView(button("انتخاب پس‌زمینهٔ زندهٔ ساعت",false,this::openWallpaper),new LinearLayout.LayoutParams(-1,dp(54)));
            note(saver,"ساعت تمام‌صفحه را از اینجا باز کن؛ با دو بار لمس بسته می‌شود. نمایش روی قفل را اندروید مدیریت می‌کند و دکمهٔ پاور ممکن است صفحه را خاموش کند.");gap(saver,8);
            note(saver,"اسکرین‌سیور سیستم معمولاً هنگام شارژ و پس از خاموشی خودکار صفحه اجرا می‌شود. در تنظیمات گوشی «ساعت مهدی» را انتخاب کن. این قابلیت جایگزین Always On Display گوشی نیست.");
            gap(saver,8);note(saver,"برای نمایش به‌عنوان پس‌زمینه هم می‌توانی از گزینهٔ بالا استفاده کنی. امکان انتخاب صفحهٔ قفل به گوشی بستگی دارد؛ ساعت اصلی قفل ممکن است همچنان نمایش داده شود.");
        }
        LinearLayout widgets=card("ویجت صفحهٔ اصلی");
        note(widgets,"هر ویجت تنظیمات جدا دارد. برای تغییر رنگ و ظاهرش، روی همان ویجت بزن.");gap(widgets,12);
        if(widgetId>=0){widgets.addView(button("ذخیرهٔ تنظیمات ویجت",true,()->{
            settings.save(this,widgetId);ClockWidget.update(this,AppWidgetManager.getInstance(this),widgetId);ClockWidget.schedule(this);
            setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId));finish();
        }),new LinearLayout.LayoutParams(-1,dp(54)));
            widgets.addView(button("انصراف",false,this::finish),new LinearLayout.LayoutParams(-1,dp(48)));
        }else{
            widgets.addView(button("افزودن ویجت به صفحهٔ اصلی",true,this::pinWidget),new LinearLayout.LayoutParams(-1,dp(54)));
            note(widgets,"تنظیمات این صفحه پیش‌فرض ویجت‌های تازه است.");
        }
        scroll.post(()->scroll.scrollTo(0,previous));
    }
    private void updateTime(){
        if(about||digital==null||date==null)return;
        long now=System.currentTimeMillis();digital.setText(CalendarDates.time(now,settings));date.setText(CalendarDates.date(now,settings));
    }
    private void textInput(LinearLayout parent,String label,String value,int limit,Consumer<String> update){
        parent.addView(text(label,13,false));EditText input=new EditText(this);input.setText(value);input.setTextSize(15);input.setTextColor(settings.ink(this));
        input.setTypeface(ClockArt.font(this,false));input.setSingleLine(true);input.setSelectAllOnFocus(true);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(limit)});
        input.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);input.setContentDescription(label);parent.addView(input,new LinearLayout.LayoutParams(-1,dp(52)));
        input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){}
            public void afterTextChanged(Editable e){update.accept(e.toString());changed(false);}});gap(parent,8);
    }
    private void openScreensaverSettings(){
        try{startActivity(new Intent(android.provider.Settings.ACTION_DREAM_SETTINGS));}
        catch(ActivityNotFoundException e){try{startActivity(new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS));}catch(ActivityNotFoundException unavailable){Toast.makeText(this,"تنظیمات اسکرین‌سیور در این گوشی در دسترس نیست؛ از حالت تمام‌صفحه استفاده کن.",Toast.LENGTH_LONG).show();}}
    }
    private void openWallpaper(){
        try{startActivity(new Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new ComponentName(this,ClockWallpaperService.class)));}
        catch(ActivityNotFoundException e){Toast.makeText(this,"انتخاب پس‌زمینهٔ زنده در این گوشی در دسترس نیست.",Toast.LENGTH_LONG).show();}
    }
    private void pinWidget(){
        AppWidgetManager m=AppWidgetManager.getInstance(this);settings.save(this,-1);
        if(m.isRequestPinAppWidgetSupported()){
            boolean accepted=m.requestPinAppWidget(new ComponentName(this,ClockWidget.class),null,null);
            if(!accepted)widgetHelp();
        }else widgetHelp();
    }
    private void widgetHelp(){dialog().setTitle("افزودن ویجت").setMessage("روی فضای خالی صفحهٔ اصلی نگه دار، «ویجت‌ها» را باز کن و «ساعت مهدی» را انتخاب کن.").setPositiveButton("باشه",null).show();}
    private AlertDialog.Builder dialog(){return new AlertDialog.Builder(new ContextThemeWrapper(this,settings.light(this)?android.R.style.Theme_Material_Light_Dialog_Alert:android.R.style.Theme_Material_Dialog_Alert));}
    private void customColor(){
        EditText input=new EditText(this);input.setSingleLine(true);input.setText(String.format(java.util.Locale.ROOT,"#%06X",settings.accent&0xffffff));input.setSelectAllOnFocus(true);input.setTextDirection(View.TEXT_DIRECTION_LTR);
        input.setContentDescription("کد رنگ، مانند #BCF36A");
        AlertDialog d=dialog().setTitle("کد رنگ دلخواه").setView(input).setPositiveButton("اعمال",null).setNegativeButton("انصراف",null).create();
        d.setOnShowListener(v->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
            String value=input.getText().toString().trim();if(!value.matches("#[0-9a-fA-F]{6}")){input.setError("کد شش‌رقمی وارد کن؛ مثل #BCF36A");return;}
            settings.accent=Color.parseColor(value);changed(true);d.dismiss();
        }));d.show();
    }
    private void aboutPage(){
        LinearLayout brand=card(null);brand.setGravity(Gravity.CENTER_HORIZONTAL);
        View logo=new View(this){@Override protected void onDraw(Canvas c){super.onDraw(c);ClockArt.logo(c,(getWidth()-dp(136))/2f,0,dp(136),settings.ink(MainActivity.this));}};
        brand.addView(logo,new LinearLayout.LayoutParams(-1,dp(100)));gap(brand,16);
        TextView title=text("ساعت مهدی",24,true);title.setGravity(Gravity.CENTER);brand.addView(title);
        TextView subtitle=text("نسخهٔ ۱.۱.۱",12,false);subtitle.setGravity(Gravity.CENTER);brand.addView(subtitle);
        LinearLayout developer=card("طراحی و برنامه‌نویسی");developer.addView(text("مهدی اسلامی",22,true));developer.addView(text("Mehdi Eslami",14,false));gap(developer,16);
        link(developer,"وب‌سایت · mehdieslami.ir","https://mehdieslami.ir/");
        link(developer,"گیت‌هاب · MehdiEslami1221","https://github.com/MehdiEslami1221");
        link(developer,"لینکدین · Mehdi Eslami","https://www.linkedin.com/in/mehdi-eslami/");
        link(developer,"اینستاگرام · mage.code","https://www.instagram.com/mage.code/");
        LinearLayout privacy=card("دربارهٔ این ساعت");note(privacy,"برنامه آفلاین کار می‌کند. تنظیمات روی همین گوشی ذخیره می‌شود و اطلاعاتی ارسال نمی‌شود. باز کردن لینک‌های بالا از طریق مرورگر انجام می‌شود.");
        gap(privacy,12);note(privacy,"فونت: ایران‌سنس ایکس · لوگو: نشان اختصاصی وب‌سایت مهدی اسلامی");
    }
    private void link(LinearLayout p,String label,String url){p.addView(button(label,false,()->{
        try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(ActivityNotFoundException e){Toast.makeText(this,"مرورگری برای باز کردن لینک پیدا نشد.",Toast.LENGTH_SHORT).show();}
    }),new LinearLayout.LayoutParams(-1,dp(52)));}
}
