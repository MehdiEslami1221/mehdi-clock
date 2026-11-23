package ir.mehdieslami.clock;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

final class Settings {
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
        s.zone=p.getString(k+"zone",""); return s;
    }
    void save(Context c,int id) {
        String k=id<0?"default.":"w"+id+".";
        c.getSharedPreferences("clocks",0).edit().putInt(k+"theme",theme).putInt(k+"accent",accent)
            .putInt(k+"style",style).putInt(k+"scale",scale).putInt(k+"opacity",opacity)
            .putBoolean(k+"seconds",seconds).putBoolean(k+"logo",logo).putBoolean(k+"digits",digits)
            .putBoolean(k+"date",date).putString(k+"zone",zone)
            .putBoolean(k+"weekday",weekday).putBoolean(k+"hour24",hour24)
            .putInt(k+"clockType",clockType).putInt(k+"calendar",calendar)
            .putString(k+"title",title).putString(k+"subtitle",subtitle)
            .putBoolean(k+"saverDate",saverDate).putBoolean(k+"saverNight",saverNight)
            .putBoolean(k+"saverDrift",saverDrift).putInt(k+"saverBrightness",saverBrightness).apply();
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
}
