package com.example.signalscanner;

import android.graphics.Bitmap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Rule-based screen analyzer. Pattern names/rule directions are based on the
 * supplied scanned book pages. Image detection itself is heuristic.
 */
public final class PatternAnalyzer {
    public static class Result {
        public String pattern, signal, confidence, details;
        Result(String p, String s, String c, String d) { pattern=p; signal=s; confidence=c; details=d; }
    }

    static class Candle {
        int x, top, bottom, bodyTop, bodyBottom;
        boolean green;
        float body, upper, lower;
    }

    public static Result analyze(Bitmap b) {
        List<Candle> cs = detectCandles(b);
        if (cs.size() < 2) {
            return new Result("চার্ট শনাক্ত হয়নি", "WAIT", "LOW",
                    "চার্টের candle পরিষ্কারভাবে ধরা যায়নি। চার্টটি বড় করে আবার SCAN করুন।");
        }

        // First check multi-candle rules confirmed in the supplied candlestick pages.
        Result multi = detectMultiCandle(cs);
        if (multi != null) return multi;

        Candle c = cs.get(cs.size()-1);
        float range = Math.max(1, c.bottom-c.top);
        float bodyRatio = c.body / range;

        if (bodyRatio < 0.12f && c.lower > range*0.55f && c.upper < range*0.18f)
            return result("Dragonfly Doji", "UP", "MEDIUM", "দীর্ঘ lower shadow ও খুব ছোট body—Dragonfly Doji-এর আনুমানিক গঠন।");
        if (bodyRatio < 0.14f && c.upper > range*0.55f && c.lower < range*0.18f)
            return result(c.green ? "Inverted Hammer" : "Shooting Star", c.green ? "UP" : "DOWN", "MEDIUM",
                    "লম্বা upper shadow ও ছোট body—বইয়ের দেখানো Inverted Hammer/Shooting Star-এর গঠন।");
        if (bodyRatio < 0.18f && c.lower > c.body*2.5f && c.upper < c.body*1.2f)
            return result("Hammer", "UP", "MEDIUM", "লম্বা lower shadow ও ছোট body—Hammer-এর আনুমানিক গঠন।");
        if (bodyRatio < 0.15f)
            return result("Doji", "WAIT", "MEDIUM", "Body খুব ছোট—Doji-এর আনুমানিক গঠন; একা এটিকে trade entry হিসেবে ব্যবহার করবেন না।");

        // Trend structure from the supplied book: HH/HL vs LH/LL.
        int n = cs.size();
        int up = 0, down = 0;
        for (int i=Math.max(1,n-7); i<n; i++) {
            if (cs.get(i).green) up++; else down++;
        }
        String trend = up > down+2 ? "recent candles bullish" : down > up+2 ? "recent candles bearish" : "mixed";
        return result("No confirmed book pattern", "WAIT", "LOW",
                "শেষে " + cs.size() + "টি candle ধরা হয়েছে। Trend: " + trend + ". বইয়ের pattern-এর পরিষ্কার confirmation পাওয়া যায়নি।");
    }

    private static Result detectMultiCandle(List<Candle> c) {
        int n=c.size();
        if(n<3) return null;
        Candle a=c.get(n-3), b=c.get(n-2), d=c.get(n-1);
        float ar=range(a), br=range(b), dr=range(d);
        float ab=a.body/ar, bb=b.body/br, db=d.body/dr;

        // Three rising/falling methods: strong candle, 3 small counter candles,
        // then continuation candle. This follows the supplied book's continuation direction.
        if (a.green && ab>0.55f && !b.green && !d.green && db>0.55f &&
                b.body<br*0.55f && d.top < a.top && d.bottom > a.bottom)
            return result("Falling Three Methods (approx)", "DOWN", "LOW",
                    "বইয়ের continuation pattern-এর মতো 3-candle structure আংশিক মিলেছে। আরও candles/context প্রয়োজন।");
        if (!a.green && ab>0.55f && b.green && d.green && db>0.55f &&
                b.body<br*0.55f && d.top < a.top && d.bottom > a.bottom)
            return result("Rising Three Methods (approx)", "UP", "LOW",
                    "বইয়ের continuation pattern-এর মতো 3-candle structure আংশিক মিলেছে। আরও candles/context প্রয়োজন।");

        // One Black Crow / One White Soldier are explicitly listed in the supplied pages.
        if (!a.green && b.green && d.green && bodyNearTop(d) && b.body>br*0.55f)
            return result("One White Soldier (possible)", "UP", "LOW",
                    "শেষে bullish candle sequence পাওয়া গেছে; বইয়ের One White Soldier-এর সাথে আংশিক মিল।");
        if (a.green && !b.green && !d.green && bodyNearBottom(d) && b.body>br*0.55f)
            return result("One Black Crow (possible)", "DOWN", "LOW",
                    "শেষে bearish candle sequence পাওয়া গেছে; বইয়ের One Black Crow-এর সাথে আংশিক মিল।");

        // Three-candle reversal stars: only report when the middle body is much smaller.
        if (!a.green && ab>0.5f && bb<0.25f && d.green && db>0.5f && d.bodyTop < b.bodyBottom)
            return result("Morning Star (possible)", "UP", "LOW",
                    "৩-candle reversal structure আংশিক মিলেছে; বইয়ের Morning Star-এর confirmation হিসেবে ব্যবহার করতে context দরকার।");
        if (a.green && ab>0.5f && bb<0.25f && !d.green && db>0.5f && d.bodyBottom > b.bodyTop)
            return result("Evening Star (possible)", "DOWN", "LOW",
                    "৩-candle reversal structure আংশিক মিলেছে; বইয়ের Evening Star-এর confirmation হিসেবে ব্যবহার করতে context দরকার।");

        // Engulfing approximation.
        if (!b.green && d.green && d.bodyTop <= b.bodyTop && d.bodyBottom >= b.bodyBottom)
            return result("Bullish Engulfing (possible)", "UP", "LOW",
                    "শেষ bullish body আগের bearish body-কে ঢেকেছে—Bullish Engulfing-এর আনুমানিক গঠন।");
        if (b.green && !d.green && d.bodyTop <= b.bodyTop && d.bodyBottom >= b.bodyBottom)
            return result("Bearish Engulfing (possible)", "DOWN", "LOW",
                    "শেষ bearish body আগের bullish body-কে ঢেকেছে—Bearish Engulfing-এর আনুমানিক গঠন।");
        return null;
    }

    private static float range(Candle c){ return Math.max(1, c.bottom-c.top); }
    private static boolean bodyNearTop(Candle c){ return c.bodyTop-c.top < range(c)*0.22f; }
    private static boolean bodyNearBottom(Candle c){ return c.bottom-c.bodyBottom < range(c)*0.22f; }

    private static Result result(String p,String s,String conf,String d){
        return new Result(p,s,conf,d+"\nনোট: এটি screen-image heuristic; নিশ্চিত ভবিষ্যৎ price prediction নয়।");
    }

    private static List<Candle> detectCandles(Bitmap b) {
        int w=b.getWidth(), h=b.getHeight();
        int left=(int)(w*0.02), right=(int)(w*0.88), top=(int)(h*0.12), bottom=(int)(h*0.88);
        List<Candle> cs=new ArrayList<>();
        int runStart=-1, lastX=-1;
        for(int x=left;x<right;x++){
            int hits=0;
            for(int y=top;y<bottom;y+=2){
                int c=b.getPixel(x,y),R=(c>>16)&255,G=(c>>8)&255,B=c&255;
                if(isGreen(R,G,B)||isRed(R,G,B)) hits++;
            }
            if(hits>=2){ if(runStart<0) runStart=x; lastX=x; }
            else if(runStart>=0){ if(lastX-runStart>=1){ Candle c=makeCandle(b,(runStart+lastX)/2,runStart,lastX,top,bottom); if(c!=null) cs.add(c); } runStart=-1; }
        }
        if(runStart>=0 && lastX-runStart>=1){ Candle c=makeCandle(b,(runStart+lastX)/2,runStart,lastX,top,bottom); if(c!=null) cs.add(c); }
        return cs;
    }

    private static boolean isGreen(int R,int G,int B){ return G>90 && G>R*1.18f && G>B*0.85f; }
    private static boolean isRed(int R,int G,int B){ return R>115 && R>G*1.18f && R>B*1.05f; }

    private static Candle makeCandle(Bitmap b,int x,int x1,int x2,int top,int bottom){
        int t=bottom, bo=top, green=0, red=0;
        for(int xx=x1;xx<=x2;xx++) for(int y=top;y<bottom;y++){
            int col=b.getPixel(xx,y),R=(col>>16)&255,G=(col>>8)&255,B=col&255;
            if(isGreen(R,G,B)||isRed(R,G,B)){ t=Math.min(t,y); bo=Math.max(bo,y); if(isGreen(R,G,B))green++; else red++; }
        }
        if(bo<=t) return null;
        Candle c=new Candle(); c.x=x;c.top=t;c.bottom=bo;c.green=green>=red;
        int bt=bo, bb=t;
        for(int y=t;y<=bo;y++){
            int count=0;
            for(int xx=x1;xx<=x2;xx++){
                int col=b.getPixel(xx,y),R=(col>>16)&255,G=(col>>8)&255,B=col&255;
                if(isGreen(R,G,B)||isRed(R,G,B)) count++;
            }
            if(count>=Math.max(2,(x2-x1)/2)){bt=Math.min(bt,y);bb=Math.max(bb,y);}
        }
        c.bodyTop=bt;c.bodyBottom=bb;c.body=Math.max(1,bb-bt);
        c.upper=Math.max(0,bt-t);c.lower=Math.max(0,bo-bb);
        return c;
    }
}
