package jp.ac.agu.wil;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.os.Environment.DIRECTORY_MUSIC;

public class MainActivity extends AppCompatActivity{
    TextView DebugMessage;
    Thread m_thread=null;
    Record rec=null;
    String path;
    String fileName;
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
    private boolean mIsCountDownOn;
    private boolean mIsStarting;
    private boolean mIsOnHeadsetSco;
    private boolean mIsStarted;
    Context mContext;
    BluetoothAdapter mBluetoothAdapter;

    private int activitiesCount; //アプリがバックグラウンド状態かそうでないか

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DebugMessage=findViewById(R.id.DebugMessage);
        EnableButton(R.id.start_button,false);
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
        activitiesCount=-1;
        mContext=this;
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
        Log.d(TAG,"FileName: "+fileName);
        rec.initAudioRecord(fileName,mAudioManager);
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
                rec=new Record();
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

    @Override
    protected void onResume() {
        super.onResume();
        if (activitiesCount++ == 0) { // on become foreground
            Log.d(TAG,"activitiesCount: "+activitiesCount);
            startBTConnetion();
        } else{
            activitiesCount=1;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (--activitiesCount == 0) { // on become background
            stopBTConnection();
        }
    }
    private boolean isInForeground(){
        return activitiesCount>0;
    }

    /**
     * Bluetooth
     */
    //Bluetooth接続を行う際に呼ぶ
    public boolean startBTConnetion(){
        Log.e(TAG,"startBTConnection: called");
        if(!mIsStarted){
            mIsStarted=true;
            Log.e(TAG,"startBTConnection: if state");
            mIsStarted=startBluetooth();
            Log.e(TAG,"startBTConnection: startBluetooth");
        }
        return mIsStarted;
    }
    //onDestroyまたはonResumeで呼ぶ
    public void stopBTConnection(){
        if(mIsStarted){
            mIsStarted=false;
            stopBluetooth();
        }
    }
    //bluetoothヘッドセット
    private boolean startBluetooth(){
        Log.d(TAG, "startBluetooth");

        // Device support bluetooth
        if (mBluetoothAdapter != null) {
            if (mAudioManager.isBluetoothScoAvailableOffCall()) {
                mContext.registerReceiver(mBroadcastReceiver,
                        new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED)); // Bluetoothデバイスが接続されたとき
                mContext.registerReceiver(mBroadcastReceiver,
                        new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED)); // 切断されたとき
                mContext.registerReceiver(mBroadcastReceiver,
                        new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)); // SCO stateが変わったとき

                // Need to set audio mode to MODE_IN_CALL for call to startBluetoothSco() to succeed.
                mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION); //SCOモードにする前に変更
                Log.d(TAG,"MODE_IN_COMMUNICATION");
                mIsCountDownOn = true;
                // mCountDown repeatedly tries to start bluetooth Sco audio connection.
                mCountDown.start(); //タイマースタート

                // need for audio sco, see mBroadcastReceiver
                mIsStarting = true;

                return true;
            }
        }
        return false;
    }
    // broadcast receiversをunregisterしSCO接続を止めカウントダウンをキャンセルする
    private void stopBluetooth() {
        Log.d(TAG, "stopBluetooth");

        if (mIsCountDownOn) {
            mIsCountDownOn = false;
            mCountDown.cancel();
        }

        // Need to stop Sco audio connection here when the app
        // change orientation or close with headset still turns on.
        mContext.unregisterReceiver(mBroadcastReceiver);
        mAudioManager.stopBluetoothSco();
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
    }
    //ヘッドセットとSCO接続状態を扱う
    private BroadcastReceiver mBroadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)){ //Bluetooth接続された！
                Log.d(TAG, "onReceive(): ACTION_ACL_CONNECTED");

                BluetoothDevice mConnectedHeadset = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                BluetoothClass bluetoothClass=mConnectedHeadset.getBluetoothClass();
                if(bluetoothClass != null){ //デバイスがヘッドセットか
                    // Check if device is a headset. Besides the 2 below, are there other
                    // device classes also qualified as headset?
                    int deviceClass = bluetoothClass.getDeviceClass();
                    Log.d(TAG,"deviceClass = "+ deviceClass);
                    if (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE ||deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET) {
                        // start bluetooth Sco audio connection.
                        // Calling startBluetoothSco() always returns faIL here,
                        // that why a count down timer is implemented to call
                        // startBluetoothSco() in the onTick.
                        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
                        //MODE_IN_CALLにしなければ骨伝導音取得不可
                        Log.d(TAG,"MODE_IN_CALL");
                        mIsCountDownOn = true;
                        mCountDown.start();
                        onHeadsetConnected();
                    }
                }
                Log.d(TAG, mConnectedHeadset.getName() + " connected");
            }else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)){ //切断されたとき
                Log.d(TAG,"onReceive(): ACTION_ACL_DISCONNECTED");

                if (mIsCountDownOn) {
                    mIsCountDownOn = false;
                    mCountDown.cancel();
                }

                mAudioManager.setMode(AudioManager.MODE_NORMAL); //ノーマルに戻す
                // onHeadsetDisconnected()の場所
            }else if (action.equals(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)){ // SCOの状態が更新されたとき
                int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE,AudioManager.SCO_AUDIO_STATE_ERROR);

                if(state == AudioManager.SCO_AUDIO_STATE_CONNECTED){ // SCOに接続されたとき
                    mIsOnHeadsetSco =true;
                    EnableButton(R.id.start_button,true);
                    EnableButton(R.id.stop_button,false);
                    if(mIsStarting) {
                        // アプリケーションが開始する前にデバイスが接続されたとき
                        // ACTION_ACL_CONNECTEDが受け取れないので、
                        mIsStarting = false;
                        onHeadsetConnected();
                    }
                    // サンプルではここはonScoAudioConnected();
                    if(mIsCountDownOn){
                        mIsCountDownOn = false;
                        mCountDown.cancel();
                    }
                    // onScoAudioConnected()の場所

                    Log.d(TAG,"onReceive(): SCO connected!");
                }else if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED){ //SCO切断時
                    Log.d(TAG,"onReceive(): SCO disconnected!");
                    // startBluetooth()を呼んだら、常にSCO_AUDIO_STATE_DISCONNECTEDを受け取る
                    // which at that stage we do not want to do anything. Thus the if condition.
                    if (!mIsStarting) {
                        mIsOnHeadsetSco = false;

                        // Need to call stopBluetoothSco(), otherwise startBluetoothSco()
                        // will not be successful.
                        mAudioManager.stopBluetoothSco();
                        onScoAudioDisconnected();
                    }
                }
            }
        }
    };

    private CountDownTimer mCountDown = new CountDownTimer(10000,1000) { //カウントダウン１０秒、インターバル１秒
        @Override
        public void onTick(long millisUntilFinished) {
            mAudioManager.startBluetoothSco(); //SCOモードへ
            Log.d(TAG,"\nonTick start bluetooth SCO");
        }

        @Override
        public void onFinish() {
            mIsCountDownOn = false;
            mAudioManager.setMode(AudioManager.MODE_NORMAL); //モードをノーマルにする

            Log.d(TAG,"\nonFinish fail to connect to headset audio");
        }
    };

    public void onHeadsetConnected() {
        Log.d(TAG, "Bluetooth headset connected");

        if (isInForeground() && !mIsOnHeadsetSco) {
            startBTConnetion();
        }
    }

    public void onScoAudioDisconnected() {
        Log.d(TAG, "Bluetooth sco audio finished");
        stopBTConnection();

        if (isInForeground()) {
            startBTConnetion();
        }
    }
}
