package ir.mehdieslami.clock;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

final class Settings implements java.io.Serializable {
    private static final long serialVersionUID=1L;
    static final String[] COLOR_ROLES={"accent","dial","numbers","hour","minute","second","marks","title","subtitle","digital"};
    static final String[] COLOR_NAMES={"رنگ اصلی","صفحهٔ ساعت","اعداد ساعت","ساعت‌شمار","دقیقه‌شمار","ثانیه‌شمار","خط‌های دور ساعت","متن سطر اول","متن سطر دوم","ساعت دیجیتال"};
    final java.util.HashMap<String,Integer> colors=new java.util.HashMap<>();
    int numbersSize=100, handWidth=100, handLength=100, logoSize=100, titleSize=100, subtitleSize=100, digitalSize=100, dateSize=100, marksSize=100;
    boolean readableColors=true, highContrast=false;
    int theme = 0; // dark, light
    int accent = Color.rgb(188,243,106);
    int style = 0;
    int scale = 100;
    int opacity = 100;
    boolean seconds = true, logo = true, digits = true, date = true;
    boolean weekday = true, hour24 = true, saverDate = true, saverNight = true, saverDrift = true;
    int clockType = 0, calendar = 0, saverBrightness = 12;
    String title = "MEHDI ESLAMI", subtitle = "mehdieslami.ir";
    String zone = "";

    static Settings load(Context c, int id) {
        SharedPreferences p=c.getSharedPreferences("clocks",Context.MODE_PRIVATE);
        String k=id<0 || !p.contains("w"+id+".theme") ? "default." : "w"+id+".";
        Settings s=new Settings();
        s.theme=p.getInt(k+"theme",0); s.accent=p.getInt(k+"accent",s.accent);
        s.style=p.getInt(k+"style",0); s.scale=p.getInt(k+"scale",100); s.opacity=p.getInt(k+"opacity",100);
        s.seconds=p.getBoolean(k+"seconds",true); s.logo=true;
        s.digits=p.getBoolean(k+"digits",true); s.date=p.getBoolean(k+"date",true);
        s.weekday=p.getBoolean(k+"weekday",true);s.hour24=p.getBoolean(k+"hour24",true);
        s.clockType=p.getInt(k+"clockType",0);s.calendar=p.getInt(k+"calendar",0);
        s.title=p.getString(k+"title",s.title);s.subtitle=p.getString(k+"subtitle",s.subtitle);
        s.saverDate=p.getBoolean(k+"saverDate",true);s.saverNight=p.getBoolean(k+"saverNight",true);
        s.saverDrift=p.getBoolean(k+"saverDrift",true);s.saverBrightness=p.getInt(k+"saverBrightness",12);
        s.zone=p.getString(k+"zone","");
        s.numbersSize=clamp(p.getInt(k+"numbersSize",100),70,200);s.handWidth=clamp(p.getInt(k+"handWidth",100),60,200);
        s.handLength=clamp(p.getInt(k+"handLength",100),70,115);s.logoSize=clamp(p.getInt(k+"logoSize",100),60,150);
        s.titleSize=clamp(p.getInt(k+"titleSize",100),60,200);s.subtitleSize=clamp(p.getInt(k+"subtitleSize",100),60,200);
        s.digitalSize=clamp(p.getInt(k+"digitalSize",100),70,180);s.dateSize=clamp(p.getInt(k+"dateSize",100),80,180);s.marksSize=clamp(p.getInt(k+"marksSize",100),60,180);
        s.readableColors=p.getBoolean(k+"readableColors",true);s.highContrast=p.getBoolean(k+"highContrast",false);
        for(String role:COLOR_ROLES)if(p.contains(k+"color."+role))s.colors.put(role,p.getInt(k+"color."+role,0));
        return s;
    }
    void save(Context c,int id) {
        String k=id<0?"default.":"w"+id+".";
        SharedPreferences.Editor editor=c.getSharedPreferences("clocks",0).edit();
        editor.putInt(k+"theme",theme).putInt(k+"accent",accent)
            .putInt(k+"style",style).putInt(k+"scale",scale).putInt(k+"opacity",opacity)
            .putBoolean(k+"seconds",seconds).putBoolean(k+"logo",logo).putBoolean(k+"digits",digits)
            .putBoolean(k+"date",date).putString(k+"zone",zone)
            .putBoolean(k+"weekday",weekday).putBoolean(k+"hour24",hour24)
            .putInt(k+"clockType",clockType).putInt(k+"calendar",calendar)
            .putString(k+"title",title).putString(k+"subtitle",subtitle)
            .putBoolean(k+"saverDate",saverDate).putBoolean(k+"saverNight",saverNight)
            .putBoolean(k+"saverDrift",saverDrift).putInt(k+"saverBrightness",saverBrightness)
            .putInt(k+"numbersSize",numbersSize).putInt(k+"handWidth",handWidth).putInt(k+"handLength",handLength).putInt(k+"logoSize",logoSize)
            .putInt(k+"titleSize",titleSize).putInt(k+"subtitleSize",subtitleSize).putInt(k+"digitalSize",digitalSize).putInt(k+"dateSize",dateSize).putInt(k+"marksSize",marksSize)
            .putBoolean(k+"readableColors",readableColors).putBoolean(k+"highContrast",highContrast);
        for(String role:COLOR_ROLES){if(colors.containsKey(role))editor.putInt(k+"color."+role,colors.get(role));else editor.remove(k+"color."+role);}
        editor.apply();
    }
    static void delete(Context c,int id) {
        SharedPreferences p=c.getSharedPreferences("clocks",0); SharedPreferences.Editor e=p.edit();
        for(String k:p.getAll().keySet()) if(k.startsWith("w"+id+".")) e.remove(k);
        e.apply();
    }
    boolean light(Context c) { return theme==1; }
    static void migrateLegacyAppDefaults(Context c){
        SharedPreferences p=c.getSharedPreferences("clocks",Context.MODE_PRIVATE);
        if(p.getBoolean("legacyDefaultsMigrated",false))return;
        SharedPreferences.Editor e=p.edit();
        // Android's invalid widget ID is zero. Early builds could store app settings there.
        if(p.contains("w0.theme")&&!p.contains("default.theme")){
            for(java.util.Map.Entry<String,?> item:p.getAll().entrySet()){
                if(!item.getKey().startsWith("w0."))continue;
                String key="default."+item.getKey().substring(3);Object value=item.getValue();
                if(value instanceof String)e.putString(key,(String)value);
                else if(value instanceof Integer)e.putInt(key,(Integer)value);
                else if(value instanceof Boolean)e.putBoolean(key,(Boolean)value);
            }
        }
        e.putBoolean("legacyDefaultsMigrated",true).apply();
    }
    int ink(Context c) { return light(c)? Color.rgb(26,38,29):Color.rgb(237,243,233); }
    int surface(Context c) {return light(c)?Color.rgb(245,247,239):Color.rgb(16,23,18);}
    int background(Context c) {return light(c)?Color.rgb(231,236,225):Color.rgb(8,12,9);}
    int muted(Context c) {return light(c)?Color.rgb(91,106,92):Color.rgb(149,164,148);}
    static int clamp(int value,int min,int max){return Math.max(min,Math.min(max,value));}
    int rawColor(Context c,String role){
        if(role.equals("accent"))return accent;
        int fallback=switch(role){case "dial"->surface(c);case "numbers","hour"->ink(c);case "title","subtitle"->muted(c);default->accent;};
        return colors.getOrDefault(role,fallback);
    }
    void setColor(String role,int color){if(role.equals("accent"))accent=color;else colors.put(role,color|0xff000000);}
    void resetColor(String role){if(role.equals("accent"))accent=0xffbcf36a;else colors.remove(role);}
    int face(Context c){return rawColor(c,"dial");}
    int clockColor(Context c,String role){
        int raw=rawColor(c,role);
        if(!readableColors&&!highContrast)return raw;
        return AccessibleColors.ensure(raw,face(c),(role.equals("hour")||role.equals("minute")||role.equals("second")||role.equals("marks"))?3:4.5);
    }
    int actualOpacity(){return highContrast?100:opacity;}
    void resetSizes(){numbersSize=handWidth=handLength=logoSize=titleSize=subtitleSize=digitalSize=dateSize=marksSize=100;}
    void readablePreset(){
        numbersSize=145;handWidth=155;handLength=100;logoSize=90;titleSize=145;subtitleSize=145;digitalSize=125;dateSize=130;marksSize=140;
        highContrast=true;readableColors=true;opacity=100;
    }
}
