package com.example.signalscanner;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int REQ_CAPTURE=1001;
    private android.media.projection.MediaProjectionManager projectionManager;
    private int width,height,density;
    private TextView status,pattern,signal,confidence,details;
    private final BroadcastReceiver receiver=new BroadcastReceiver(){ @Override public void onReceive(Context c,Intent i){
        pattern.setText(i.getStringExtra(CaptureService.EXTRA_PATTERN));
        signal.setText(i.getStringExtra(CaptureService.EXTRA_SIGNAL));
        confidence.setText("Confidence: "+i.getStringExtra(CaptureService.EXTRA_CONFIDENCE));
        details.setText(i.getStringExtra(CaptureService.EXTRA_DETAILS));
        status.setText("স্ক্যান সম্পন্ন");
    }};
    @Override protected void onCreate(Bundle b){ super.onCreate(b); setContentView(R.layout.activity_main);
        status=findViewById(R.id.status); pattern=findViewById(R.id.pattern); signal=findViewById(R.id.signal); confidence=findViewById(R.id.confidence); details=findViewById(R.id.details);
        Button scan=findViewById(R.id.scanButton); projectionManager=(android.media.projection.MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        WindowManager wm=(WindowManager)getSystemService(WINDOW_SERVICE); android.util.DisplayMetrics dm=new android.util.DisplayMetrics(); wm.getDefaultDisplay().getRealMetrics(dm); width=dm.widthPixels;height=dm.heightPixels;density=dm.densityDpi;
        scan.setOnClickListener(v->requestCapture());
    }
    @Override protected void onResume(){ super.onResume(); registerReceiver(receiver,new IntentFilter(CaptureService.ACTION_RESULT),Context.RECEIVER_NOT_EXPORTED); }
    @Override protected void onPause(){ unregisterReceiver(receiver); super.onPause(); }
    private void requestCapture(){ status.setText("স্ক্রিন শেয়ার অনুমতি চাইছে…"); startActivityForResult(projectionManager.createScreenCaptureIntent(),REQ_CAPTURE); }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){ super.onActivityResult(requestCode,resultCode,data); if(requestCode!=REQ_CAPTURE)return; if(resultCode!=RESULT_OK||data==null){status.setText("অনুমতি দেওয়া হয়নি");return;}
        Intent s=new Intent(this,CaptureService.class); s.putExtra("resultCode",resultCode);s.putExtra("data",data);s.putExtra("width",width);s.putExtra("height",height);s.putExtra("density",density);
        if(android.os.Build.VERSION.SDK_INT>=26) startForegroundService(s); else startService(s); status.setText("চার্ট স্ক্যান হচ্ছে…");
    }
}
