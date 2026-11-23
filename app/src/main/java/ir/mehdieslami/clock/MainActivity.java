package ir.mehdieslami.clock;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

public final class MainActivity extends Activity {
    private Settings settings;
    private int widgetId=-1,colorRole;
    private boolean about,syncing,previewQueued,dirty;
    private LinearLayout root,page,styleOptions;
    private View previewPanel;
    private ScrollView scroll;
    private ClockView preview;
    private TextView digital,date;
    private long shownSecond=Long.MIN_VALUE,shownMinute=Long.MIN_VALUE;
    private int timeFormatHash,dateFormatHash;
    private final List<Runnable> bindings=new ArrayList<>();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private static final ExecutorService WIDGETS=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"clock-widget-render");t.setDaemon(true);return t;});
    private final Runnable widgetRefresh=()->{Context app=getApplicationContext();WIDGETS.execute(()->ClockWidget.refresh(app));};
    private final Runnable persist=()->{if(widgetId<0){settings.save(this,-1);dirty=false;handler.removeCallbacks(widgetRefresh);handler.postDelayed(widgetRefresh,650);}};
    private final Runnable ticker=new Runnable(){public void run(){updateTime();handler.postDelayed(this,1000-System.currentTimeMillis()%1000);}};

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);getWindow().setDecorFitsSystemWindows(false);getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
        if(widgetId==AppWidgetManager.INVALID_APPWIDGET_ID)widgetId=-1;
        if(widgetId<0)Settings.migrateLegacyAppDefaults(this);else setResult(RESULT_CANCELED);
        settings=Settings.load(this,widgetId);
        if(saved!=null){Object restored=saved.getSerializable("clockSettings");if(restored instanceof Settings)settings=(Settings)restored;about=saved.getBoolean("about");colorRole=saved.getInt("colorRole",0);}
        buildPage();
    }
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putSerializable("clockSettings",settings);state.putBoolean("about",about);state.putInt("colorRole",colorRole);}
    @Override protected void onResume(){super.onResume();handler.removeCallbacks(ticker);handler.post(ticker);if(preview!=null)preview.setActive(true);}
    @Override protected void onPause(){
        handler.removeCallbacks(ticker);handler.removeCallbacks(persist);if(preview!=null)preview.setActive(false);
        if(widgetId<0&&(dirty||handler.hasCallbacks(widgetRefresh))){settings.save(this,-1);dirty=false;handler.removeCallbacks(widgetRefresh);widgetRefresh.run();}
        super.onPause();
    }
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
    @Override public void onBackPressed(){if(about){about=false;buildPage();}else super.onBackPressed();}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String value,int size,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTypeface(ClockArt.font(this,bold));t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);t.setGravity(Gravity.START);t.setLineSpacing(dp(4),1.08f);
        bindings.add(()->t.setTextColor(settings.ink(this)));return t;
    }
    private void gap(LinearLayout p,int size){Space s=new Space(this);p.addView(s,new LinearLayout.LayoutParams(1,dp(size)));}
    private void note(LinearLayout p,String value){TextView t=text(value,16,false);bindings.add(()->t.setTextColor(settings.muted(this)));p.addView(t,new LinearLayout.LayoutParams(-1,-2));}
    private LinearLayout card(String title){
        LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(16),dp(18),dp(16),dp(18));
        bindings.add(()->p.setBackground(bg(settings.surface(this),22)));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(16);page.addView(p,lp);
        if(title!=null){TextView heading=text(title,20,true);heading.setAccessibilityHeading(true);p.addView(heading);gap(p,12);}return p;
    }
    private Button button(String label,BooleanSupplier selected,Runnable action){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(17);b.setTypeface(ClockArt.font(this,true));b.setMinHeight(dp(60));b.setMinWidth(dp(56));b.setPadding(dp(10),dp(10),dp(10),dp(10));
        bindings.add(()->{boolean on=selected!=null&&selected.getAsBoolean();int background=on?settings.accent:settings.surface(this);b.setBackgroundTintList(ColorStateList.valueOf(background));b.setTextColor(on?AccessibleColors.textOn(background):settings.ink(this));b.setSelected(on);if(selected!=null)b.setStateDescription(on?"انتخاب‌شده":"انتخاب‌نشده");});
        b.setOnClickListener(v->action.run());return b;
    }
    private Button action(LinearLayout p,String label,boolean primary,Runnable run){Button b=button(label,primary?()->true:null,run);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(4);p.addView(b,lp);return b;}
    private void choices(LinearLayout p,String[] names,IntSupplier get,IntConsumer set){
        boolean stack=getResources().getConfiguration().fontScale>=1.3f||getResources().getConfiguration().screenWidthDp<340;
        LinearLayout row=new LinearLayout(this);row.setOrientation(stack?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);p.addView(row,new LinearLayout.LayoutParams(-1,-2));
        for(int i=0;i<names.length;i++){final int n=i;Button b=button(names[i],()->get.getAsInt()==n,()->{set.accept(n);changed(true);});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(stack?-1:0,-2,stack?0:1);lp.setMargins(dp(2),dp(3),dp(2),dp(3));row.addView(b,lp);}
    }
    private Switch toggle(LinearLayout p,String label,BooleanSupplier get,Consumer<Boolean> set){
        Switch s=new Switch(this);s.setId(View.generateViewId());s.setText(label);s.setTextSize(18);s.setTypeface(ClockArt.font(this,false));s.setMinHeight(dp(64));s.setPadding(0,dp(14),0,dp(14));s.setSwitchPadding(dp(16));p.addView(s,new LinearLayout.LayoutParams(-1,-2));
        bindings.add(()->{s.setTextColor(settings.ink(this));s.setChecked(get.getAsBoolean());int checked=AccessibleColors.ensure(settings.accent,settings.surface(this),3);s.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{checked,settings.muted(this)}));});
        s.setOnCheckedChangeListener((v,value)->{if(!syncing){set.accept(value);changed(true);}});return s;
    }
    private void slider(LinearLayout p,String label,IntSupplier get,int min,int max,IntConsumer set){
        TextView title=text("",17,false);p.addView(title);LinearLayout row=new LinearLayout(this);row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);p.addView(row,new LinearLayout.LayoutParams(-1,-2));
        SeekBar bar=new SeekBar(this);bar.setId(View.generateViewId());title.setLabelFor(bar.getId());bar.setMin(min);bar.setMax(max);bar.setContentDescription(label);bar.setMinHeight(dp(64));
        Button minus=button("−",null,()->{set.accept(Math.max(min,get.getAsInt()-5));changed(true);});minus.setTextSize(24);minus.setContentDescription("کاهش "+label);
        Button plus=button("+",null,()->{set.accept(Math.min(max,get.getAsInt()+5));changed(true);});plus.setTextSize(24);plus.setContentDescription("افزایش "+label);
        row.addView(minus,new LinearLayout.LayoutParams(dp(56),-2));row.addView(bar,new LinearLayout.LayoutParams(0,dp(64),1));row.addView(plus,new LinearLayout.LayoutParams(dp(56),-2));
        Runnable value=()->{String current=ClockArt.digits(Integer.toString(get.getAsInt()),true)+"٪";title.setText(label+" · "+current);bar.setStateDescription(current);};
        bindings.add(()->{bar.setProgress(get.getAsInt());bar.setProgressTintList(ColorStateList.valueOf(AccessibleColors.ensure(settings.accent,settings.surface(this),3)));bar.setThumbTintList(ColorStateList.valueOf(AccessibleColors.ensure(settings.accent,settings.surface(this),3)));value.run();});
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int v,boolean user){if(user&&!syncing){set.accept(v);value.run();changed(false);}}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){syncUi();}});gap(p,14);
    }
    private void input(LinearLayout p,String label,Supplier<String> get,Consumer<String> set,int limit){
        TextView title=text(label,17,false);p.addView(title);EditText e=new EditText(this);e.setId(View.generateViewId());title.setLabelFor(e.getId());e.setText(get.get());e.setTextSize(18);e.setTypeface(ClockArt.font(this,false));e.setMinHeight(dp(64));e.setSingleLine(true);e.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);e.setContentDescription(label);e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(limit)});p.addView(e,new LinearLayout.LayoutParams(-1,-2));
        bindings.add(()->{e.setTextColor(settings.ink(this));e.setBackgroundTintList(ColorStateList.valueOf(settings.muted(this)));if(!e.hasFocus()&&!e.getText().toString().equals(get.get()))e.setText(get.get());});
        e.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){}public void afterTextChanged(Editable value){if(!syncing){set.accept(value.toString());changed(false);}}});gap(p,12);
    }
    private void changed(boolean sync){
        dirty=true;if(sync)syncUi();
        if(!previewQueued&&preview!=null){previewQueued=true;preview.postOnAnimation(()->{previewQueued=false;if(preview!=null)preview.setSettings(settings);if(digital!=null)digital.setTextSize(24*settings.digitalSize/100f);if(date!=null)date.setTextSize(16*settings.dateSize/100f);updateTime();});}
        if(widgetId<0){handler.removeCallbacks(persist);handler.postDelayed(persist,200);}
    }
    private void syncUi(){syncing=true;for(Runnable binding:bindings)binding.run();syncing=false;updateTime();}
    private void buildPage(){
        if(preview!=null)preview.setActive(false);preview=null;previewPanel=null;digital=null;date=null;previewQueued=false;shownSecond=shownMinute=Long.MIN_VALUE;bindings.clear();
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);bindings.add(()->root.setBackgroundColor(settings.background(this)));
        root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());v.setPadding(i.left,i.top,i.right,i.bottom);if(previewPanel!=null)previewPanel.setVisibility(insets.isVisible(WindowInsets.Type.ime())?View.GONE:View.VISIBLE);return insets;});
        scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(16),dp(16),dp(16),dp(24));scroll.addView(page,new ScrollView.LayoutParams(-1,-2));setContentView(root);root.requestApplyInsets();
        bindings.add(()->{getWindow().setNavigationBarColor(settings.background(this));getWindow().setStatusBarColor(settings.background(this));WindowInsetsController c=getWindow().getInsetsController();if(c!=null)c.setSystemBarsAppearance(settings.light(this)?WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS:0,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);});
        LinearLayout header=new LinearLayout(this);boolean large=getResources().getConfiguration().fontScale>=1.3f;header.setOrientation(large?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);header.setGravity(Gravity.CENTER_VERTICAL);page.addView(header);
        TextView title=text(about?"دربارهٔ برنامه":widgetId>=0?"تنظیم ویجت":"ساعت مهدی",24,true);title.setAccessibilityHeading(true);header.addView(title,new LinearLayout.LayoutParams(large?-1:0,-2,large?0:1));
        Button aboutButton=button(about?"بازگشت":"درباره",null,()->{about=!about;buildPage();});header.addView(aboutButton,new LinearLayout.LayoutParams(large?-1:dp(104),-2));gap(page,14);
        if(about){aboutPage();syncUi();return;}
        page.removeView(header);header.setPadding(dp(16),dp(6),dp(16),0);root.addView(header,0,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout hero=new LinearLayout(this);previewPanel=hero;hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(12),0,dp(12),dp(8));bindings.add(()->hero.setBackgroundColor(settings.surface(this)));root.addView(hero,1,new LinearLayout.LayoutParams(-1,-2));
        preview=new ClockView(this,settings);preview.setId(View.generateViewId());
        int side=Math.max(92,Math.min(large?140:210,Math.round(getResources().getConfiguration().screenHeightDp*.28f)));hero.addView(preview,new LinearLayout.LayoutParams(-1,dp(side)));
        digital=text("",24,true);digital.setGravity(Gravity.CENTER);digital.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);hero.addView(digital);
        date=text("",16,false);date.setGravity(Gravity.CENTER);date.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);hero.addView(date);
        bindings.add(()->{digital.setVisibility(settings.clockType==0?View.VISIBLE:View.GONE);digital.setTextSize(24*settings.digitalSize/100f);digital.setTextColor(AccessibleColors.ensure(settings.rawColor(this,"digital"),settings.surface(this),4.5));date.setTextSize(16*settings.dateSize/100f);date.setTextColor(settings.ink(this));});
        LinearLayout themes=card("تم و صفحهٔ ساعت");
        choices(themes,new String[]{"عقربه‌ای","دیجیتال","ترکیبی"},()->settings.clockType,n->settings.clockType=n);gap(themes,8);
        choices(themes,new String[]{"تیره","روشن"},()->settings.theme,n->settings.theme=n);gap(themes,8);
        styleOptions=new LinearLayout(this);styleOptions.setOrientation(LinearLayout.VERTICAL);themes.addView(styleOptions);
        choices(styleOptions,new String[]{"کلاسیک","ساده","مداری"},()->settings.style,n->settings.style=n);bindings.add(()->styleOptions.setVisibility(settings.clockType==1?View.GONE:View.VISIBLE));
        LinearLayout access=card("خوانایی و استفادهٔ راحت‌تر");
        action(access,"تنظیم پیشنهادی برای خوانایی بیشتر",true,()->{settings.readablePreset();changed(true);});
        toggle(access,"حفظ کنتراست رنگ‌های ساعت",()->settings.readableColors,v->settings.readableColors=v);
        toggle(access,"پس‌زمینهٔ کامل و پرکنتراست",()->settings.highContrast,v->settings.highContrast=v);
        note(access,"اندازهٔ نوشته‌های تنظیمات از تنظیم اندازهٔ متن گوشی پیروی می‌کند. دکمه‌های + و − هم برای تغییر دقیق اندازه‌ها در دسترس‌اند.");
        LinearLayout sizes=card("اندازهٔ اجزای داخل ساعت");
        slider(sizes,"اندازهٔ اعداد ساعت",()->settings.numbersSize,70,200,n->settings.numbersSize=n);
        slider(sizes,"ضخامت عقربه‌ها",()->settings.handWidth,60,200,n->settings.handWidth=n);
        slider(sizes,"طول عقربه‌ها",()->settings.handLength,70,115,n->settings.handLength=n);
        slider(sizes,"اندازهٔ لوگو",()->settings.logoSize,60,150,n->settings.logoSize=n);
        slider(sizes,"اندازهٔ سطر اول",()->settings.titleSize,60,200,n->settings.titleSize=n);
        slider(sizes,"اندازهٔ سطر دوم",()->settings.subtitleSize,60,200,n->settings.subtitleSize=n);
        slider(sizes,"اندازهٔ ساعت دیجیتال",()->settings.digitalSize,70,180,n->settings.digitalSize=n);
        slider(sizes,"اندازهٔ تاریخ",()->settings.dateSize,80,180,n->settings.dateSize=n);
        slider(sizes,"ضخامت خط‌های دور ساعت",()->settings.marksSize,60,180,n->settings.marksSize=n);
        action(sizes,"بازگشت اندازه‌ها به حالت اولیه",false,()->{settings.resetSizes();changed(true);});
        colorsCard();
        LinearLayout wording=card("متن روی ساعت");input(wording,"سطر اول",()->settings.title,v->settings.title=v,32);input(wording,"سطر دوم",()->settings.subtitle,v->settings.subtitle=v,48);note(wording,"برای پنهان کردن یک سطر، متنش را پاک کن. طرح اصلی لوگو ثابت می‌ماند.");
        LinearLayout frame=card("اندازهٔ قاب و شفافیت");
        slider(frame,"اندازهٔ ساعت داخل قاب",()->settings.scale,60,100,n->settings.scale=n);slider(frame,"وضوح پس‌زمینه",()->settings.opacity,0,100,n->settings.opacity=n);
        note(frame,"در حالت پرکنتراست، پس‌زمینهٔ ساعت کامل نمایش داده می‌شود. برای تغییر اندازهٔ قاب، روی ویجت نگه دار و دستگیره‌های اطرافش را بکش.");
        LinearLayout details=card("تاریخ و زمان");
        toggle(details,"نمایش ثانیه‌شمار",()->settings.seconds,v->settings.seconds=v);toggle(details,"اعداد فارسی روی ساعت و تاریخ",()->settings.digits,v->settings.digits=v);toggle(details,"ساعت دیجیتال ۲۴ ساعته",()->settings.hour24,v->settings.hour24=v);
        toggle(details,"نمایش تاریخ در ویجت",()->settings.date,v->settings.date=v);toggle(details,"نمایش روز هفته",()->settings.weekday,v->settings.weekday=v);
        choices(details,new String[]{"شمسی","میلادی","هر دو"},()->settings.calendar,v->settings.calendar=v);gap(details,12);
        note(details,"اعداد ساعت دیجیتال ویجت از زبان گوشی پیروی می‌کنند. کنتراست ویجت شفاف به تصویر پس‌زمینهٔ گوشی هم بستگی دارد.");gap(details,12);
        TextView zone=text("منطقهٔ زمانی",18,true);details.addView(zone);
        choices(details,new String[]{"زمان گوشی","تهران","UTC"},()->settings.zone.equals("Asia/Tehran")?1:settings.zone.equals("UTC")?2:0,v->settings.zone=v==1?"Asia/Tehran":v==2?"UTC":"");
        if(widgetId<0)saverCard();
        LinearLayout widgets=card("ویجت صفحهٔ اصلی");note(widgets,"هر ویجت تنظیمات جدا دارد. برای تغییر ظاهرش، روی همان ویجت بزن.");gap(widgets,10);
        if(widgetId>=0){
            action(widgets,"ذخیرهٔ تنظیمات ویجت",true,()->{settings.save(this,widgetId);Context app=getApplicationContext();int id=widgetId;WIDGETS.execute(()->{ClockWidget.update(app,AppWidgetManager.getInstance(app),id);ClockWidget.schedule(app);});setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId));finish();});
            action(widgets,"انصراف",false,this::finish);
        }else{action(widgets,"افزودن ویجت به صفحهٔ اصلی",true,this::pinWidget);note(widgets,"تنظیمات این صفحه، پیش‌فرض ویجت‌های تازه است.");}
        syncUi();
    }
    private void colorsCard(){
        LinearLayout colors=card("جعبهٔ رنگ ساعت");TextView label=text("بخشی که می‌خواهی رنگش را عوض کنی",17,false);colors.addView(label);
        Spinner selector=new Spinner(this);selector.setId(View.generateViewId());label.setLabelFor(selector.getId());selector.setContentDescription("انتخاب بخش ساعت برای تغییر رنگ");selector.setMinimumHeight(dp(64));
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,Settings.COLOR_NAMES){
            private View style(View v){TextView t=(TextView)v;t.setTextSize(18);t.setTypeface(ClockArt.font(MainActivity.this,false));t.setTextColor(settings.ink(MainActivity.this));t.setBackgroundColor(settings.surface(MainActivity.this));t.setMinHeight(dp(64));t.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);t.setPadding(dp(10),dp(10),dp(10),dp(10));return t;}
            @Override public View getView(int n,View v,ViewGroup p){return style(super.getView(n,v,p));}
            @Override public View getDropDownView(int n,View v,ViewGroup p){return style(super.getDropDownView(n,v,p));}
        };selector.setAdapter(adapter);selector.setSelection(colorRole);colors.addView(selector,new LinearLayout.LayoutParams(-1,-2));
        TextView sample=text("",18,true);sample.setGravity(Gravity.CENTER);sample.setMinHeight(dp(64));sample.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);colors.addView(sample);
        Runnable update=()->{int c=settings.rawColor(this,Settings.COLOR_ROLES[colorRole]);sample.setBackground(bg(c,12));sample.setTextColor(AccessibleColors.textOn(c));sample.setText(String.format(Locale.ROOT,"#%06X",c&0xffffff));};bindings.add(update);
        selector.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> a,View v,int position,long id){colorRole=position;update.run();}public void onNothingSelected(android.widget.AdapterView<?> a){}});
        bindings.add(()->{View selected=selector.getSelectedView();if(selected instanceof TextView){((TextView)selected).setTextColor(settings.ink(this));selected.setBackgroundColor(settings.surface(this));}});
        action(colors,"باز کردن جعبهٔ رنگ",true,()->{String role=Settings.COLOR_ROLES[colorRole];new ColorPickerDialog(this,settings,Settings.COLOR_NAMES[colorRole],settings.rawColor(this,role),v->{settings.setColor(role,v);changed(true);},()->{settings.resetColor(role);changed(true);}).show();});
        note(colors,"رنگ اصلی، اعداد، عقربه‌ها، خط‌ها، نوشته‌ها و صفحهٔ ساعت را جداگانه انتخاب کن. در جعبهٔ رنگ، رنگ‌های آماده و انتخاب دقیق هم وجود دارد.");
    }
    private void saverCard(){
        LinearLayout saver=card("اسکرین‌سیور و ساعت تمام‌صفحه");
        toggle(saver,"نمایش تاریخ در اسکرین‌سیور",()->settings.saverDate,v->settings.saverDate=v);toggle(saver,"پس‌زمینهٔ مشکی برای شب",()->settings.saverNight,v->settings.saverNight=v);toggle(saver,"جابه‌جایی آرام ساعت روی صفحه",()->settings.saverDrift,v->settings.saverDrift=v);
        slider(saver,"روشنایی اسکرین‌سیور",()->settings.saverBrightness,5,100,n->settings.saverBrightness=n);
        action(saver,"اجرای ساعت تمام‌صفحه",true,()->{settings.save(this,-1);startActivity(new Intent(this,FullScreenClockActivity.class));});
        action(saver,"انتخاب اسکرین‌سیور در تنظیمات گوشی",false,this::openScreensaverSettings);action(saver,"انتخاب پس‌زمینهٔ زندهٔ ساعت",false,this::openWallpaper);
        note(saver,"اسکرین‌سیور سیستم معمولاً هنگام شارژ و پس از بی‌استفاده‌ماندن گوشی اجرا می‌شود. ساعت تمام‌صفحه را می‌توانی همین حالا باز کنی؛ دکمهٔ خروج هم روی صفحه دارد.");gap(saver,8);note(saver,"نمایش پس‌زمینهٔ زنده روی قفل به گوشی بستگی دارد. این حالت‌ها جایگزین Always On Display گوشی نیستند.");
    }
    private void updateTime(){
        if(about||digital==null||date==null)return;long now=System.currentTimeMillis();
        int timeHash=Objects.hash(settings.zone,settings.hour24,settings.seconds,settings.digits),dateHash=Objects.hash(settings.zone,settings.calendar,settings.weekday,settings.digits);
        if(shownSecond!=now/1000||timeFormatHash!=timeHash){shownSecond=now/1000;timeFormatHash=timeHash;digital.setText(CalendarDates.time(now,settings));}
        if(shownMinute!=now/60000||dateFormatHash!=dateHash){shownMinute=now/60000;dateFormatHash=dateHash;date.setText(CalendarDates.date(now,settings));}
    }
    private void pinWidget(){settings.save(this,-1);AppWidgetManager m=AppWidgetManager.getInstance(this);if(!m.isRequestPinAppWidgetSupported()||!m.requestPinAppWidget(new ComponentName(this,ClockWidget.class),null,null))dialog().setTitle("افزودن ویجت").setMessage("روی فضای خالی صفحهٔ اصلی نگه دار، «ویجت‌ها» را باز کن و «ساعت مهدی» را انتخاب کن.").setPositiveButton("باشه",null).show();}
    private AlertDialog.Builder dialog(){return new AlertDialog.Builder(new ContextThemeWrapper(this,settings.light(this)?android.R.style.Theme_Material_Light_Dialog_Alert:android.R.style.Theme_Material_Dialog_Alert));}
    private void openScreensaverSettings(){try{startActivity(new Intent(android.provider.Settings.ACTION_DREAM_SETTINGS));}catch(ActivityNotFoundException e){try{startActivity(new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS));}catch(ActivityNotFoundException unavailable){Toast.makeText(this,"از ساعت تمام‌صفحه استفاده کن؛ تنظیمات اسکرین‌سیور در این گوشی در دسترس نیست.",Toast.LENGTH_LONG).show();}}}
    private void openWallpaper(){try{startActivity(new Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new ComponentName(this,ClockWallpaperService.class)));}catch(ActivityNotFoundException e){Toast.makeText(this,"انتخاب پس‌زمینهٔ زنده در این گوشی در دسترس نیست.",Toast.LENGTH_LONG).show();}}
    private void link(LinearLayout p,String name,String url){action(p,name,false,()->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(ActivityNotFoundException e){Toast.makeText(this,"مرورگری برای باز کردن لینک پیدا نشد.",Toast.LENGTH_SHORT).show();}});}
    private void aboutPage(){
        LinearLayout brand=card(null);View logo=new View(this){@Override protected void onDraw(Canvas c){super.onDraw(c);ClockArt.logo(c,(getWidth()-dp(136))/2f,0,dp(136),settings.ink(MainActivity.this));}};logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);brand.addView(logo,new LinearLayout.LayoutParams(-1,dp(100)));brand.addView(text("ساعت مهدی",26,true));brand.addView(text("نسخهٔ ۱.۲.۰",18,false));
        LinearLayout developer=card("طراحی و برنامه‌نویسی");developer.addView(text("مهدی اسلامی",24,true));developer.addView(text("Mehdi Eslami",18,false));
        link(developer,"وب‌سایت · mehdieslami.ir","https://mehdieslami.ir/");link(developer,"گیت‌هاب · MehdiEslami1221","https://github.com/MehdiEslami1221");link(developer,"لینکدین · Mehdi Eslami","https://www.linkedin.com/in/mehdi-eslami/");link(developer,"اینستاگرام · mage.code","https://www.instagram.com/mage.code/");
        LinearLayout privacy=card("دربارهٔ این ساعت");note(privacy,"برنامه آفلاین کار می‌کند و اطلاعاتی ارسال نمی‌کند. تنظیمات روی همین گوشی ذخیره می‌شود.");note(privacy,"فونت ایران‌سنس ایکس · لوگوی اختصاصی مهدی اسلامی");
    }
}
