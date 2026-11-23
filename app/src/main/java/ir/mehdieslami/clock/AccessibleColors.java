package ir.mehdieslami.clock;

import android.graphics.Color;

/** sRGB contrast helpers shared by the UI and user-customizable clock. */
final class AccessibleColors {
    private AccessibleColors(){}
    static double contrast(int a,int b){double x=Color.luminance(a|0xff000000),y=Color.luminance(b|0xff000000);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    static int textOn(int background){return contrast(Color.BLACK,background)>=contrast(Color.WHITE,background)?Color.BLACK:Color.WHITE;}
    static int ensure(int color,int background,double minimum){
        color|=0xff000000;if(contrast(color,background)>=minimum)return color;
        int target=textOn(background);float low=0,high=1;
        for(int i=0;i<20;i++){float f=(low+high)/2;int candidate=mix(color,target,f);if(contrast(candidate,background)>=minimum)high=f;else low=f;}
        return mix(color,target,high);
    }
    private static int mix(int a,int b,float n){return Color.rgb(Math.round(Color.red(a)*(1-n)+Color.red(b)*n),Math.round(Color.green(a)*(1-n)+Color.green(b)*n),Math.round(Color.blue(a)*(1-n)+Color.blue(b)*n));}
}
