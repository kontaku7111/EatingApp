package jp.ac.agu.wil;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    TextView DebugMessage;
    Thread m_thread=null;
    Record rec=null;
    String path;
    String fileName;
    int REQUEST_ENABLE_BT=1;
    String TAG="MainActivity";
    AudioManager audioManager=null;
    public String Date=null;
    private static final int REQUEST_EXTERNAL_STORAGE_CODE=1;
    private static String[] mPermissions={
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private static void verifyStoragePermissions(Activity activity){
        int readPermission = ContextCompat.checkSelfPermission(activity,mPermissions[0]);
        int writePermission=ContextCompat.checkSelfPermission(activity,mPermissions[1]);
        int microphonePermission=ContextCompat.checkSelfPermission(activity,mPermissions[2]);
        int bluetoothPermission=ContextCompat.checkSelfPermission(activity,mPermissions[3]);
        int accessCoarseLocationPermission=ContextCompat.checkSelfPermission(activity,mPermissions[5]);
        if(PackageManager.PERMISSION_GRANTED!=writePermission||
        PackageManager.PERMISSION_GRANTED!=readPermission||
        PackageManager.PERMISSION_GRANTED!=microphonePermission||
                PackageManager.PERMISSION_GRANTED!=accessCoarseLocationPermission
        ){
            ActivityCompat.requestPermissions(
                    activity,
                    mPermissions,
                    REQUEST_EXTERNAL_STORAGE_CODE
            );
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DebugMessage=findViewById(R.id.DebugMessage);
        //パス取得
        File path_storage=getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        path=path_storage.getAbsolutePath();
        Log.d(TAG,"onCreate(): path: "+path);
        rec=new Record();
        //permission変更
        MainActivity.verifyStoragePermissions(this);
        //Bluetooth無効化から有効化
        Intent reqEnableBTIntent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(reqEnableBTIntent,REQUEST_ENABLE_BT);
    }

    //Bluetooth無効化から有効化
    @Override
    protected void onActivityResult(int requestCode,int resultCode, Intent data){
        if (requestCode==REQUEST_ENABLE_BT){
            if (resultCode==RESULT_OK){
                Log.d(TAG,"onActivityResult(): Bluetooth is enabled");
            }
        }
    }

    /* control to make button enable or disable*/
    private void EnableButton(int id, boolean isEnable){
        ((Button)findViewById(id)).setEnabled(isEnable);
    }
    //スレッドを作る
    private void do_loopback(final boolean flag){
        Log.d(TAG, "do_loopback(): ====Thread start====");
        m_thread = new Thread(new Thread(new Runnable() {
            @Override
            public void run() {
                run_loop(flag);
            }
        }));
        m_thread.start();
    }
    //ループを実行する
    public void run_loop(boolean isRunning){
        // If Stop Button is pressed
        // Stopボタン押されたとき
        if(false==isRunning){
            Log.d(TAG,"run_loop(): ====Stop Button is pressed====");
            rec.stopAudioRecord();
            return;
        }
        // Initialize AudioRecord
        // AudioRecordの初期化
        rec.initAudioRecord(fileName);
        if(rec.audioRecord==null){
            Log.d(TAG,"run_loop(): ====Failed to initialize AudioRecord====");
            return;
        }
        if (AudioRecord.STATE_INITIALIZED==rec.audioRecord.getState()){
            rec.startAudioRecord();
            Log.d(TAG,"run_loop(): ====Recorder Started...====");
        }else{
            Log.d(TAG,"run_loop(): ====Initialization failed for AudioRecord====");
        }
        Log.d(TAG,"run_loop(): ====loopback exit====");
        return;
    }

    /* onClickListener*/
    public void BtnClick(View view) {
        switch (view.getId()) {
            case R.id.start_button:
                DebugMessage.setText("You push a Start button");
                //ボタン押せるように
                EnableButton(R.id.start_button,false);
                EnableButton(R.id.stop_button,true);
                // 日時取得
                DateFormat df=new SimpleDateFormat("yyyyMMddHHmmss");
                java.util.Date date = new Date(System.currentTimeMillis());
                Date=df.format(date);
                fileName=path+"/"+Date+".wav";
                Log.d(TAG,"BtnClick(): fileName: "+fileName);
                do_loopback(true);
                break;

            case R.id.stop_button:
                DebugMessage.setText("You push a Stop button");
                EnableButton(R.id.start_button,true);
                EnableButton(R.id.stop_button,false);
                do_loopback(false);
                break;

            default:
                break;
        }
    }
}
