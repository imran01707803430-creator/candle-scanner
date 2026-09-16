package com.example.signalscanner;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;

public class CaptureService extends Service {
    public static final String ACTION_RESULT="com.example.signalscanner.RESULT";
    public static final String EXTRA_PATTERN="pattern";
    public static final String EXTRA_SIGNAL="signal";
    public static final String EXTRA_CONFIDENCE="confidence";
    public static final String EXTRA_DETAILS="details";
    private MediaProjection projection; private VirtualDisplay display; private ImageReader reader;
    private int width,height,density;

    @Override public void onCreate(){ super.onCreate(); createChannel(); }

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        startForeground(7, buildNotification());
        int result=intent.getIntExtra("resultCode",0);
        Intent data=intent.getParcelableExtra("data");
        width=intent.getIntExtra("width",1080); height=intent.getIntExtra("height",1920); density=intent.getIntExtra("density",1);
        MediaProjectionManager pm=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        projection=pm.getMediaProjection(result,data);
        reader=ImageReader.newInstance(width,height, PixelFormat.RGBA_8888,2);
        reader.setOnImageAvailableListener(r->{
            Image img=null;
            try{ img=r.acquireLatestImage(); if(img==null)return; Bitmap bmp=ImageUtils.imageToBitmap(img,width,height); if(bmp!=null){ PatternAnalyzer.Result x=PatternAnalyzer.analyze(bmp); sendResult(x); }}
            finally{ if(img!=null)img.close(); stopSelf(); }
        },new Handler(Looper.getMainLooper()));
        display=projection.createVirtualDisplay("CandleSignalScanner",width,height,density,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,null);
        return START_NOT_STICKY;
    }
    private void sendResult(PatternAnalyzer.Result r){ Intent i=new Intent(ACTION_RESULT); i.setPackage(getPackageName()); i.putExtra(EXTRA_PATTERN,r.pattern);i.putExtra(EXTRA_SIGNAL,r.signal);i.putExtra(EXTRA_CONFIDENCE,r.confidence);i.putExtra(EXTRA_DETAILS,r.details);sendBroadcast(i); }
    private void createChannel(){ if(Build.VERSION.SDK_INT>=26){ NotificationChannel c=new NotificationChannel("scan","Chart Scanner",NotificationManager.IMPORTANCE_LOW); ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c); }}
    private Notification buildNotification(){ if(Build.VERSION.SDK_INT>=26) return new Notification.Builder(this,"scan").setContentTitle("Candle Signal Scanner").setContentText("চার্ট স্ক্যান চলছে…").setSmallIcon(android.R.drawable.ic_menu_view).build(); return new Notification.Builder(this).setContentTitle("Candle Signal Scanner").setContentText("চার্ট স্ক্যান চলছে…").setSmallIcon(android.R.drawable.ic_menu_view).build(); }
    @Override public void onDestroy(){ if(display!=null)display.release(); if(reader!=null)reader.close(); if(projection!=null)projection.stop(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent){ return null; }
}
