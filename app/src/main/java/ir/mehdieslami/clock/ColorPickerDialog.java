package ir.mehdieslami.clock;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.Locale;
import java.util.function.IntConsumer;

/** Color swatches plus HSV, RGB and hex input; every gesture has a control alternative. */
final class ColorPickerDialog extends AlertDialog {
    private static final int[] SWATCHES={0xffbcf36a,0xff43a047,0xff00695c,0xff80cbc4,0xff72d4ff,0xff039be5,0xff1565c0,0xff283593,0xffd0a8ff,0xff7e57c2,0xff6a1b9a,0xffc2185b,0xffff8f9e,0xffef5350,0xffb71c1c,0xffff7043,0xffffbc70,0xffffd54f,0xff8d6e63,0xff455a64,0xffffffff,0xffd7e0d5,0xff757575,0xff000000};
    private final Settings settings;
    private final IntConsumer apply;
    private final Runnable reset;
    private final String roleName;
    private int color;
    private final float[] hsv=new float[3];
    private LinearLayout content,palette,precise;
    private TextView sample;
    private EditText hex;
    private SeekBar hue;
    private final SeekBar[] rgb=new SeekBar[3];
    private final TextView[] rgbLabels=new TextView[3];
    private ColorSquare square;
    private boolean syncing;
    ColorPickerDialog(Context c,Settings s,String roleName,int current,IntConsumer apply,Runnable reset){
        super(new ContextThemeWrapper(c,s.light(c)?android.R.style.Theme_Material_Light_Dialog_Alert:android.R.style.Theme_Material_Dialog_Alert));
        settings=s;this.roleName=roleName;color=current;this.apply=apply;this.reset=reset;Color.colorToHSV(color,hsv);
    }
    private int dp(float n){return Math.round(n*getContext().getResources().getDisplayMetrics().density);}
    private TextView text(String value){TextView t=new TextView(getContext());t.setText(value);t.setTextSize(17);t.setTypeface(ClockArt.font(getContext(),false));t.setTextColor(settings.ink(getContext()));t.setPadding(0,dp(6),0,dp(6));return t;}
    private Button button(String name,Runnable action){Button b=new Button(getContext());b.setText(name);b.setAllCaps(false);b.setTextSize(17);b.setTypeface(ClockArt.font(getContext(),true));b.setMinHeight(dp(56));b.setPadding(dp(8),dp(10),dp(8),dp(10));b.setOnClickListener(v->action.run());return b;}
    @Override protected void onCreate(Bundle saved){
        setTitle(roleName);ScrollView scroll=new ScrollView(getContext());
        content=new LinearLayout(getContext());content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(18),dp(8),dp(18),dp(8));content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);scroll.addView(content);
        sample=text("");sample.setGravity(Gravity.CENTER);sample.setMinHeight(dp(64));content.addView(sample,new LinearLayout.LayoutParams(-1,-2));
        content.addView(button("رنگ‌های آماده",()->{palette.setVisibility(View.VISIBLE);precise.setVisibility(View.GONE);}));
        content.addView(button("انتخاب دقیق رنگ",()->{palette.setVisibility(View.GONE);precise.setVisibility(View.VISIBLE);}));
        palette=new LinearLayout(getContext());palette.setOrientation(LinearLayout.VERTICAL);content.addView(palette);
        for(int i=0;i<SWATCHES.length;i+=3){
            LinearLayout row=new LinearLayout(getContext());palette.addView(row,new LinearLayout.LayoutParams(-1,-2));
            for(int j=i;j<Math.min(i+3,SWATCHES.length);j++){
                final int swatch=SWATCHES[j];Button chip=button("",()->setColor(swatch));chip.setText("●");chip.setTextColor(AccessibleColors.textOn(swatch));chip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(swatch));
                chip.setContentDescription("انتخاب رنگ "+code(swatch));chip.setTag(swatch);chip.setMinWidth(dp(56));
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(3),dp(3),dp(3),dp(3));row.addView(chip,p);
            }
        }
        precise=new LinearLayout(getContext());precise.setOrientation(LinearLayout.VERTICAL);precise.setVisibility(View.GONE);content.addView(precise);
        square=new ColorSquare(getContext());precise.addView(square,new LinearLayout.LayoutParams(-1,dp(180)));
        precise.addView(text("طیف رنگ"));hue=new SeekBar(getContext());hue.setMax(359);hue.setMinHeight(dp(64));hue.setContentDescription("طیف رنگ");precise.addView(hue,new LinearLayout.LayoutParams(-1,dp(64)));
        hue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int value,boolean fromUser){if(fromUser&&!syncing){hsv[0]=value;setColor(Color.HSVToColor(hsv));}}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
        for(int i=0;i<3;i++){
            final int index=i;String name=new String[]{"قرمز","سبز","آبی"}[i];rgbLabels[i]=text(name);precise.addView(rgbLabels[i]);
            LinearLayout row=new LinearLayout(getContext());row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);precise.addView(row,new LinearLayout.LayoutParams(-1,-2));
            Button minus=button("−",()->changeRgb(index,-1));minus.setContentDescription("کاهش "+name);row.addView(minus,new LinearLayout.LayoutParams(dp(56),-2));
            SeekBar bar=new SeekBar(getContext());bar.setMax(255);bar.setContentDescription("مقدار "+name);rgb[i]=bar;row.addView(bar,new LinearLayout.LayoutParams(0,dp(64),1));
            Button plus=button("+",()->changeRgb(index,1));plus.setContentDescription("افزایش "+name);row.addView(plus,new LinearLayout.LayoutParams(dp(56),-2));
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int value,boolean fromUser){if(fromUser&&!syncing){int[] channels={Color.red(color),Color.green(color),Color.blue(color)};channels[index]=value;setColor(Color.rgb(channels[0],channels[1],channels[2]));}}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
        }
        TextView hexLabel=text("کد رنگ");precise.addView(hexLabel);hex=new EditText(getContext());hex.setId(View.generateViewId());hexLabel.setLabelFor(hex.getId());hex.setSingleLine(true);hex.setTextSize(18);hex.setMinHeight(dp(56));hex.setTextDirection(View.TEXT_DIRECTION_LTR);hex.setTextColor(settings.ink(getContext()));hex.setContentDescription("کد شش‌رقمی رنگ، مثل #BCF36A");hex.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});precise.addView(hex);
        hex.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){}public void afterTextChanged(Editable e){if(!syncing&&e.toString().matches("#[0-9a-fA-F]{6}"))setColor(Color.parseColor(e.toString()));}});
        content.addView(text("وقتی حفظ کنتراست روشن است، رنگ نمایش‌داده‌شده روی ساعت برای خوانایی بهتر تنظیم می‌شود."));
        setView(scroll);setButton(BUTTON_POSITIVE,"اعمال",(d,w)->apply.accept(color));setButton(BUTTON_NEGATIVE,"انصراف",(d,w)->{});setButton(BUTTON_NEUTRAL,"رنگ پیش‌فرض",(d,w)->reset.run());
        super.onCreate(saved);sync();
    }
    @Override public void show(){
        super.show();getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);for(int which:new int[]{BUTTON_POSITIVE,BUTTON_NEGATIVE,BUTTON_NEUTRAL}){
            Button b=getButton(which);b.setMinHeight(dp(64));b.setTextSize(17);b.setTypeface(ClockArt.font(getContext(),true));b.setTextColor(settings.ink(getContext()));
            if(b.getParent() instanceof LinearLayout){((LinearLayout)b.getParent()).setOrientation(LinearLayout.VERTICAL);b.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));}
        }
    }
    private static String code(int color){return String.format(Locale.ROOT,"#%06X",color&0xffffff);}
    private void changeRgb(int index,int amount){int[] c={Color.red(color),Color.green(color),Color.blue(color)};c[index]=Settings.clamp(c[index]+amount,0,255);setColor(Color.rgb(c[0],c[1],c[2]));}
    private void setColor(int value){float previousHue=hsv[0];color=value|0xff000000;Color.colorToHSV(color,hsv);if(hsv[1]==0)hsv[0]=previousHue;sync();}
    private void sync(){
        if(sample==null)return;syncing=true;sample.setText(code(color));sample.setBackgroundColor(color);sample.setTextColor(AccessibleColors.textOn(color));
        if(hex!=null&&!hex.hasFocus())hex.setText(code(color));
        hue.setProgress(Math.round(hsv[0]));int[] c={Color.red(color),Color.green(color),Color.blue(color)};
        for(int i=0;i<3;i++){rgb[i].setProgress(c[i]);rgbLabels[i].setText(new String[]{"قرمز","سبز","آبی"}[i]+" · "+ClockArt.digits(Integer.toString(c[i]),true));}
        for(int i=0;i<palette.getChildCount();i++){LinearLayout row=(LinearLayout)palette.getChildAt(i);for(int j=0;j<row.getChildCount();j++){Button chip=(Button)row.getChildAt(j);boolean selected=(Integer)chip.getTag()==color;chip.setSelected(selected);chip.setText(selected?"✓":"●");chip.setStateDescription(selected?"انتخاب‌شده":"");}}
        square.invalidate();syncing=false;
    }
    private final class ColorSquare extends View {
        private final Paint paint=new Paint(3);
        ColorSquare(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){
            paint.setShader(new LinearGradient(0,0,getWidth(),0,Color.WHITE,Color.HSVToColor(new float[]{hsv[0],1,1}),Shader.TileMode.CLAMP));c.drawRect(0,0,getWidth(),getHeight(),paint);
            paint.setShader(new LinearGradient(0,0,0,getHeight(),Color.TRANSPARENT,Color.BLACK,Shader.TileMode.CLAMP));c.drawRect(0,0,getWidth(),getHeight(),paint);paint.setShader(null);
            float x=hsv[1]*getWidth(),y=(1-hsv[2])*getHeight();paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(3));paint.setColor(Color.BLACK);c.drawCircle(x,y,dp(9),paint);paint.setStrokeWidth(dp(1.5f));paint.setColor(Color.WHITE);c.drawCircle(x,y,dp(9),paint);paint.setStyle(Paint.Style.FILL);
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE||e.getAction()==MotionEvent.ACTION_UP){getParent().requestDisallowInterceptTouchEvent(true);hsv[1]=Math.max(0,Math.min(1,e.getX()/getWidth()));hsv[2]=1-Math.max(0,Math.min(1,e.getY()/getHeight()));setColor(Color.HSVToColor(hsv));if(e.getAction()==MotionEvent.ACTION_UP)performClick();return true;}return super.onTouchEvent(e);
        }
        @Override public boolean performClick(){super.performClick();return true;}
    }
}
