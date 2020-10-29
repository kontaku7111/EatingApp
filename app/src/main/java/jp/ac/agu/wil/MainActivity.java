package jp.ac.agu.wil;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.os.Environment.DIRECTORY_MUSIC;

public class MainActivity extends AppCompatActivity{
    public static TextView DebugMessage;
    // 他クラスでも利用できるようにstatic付与
    public static TextView chewCount;
    public static int total_chewingCount;
    public static int bite_chewingCount;
    public static boolean isTotal;
    Thread m_thread=null;
    Record rec=null;
    public static String path;
    AudioManager mAudioManager;
    int REQUEST_ENABLE_BT=1;
    String TAG="MainActivity";
    public String Date=null;

    // Permission関連
    private static final int REQUEST_EXTERNAL_STORAGE_CODE=1;
    private static String[] mPermissions={
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    /**
     * Bluetooth接続専用関数
     */
    Context mContext;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothHandler bluetoothHandler;

    private int activitiesCount; //アプリがバックグラウンド状態かそうでないか

    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
        setContentView(R.layout.layout_ja);
        DebugMessage=findViewById(R.id.DebugMessage);
        chewCount=findViewById(R.id.chewing_count);
        EnableButton(R.id.start_button,true);
        EnableButton(R.id.stop_button,false);
        //パス取得
        File path_storage=getExternalFilesDir(DIRECTORY_MUSIC);
        path=path_storage.getAbsolutePath();
        Log.d(TAG,"onCreate(): path: "+path);
        //permission変更
        MainActivity.verifyPermissions(this);
        //Bluetooth無効化から有効化
        Intent reqEnableBTIntent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(reqEnableBTIntent,REQUEST_ENABLE_BT);
        //Bluetooth接続
        mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter(); //デバイスの有無を確認
        mAudioManager=(AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        Log.d(TAG,"onCreate: ====set bluetooth====");
        mContext=this;
        bluetoothHandler=new BluetoothHandler(mBluetoothAdapter,mAudioManager,mContext);
        bluetoothHandler.activitiesCount=-1;
        bite_chewingCount=0;
        total_chewingCount=0;
        isTotal=false;

    }

    private static void verifyPermissions(Activity activity){
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

    //Bluetooth無効化から有効化されているか確認（onCreateで有効化している）
    @Override
    protected void onActivityResult(int requestCode,int resultCode, Intent data){
        Log.d(TAG,"onActivityResult: ====enter onActivityResult!====");
        if (requestCode==REQUEST_ENABLE_BT){
            if (resultCode==RESULT_OK){
                Log.d(TAG,"onActivityResult: Bluetooth is enabled");
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
                try {
                    run_loop(flag);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));
        m_thread.start();
    }
    //ループを実行する
    public void run_loop(boolean isRunning) throws IOException {
        // If Stop Button is pressed
        // Stopボタン押されたとき
        if(false==isRunning){
            Log.d(TAG,"run_loop(): ====Stop Button is pressed====");
            rec.stopAudioRecord();
            return;
        }
        // Initialize AudioRecord
        // AudioRecordの初期化
        rec.initAudioRecord(path, Date, mAudioManager);
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
//                DebugMessage.setText("You push a Start button");
                DebugMessage.setText("計測開始");
                //ボタン押せるように
                EnableButton(R.id.start_button,false);
                EnableButton(R.id.stop_button,true);
                // 日時取得
                DateFormat df=new SimpleDateFormat("yyyyMMddHHmmss");
                java.util.Date date = new Date(System.currentTimeMillis());
                Date=df.format(date);
                //音声ファイルとcsvファイルを保存するディレクトリ作成
                File dir=new File(path+"/"+Date);
                if(!dir.exists()){
                    if(dir.mkdirs()){
                        Log.d(TAG,"Succeed in creating a folder");
                    }else{
                        Log.d(TAG,"Fail to create a folder");
                    }
                }
                rec=new Record(mContext);
                do_loopback(true);
                break;

            case R.id.stop_button:
                DebugMessage.setText("Y計測終了");
                EnableButton(R.id.start_button,true);
                EnableButton(R.id.stop_button,false);
                do_loopback(false);
                break;
            case R.id.bite_button:
                DebugMessage.setText("Display chewing count every bite");
                isTotal=false;
//                chewCount.setText("Chew count (Bite): "+MainActivity.bite_chewingCount);
                chewCount.setText("咀嚼回数: "+MainActivity.bite_chewingCount);
                break;
            case R.id.total_button:
                DebugMessage.setText("Display chewing count in total");
                isTotal=true;
                chewCount.setText("Chew count (Total): "+MainActivity.total_chewingCount);
                break;
            case R.id.reset_button:
                DebugMessage.setText("Reset chewing count in total");
                total_chewingCount=0;
//                chewCount.setText("Chew count (Total): "+MainActivity.total_chewingCount);
                chewCount.setText("咀嚼回数: "+MainActivity.total_chewingCount);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (activitiesCount++ == 0) { // on become foreground
            Log.d(TAG,"activitiesCount: "+activitiesCount);
            bluetoothHandler.startBTConnetion();
        } else{
            activitiesCount=1;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (--activitiesCount == 0) { // on become background
            bluetoothHandler.stopBTConnection();
        }
    }
}
