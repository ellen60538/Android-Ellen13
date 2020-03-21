package tw.org.iii.ellen.ellen13;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private ConnectivityManager cmgr ;
    private MyReceiver myReceiver ;
    private TextView mesg ;
    private ImageView img ;
    private boolean isAllowSDcard ;
    private File downloadDir ;
    private ProgressDialog progressDialog ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    123);

        }else {
            isAllowSDcard = true ;
            init() ;
        }

    }

    private void init(){
        if (isAllowSDcard){
            downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) ;
        }

        progressDialog = new ProgressDialog(this) ;
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER) ;
        progressDialog.setMessage("Downloading .....");

        img = findViewById(R.id.img) ;
        mesg = findViewById(R.id.mesg) ;

        cmgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE) ;
        myReceiver = new MyReceiver() ; //若有多個activity,應該在onStart掛上, onPause時死掉
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION) ;
        //filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION) ;   如有多個要過濾的action可用add加上
        filter.addAction("ellen") ;
        registerReceiver(myReceiver, filter) ;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            isAllowSDcard = true ;
        }else {
            isAllowSDcard = false ;
        }
        init() ;
    }

    @Override
    public void finish() {
        unregisterReceiver(myReceiver) ;
        super.finish();
    }

    private boolean isConnectNetwork() {
        NetworkInfo networkInfo = cmgr.getActiveNetworkInfo(); //如果是null 就是沒網路連線
        return networkInfo != null && networkInfo.isConnectedOrConnecting() ;
    }

    private boolean is4GConnected(){
        NetworkInfo networkInfo = cmgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE) ;
        //ConnectivityManager.TYPE_WIFI
        return networkInfo.isConnectedOrConnecting() ;
    }

    public void test1(View view) {
        Log.v("ellen","isNetwork = " + isConnectNetwork()) ;

    }

    public void test2(View view) {
        Log.v("ellen","is4G = " + is4GConnected()) ;

    }

    public void test3(View view) {

        new Thread(){
            @Override
            public void run() {
                try {
                    URL url = new URL("https://bradchao.com/wp");  // http => Android 8+ usesCleartextTraffic 使用明碼傳送
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection() ;
                    conn.connect() ;


                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream())) ;
                    String line ;
                    StringBuffer sb = new StringBuffer() ;
                    while ( (line = reader.readLine()) != null ){
                        sb.append(line + "\n") ;
                    }

                    reader.close() ;

                    Intent intent = new Intent("ellen") ;
                    intent.putExtra("data",sb.toString()) ;
                    sendBroadcast(intent) ; // Context => Activity, Service, Application

                }catch (Exception e){
                    Log.v("ellen",e.toString()) ;
                }
            }
        }.start() ;


    }

    public void test4(View view){
        new Thread(){
            @Override
            public void run() {
                fetchImage() ;
            }
        }.start();
    }

    public void test5(View view) {

        if (!isAllowSDcard){
            //沒有允許存取
            return ;
        }

        progressDialog.show() ;

        new Thread(){
            @Override
            public void run() {
                fetchPDF() ;
            }
        }.start();

    }

    private void fetchPDF(){
        try{
            URL url = new URL("https://pdfmyurl.com/?url=https://www.gamer.com.tw") ;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection() ;
            conn.connect() ;

            File downloadFile = new File(downloadDir,"gamer.pdf") ;
            FileOutputStream fout = new FileOutputStream(downloadFile) ;

            byte[] buf = new byte[4096*1024] ;
            BufferedInputStream bin = new BufferedInputStream(conn.getInputStream()) ;
            int len = -1 ;
            while ( (len = bin.read(buf)) != -1 ){
                fout.write(buf, 0 ,len) ;
            }


            bin.close() ;
            fout.flush() ;
            fout.close() ;
            uiHandler.sendEmptyMessage(2) ;

            Log.v("ellen","save OK") ;

        }catch (Exception e){
            Log.v("ellen",e.toString()) ;
        }finally {
            uiHandler.sendEmptyMessage(1) ;
        }
    }

    public void test6(View view){
        Intent intent = new Intent(Intent.ACTION_SEND) ;
        intent.setType("text/plain") ;
        intent.putExtra(Intent.EXTRA_SUBJECT,"Sharing URL") ;
        intent.putExtra(Intent.EXTRA_TEXT,"http://www.url.com") ;
        startActivity(Intent.createChooser(intent,"Share URL"));
    }




    private Bitmap bmp ;
    private void fetchImage(){
        try{
            URL url = new URL("https://d17fnq9dkz9hgj.cloudfront.net/breed-uploads/2018/09/dog-landing-hero-lg.jpg?bust=1536935129&width=1080") ;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection() ;
            conn.connect() ;

            bmp = BitmapFactory.decodeStream(conn.getInputStream()) ;
            uiHandler.sendEmptyMessage(0) ;

        }catch (Exception e){

        }
    }


    private UIHandler uiHandler = new UIHandler() ;

    private class UIHandler extends Handler{
        @Override
        public void handleMessage(@NonNull Message msg) {

            super.handleMessage(msg);
            if (msg.what == 0) img.setImageBitmap(bmp) ;
            if (msg.what == 1) progressDialog.dismiss() ;
            if (msg.what == 2) showPDF() ;

        }
    }

    private void showPDF(){
        File file = new File(downloadDir, "gamer.pdf") ;
        Uri pdfuri = FileProvider.getUriForFile(
                this,getPackageName()+".provider",file) ;

        Intent intent = new Intent(Intent.ACTION_VIEW) ;
        intent.setDataAndType(pdfuri, "application/pdf") ;
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY) ;
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) ;
        startActivity(intent) ;
    }

    //廣播接收器
    private class MyReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v("ellen","onReceive") ;

            if ( intent.getAction().equals("ellen") ){
                String data = intent.getStringExtra("data") ;
                mesg.setText(data) ;
            }else if ( intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION) ){
                test1(null) ;
            }
        }
    }













}
