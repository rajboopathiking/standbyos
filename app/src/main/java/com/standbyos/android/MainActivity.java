package com.standbyos.android;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 42;
    private final String[] faces = {"Digital", "Flip", "Analog", "World", "Minimal", "Dashboard"};
    private final String[] widgets = {"Clock", "Calendar", "Battery", "Timer", "Photo", "World time"};
    private android.content.SharedPreferences prefs;
    private StandbyView view;
    private int face, leftWidget, rightWidget;
    private boolean redNight, chargingOnly, shiftPixels;
    private float dim;
    private long stopwatchStarted, stopwatchAccumulated;
    private boolean stopwatchRunning;
    private long timerUntil;
    private Bitmap photo;
    private int battery = -1;
    private boolean plugged;
    private final Handler handler = new Handler(android.os.Looper.getMainLooper());
    private final Runnable frame = new Runnable(){ public void run(){ if(view!=null) view.invalidate(); handler.postDelayed(this, 500); }};
    private final BroadcastReceiver powerReceiver = new BroadcastReceiver(){
        @Override public void onReceive(Context c, Intent i){ updateBattery(i); }
    };
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs = getSharedPreferences("standby", MODE_PRIVATE);
        face = prefs.getInt("face", 0); leftWidget = prefs.getInt("left", 0); rightWidget = prefs.getInt("right", 2);
        redNight = prefs.getBoolean("night", false); chargingOnly = prefs.getBoolean("chargingOnly", false);
        shiftPixels = prefs.getBoolean("shift", true); dim = prefs.getFloat("dim", 0.8f);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.BLACK); getWindow().setNavigationBarColor(Color.BLACK);
        view = new StandbyView(); setContentView(view); applyBrightness(); loadPhoto();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(powerReceiver, filter);
        handler.post(frame);
        immersive();
    }
    @Override protected void onDestroy(){ handler.removeCallbacks(frame); try{ unregisterReceiver(powerReceiver); }catch(Exception ignored){} super.onDestroy(); }
    @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c); view.invalidate();}
    @Override public void onWindowFocusChanged(boolean hasFocus){super.onWindowFocusChanged(hasFocus); if(hasFocus) immersive();}
    private void immersive(){getWindow().getDecorView().setSystemUiVisibility(5894 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);}
    private void updateBattery(Intent intent){
        if(intent==null)return;
        int level=intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1), scale=intent.getIntExtra(BatteryManager.EXTRA_SCALE,100);
        battery=scale>0 ? Math.round(100f*level/scale) : -1;
        plugged=intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0;
        if(view!=null)view.invalidate();
    }
    private void applyBrightness(){WindowManager.LayoutParams p=getWindow().getAttributes();p.screenBrightness=Math.max(0.02f, Math.min(1,dim));getWindow().setAttributes(p);}
    private void persist(){prefs.edit().putInt("face",face).putInt("left",leftWidget).putInt("right",rightWidget).putBoolean("night",redNight).putBoolean("chargingOnly",chargingOnly).putBoolean("shift",shiftPixels).putFloat("dim",dim).apply();}
    private void next(int delta){face=(face+delta+faces.length)%faces.length;persist();view.invalidate();}
    private void chooseWidget(boolean left){new AlertDialog.Builder(this).setTitle(left?"Left widget":"Right widget")
        .setSingleChoiceItems(widgets,left?leftWidget:rightWidget,(d,index)->{if(left)leftWidget=index;else rightWidget=index;persist();d.dismiss();view.invalidate(); if(index==4 && photo==null) pickImage();})
        .setNegativeButton("Cancel",null).show();}
    private void pickImage(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,PICK_IMAGE);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
        if(request==PICK_IMAGE && result==RESULT_OK && data!=null && data.getData()!=null){
            Uri uri=data.getData();
            try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
            prefs.edit().putString("photo",uri.toString()).apply(); loadPhoto();view.invalidate();
        }
    }
    private void loadPhoto(){String s=prefs.getString("photo",null);if(s==null)return;
        try{if(photo!=null){photo.recycle();photo=null;} BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;
            photo=BitmapFactory.decodeStream(getContentResolver().openInputStream(Uri.parse(s)),null,options);
        }catch(Exception ignored){photo=null;}
    }
    private void settings(){
        String[] actions={"Choose clock face", "Left dashboard widget", "Right dashboard widget", "Select photo", "Red night mode: "+(redNight?"ON":"OFF"),
            "Charging-only screen: "+(chargingOnly?"ON":"OFF"), "Burn-in shifting: "+(shiftPixels?"ON":"OFF"), "Screen brightness", "Set timer", "Reset stopwatch"};
        new AlertDialog.Builder(this).setTitle("StandbyOS • Settings").setItems(actions,(dialog,which)->{
            switch(which){
                case 0:new AlertDialog.Builder(this).setTitle("Clock style").setSingleChoiceItems(faces,face,(d,i)->{face=i;persist();d.dismiss();view.invalidate();}).setNegativeButton("Cancel",null).show();break;
                case 1:chooseWidget(true);break;
                case 2:chooseWidget(false);break;
                case 3:pickImage();break;
                case 4:redNight=!redNight;persist();view.invalidate();break;
                case 5:chargingOnly=!chargingOnly;persist();view.invalidate();break;
                case 6:shiftPixels=!shiftPixels;persist();view.invalidate();break;
                case 7:brightnessDialog();break;
                case 8:timerDialog();break;
                case 9:stopwatchRunning=false;stopwatchAccumulated=0;stopwatchStarted=0;view.invalidate();break;
            }
        }).setNegativeButton("Close",null).show();
    }
    private void brightnessDialog(){SeekBar bar=new SeekBar(this);bar.setMax(100);bar.setProgress(Math.round(dim*100));
        bar.setPadding(40,30,40,30);bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int progress,boolean from){dim=Math.max(.02f,progress/100f);applyBrightness();persist();}
            public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){}
        });new AlertDialog.Builder(this).setTitle("Brightness").setView(bar).setPositiveButton("Done",null).show();}
    private void timerDialog(){final String[] times={"1 minute","5 minutes","10 minutes","15 minutes","30 minutes","60 minutes","Stop timer"};
        new AlertDialog.Builder(this).setTitle("Start timer").setItems(times,(d,n)->{timerUntil=n==6?0:SystemClock.elapsedRealtime()+new long[]{1,5,10,15,30,60}[n]*60000L;view.invalidate();}).show();}
    private int primary(){return redNight?Color.rgb(255,52,36):Color.WHITE;}
    private int muted(){return redNight?Color.rgb(135,20,15):Color.rgb(158,162,175);}
    private static String now(String fmt){return new SimpleDateFormat(fmt,Locale.getDefault()).format(new Date());}
    private static String zoneTime(String id){SimpleDateFormat f=new SimpleDateFormat("h:mm a",Locale.US);f.setTimeZone(TimeZone.getTimeZone(id));return f.format(new Date());}
    private class StandbyView extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        private float downX,downY;
        private int w,h;
        StandbyView(){super(MainActivity.this);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        private void color(int c){p.setColor(c);p.setStyle(Paint.Style.FILL);p.setStrokeWidth(2);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));}
        private void txt(Canvas c,String text,float x,float y,float size,int color,boolean heavy,boolean centered){
            color(color);p.setTextSize(size);p.setTypeface(Typeface.create(heavy?"sans-serif-light":"sans-serif",heavy?Typeface.BOLD:Typeface.NORMAL));
            p.setTextAlign(centered?Paint.Align.CENTER:Paint.Align.LEFT);c.drawText(text,x,y,p);
        }
        private void rect(Canvas c,float l,float t,float r,float b,float radius,int color){color(color);c.drawRoundRect(l,t,r,b,radius,radius,p);}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);w=getWidth();h=getHeight();canvas.drawColor(Color.BLACK);
            if(chargingOnly&&!plugged){txt(canvas,"Connect charger to start StandBy",w/2f,h/2f,Math.max(18,h*.06f),primary(),false,true);return;}
            canvas.save();if(shiftPixels){int s=(int)(System.currentTimeMillis()/60000%4);canvas.translate((s%2==0?1:-1)*Math.min(4,w*.003f),(s<2?1:-1)*Math.min(4,h*.006f));}
            switch(face){case 0:digital(canvas);break;case 1:flip(canvas);break;case 2:analog(canvas);break;case 3:world(canvas);break;case 4:minimal(canvas);break;default:dashboard(canvas);}
            canvas.restore();
            txt(canvas,faces[face]+"  •  Swipe to change  •  Long press for settings",w/2f,h-15,Math.max(10,Math.min(14,h*.025f)),muted(),false,true);
            if(plugged)txt(canvas,"⚡",w-35,32,22,primary(),true,true);
        }
        private void digital(Canvas c){txt(c,now("h:mm"),w/2f,h*.57f,h*.50f,primary(),true,true);
            txt(c,now("a")+"    "+now("EEEE, MMM d"),w/2f,h*.79f,h*.075f,muted(),false,true);}
        private void flip(Canvas c){String[] parts=now("HH:mm").split(":");float cw=w*.28f,gap=w*.035f,start=w/2f-cw-gap/2f;
            rect(c,start-cw*.52f,h*.14f,start+cw*.52f,h*.78f,Math.min(22,h*.05f),redNight?0xff280603:0xff1b1b20);
            rect(c,start+gap+cw*.48f,h*.14f,start+gap+cw*1.52f,h*.78f,Math.min(22,h*.05f),redNight?0xff280603:0xff1b1b20);
            txt(c,parts[0],start,h*.65f,h*.48f,primary(),true,true);
            txt(c,parts[1],start+gap+cw,h*.65f,h*.48f,primary(),true,true);
            txt(c,now("EEE, MMM d"),w/2f,h*.88f,h*.05f,muted(),false,true);
        }
        private void analog(Canvas c){float cx=w*.5f,cy=h*.48f,r=h*.39f;color(muted());p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawCircle(cx,cy,r,p);
            for(int n=0;n<12;n++){double a=Math.PI*n/6-Math.PI/2;float x1=(float)(cx+Math.cos(a)*r*.86), y1=(float)(cy+Math.sin(a)*r*.86);
                float x2=(float)(cx+Math.cos(a)*r*.95),y2=(float)(cy+Math.sin(a)*r*.95);color(primary());p.setStrokeWidth(n%3==0?6:3);c.drawLine(x1,y1,x2,y2,p);}
            Calendar now=Calendar.getInstance();float mins=now.get(Calendar.MINUTE)+now.get(Calendar.SECOND)/60f;
            float hours=now.get(Calendar.HOUR)+mins/60f;hand(c,cx,cy,r*.51f,(hours/12f)*360,9);hand(c,cx,cy,r*.75f,mins*6,6);
            hand(c,cx,cy,r*.8f,now.get(Calendar.SECOND)*6,2);color(primary());c.drawCircle(cx,cy,7,p);
            txt(c,now("EEE, MMM d"),w/2f,h*.96f,h*.044f,muted(),false,true);
        }
        private void hand(Canvas c,float cx,float cy,float len,float degrees,float thickness){double a=Math.toRadians(degrees-90);color(primary());p.setStrokeWidth(thickness);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(cx,cy,(float)(cx+Math.cos(a)*len),(float)(cy+Math.sin(a)*len),p);}
        private void world(Canvas c){String[] cities={"New York", "London", "Tokyo", "India"};String[] tz={"America/New_York","Europe/London","Asia/Tokyo","Asia/Kolkata"};
            int cols=2;float top=h*.17f;
            for(int i=0;i<4;i++){float x=w*(i%cols==0?.27f:.73f),y=top+(i/cols)*h*.38f;
                txt(c,cities[i],x,y,h*.064f,muted(),false,true);txt(c,zoneTime(tz[i]),x,y+h*.16f,h*.12f,primary(),true,true);}
        }
        private void minimal(Canvas c){txt(c,now("HH:mm"),w/2f,h*.58f,h*.37f,primary(),false,true);
            txt(c,now("EEEE").toUpperCase(Locale.getDefault())+"   "+now("d MMMM"),w/2f,h*.75f,h*.05f,muted(),false,true);}
        private void dashboard(Canvas c){float pad=w*.035f,gap=w*.02f,top=h*.09f,bottom=h*.9f;
            float mid=w/2f;rect(c,pad,top,mid-gap/2,bottom,30,redNight?0xff170303:0xff16171b);
            rect(c,mid+gap/2,top,w-pad,bottom,30,redNight?0xff170303:0xff16171b);
            widget(c,leftWidget,pad,top,mid-gap/2,bottom);widget(c,rightWidget,mid+gap/2,top,w-pad,bottom);}
        private void widget(Canvas c,int kind,float l,float t,float r,float b){float cx=(l+r)/2f,cy=(t+b)/2f,ww=r-l,hh=b-t;
            switch(kind){
                case 0:txt(c,now("h:mm"),cx,cy+.04f*hh,Math.min(hh*.32f,ww*.24f),primary(),true,true);
                    txt(c,now("a"),cx,cy+hh*.19f,hh*.08f,muted(),false,true);break;
                case 1:txt(c,now("EEEE"),cx,t+hh*.32f,hh*.13f,primary(),true,true);
                    txt(c,now("d"),cx,t+hh*.65f,hh*.32f,primary(),true,true);
                    txt(c,now("MMMM yyyy"),cx,t+hh*.8f,hh*.075f,muted(),false,true);break;
                case 2:txt(c,"BATTERY",cx,t+hh*.24f,hh*.065f,muted(),false,true);
                    txt(c,battery<0?"--%":battery+"%",cx,t+hh*.59f,hh*.31f,primary(),true,true);
                    txt(c,plugged?"Charging":"On battery",cx,t+hh*.77f,hh*.077f,muted(),false,true);break;
                case 3:long elapsed=stopwatchAccumulated+(stopwatchRunning?SystemClock.elapsedRealtime()-stopwatchStarted:0);
                    long left=timerUntil==0?0:Math.max(0,timerUntil-SystemClock.elapsedRealtime());
                    txt(c,timerUntil!=0?"COUNTDOWN":"STOPWATCH",cx,t+hh*.27f,hh*.07f,muted(),false,true);
                    long shown=timerUntil!=0?left:elapsed;
                    txt(c,String.format(Locale.US,"%02d:%02d",shown/60000,(shown/1000)%60),cx,t+hh*.57f,hh*.19f,primary(),true,true);
                    txt(c,timerUntil!=0?"Long press → Set timer":"Tap widget → Start / Pause",cx,t+hh*.77f,hh*.055f,muted(),false,true);break;
                case 4:if(photo!=null){c.save();Path clip=new Path();clip.addRoundRect(l+8,t+8,r-8,b-8,22,22,Path.Direction.CW);c.clipPath(clip);
                    float scale=Math.max(ww/photo.getWidth(),hh/photo.getHeight());float dw=photo.getWidth()*scale,dh=photo.getHeight()*scale;
                    color(Color.WHITE);c.drawBitmap(photo,null,new RectF(cx-dw/2,cy-dh/2,cx+dw/2,cy+dh/2),p);c.restore();}
                    else{txt(c,"PHOTO",cx,cy,hh*.12f,primary(),true,true);txt(c,"Long press → Select photo",cx,cy+hh*.12f,hh*.05f,muted(),false,true);}break;
                case 5:txt(c,"WORLD TIME",cx,t+hh*.22f,hh*.065f,muted(),false,true);
                    txt(c,"London  "+zoneTime("Europe/London"),cx,t+hh*.43f,hh*.082f,primary(),false,true);
                    txt(c,"Tokyo   "+zoneTime("Asia/Tokyo"),cx,t+hh*.62f,hh*.082f,primary(),false,true);
                    txt(c,"NYC     "+zoneTime("America/New_York"),cx,t+hh*.81f,hh*.082f,primary(),false,true);break;
            }
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent event){switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:downX=event.getX();downY=event.getY();return true;
            case MotionEvent.ACTION_UP:float dx=event.getX()-downX,dy=event.getY()-downY;
                if(Math.abs(dx)>Math.max(65,w*.10f)&&Math.abs(dx)>Math.abs(dy)){next(dx<0?1:-1);return true;}
                if(face==5){int widget=event.getX()<w/2f?leftWidget:rightWidget;
                    if(widget==3){if(timerUntil!=0){timerUntil=0;}else if(stopwatchRunning){stopwatchAccumulated+=SystemClock.elapsedRealtime()-stopwatchStarted;stopwatchRunning=false;}
                        else{stopwatchStarted=SystemClock.elapsedRealtime();stopwatchRunning=true;}invalidate();return true;}}
                settings();return true;
        }return true;}
        @Override public boolean performClick(){super.performClick();return true;}
    }
}
