package jp.ac.agu.wil;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.CountDownTimer;
import android.util.Log;

import static android.bluetooth.BluetoothDevice.*;

public class BluetoothHandler extends MainActivity{
    /**
     * Bluetooth
     */
    String TAG="Bluetooth";
    /**
     * Bluetooth接続専用関数
     */
    private boolean mIsCountDownOn;
    private boolean mIsStarting;
    private boolean mIsOnHeadsetSco;
    private boolean mIsStarted;
    int activitiesCount; //アプリがバックグラウンド状態かそうでないか

    BluetoothHandler(BluetoothAdapter main_bluetoothAdapter,AudioManager main_audioManager, Context main_context){
        mBluetoothAdapter=main_bluetoothAdapter;
        mAudioManager=main_audioManager;
        mContext=main_context;
    }

    //Bluetooth接続を行う際に呼ぶ
    public boolean startBTConnetion(){
        Log.e(TAG,"startBTConnection: called");
        if(!mIsStarted){
            mIsStarted=true;
            Log.e(TAG,"startBTConnection: if state");
            mIsStarted=startBluetooth();
            if (!mIsStarted)
                Log.e(TAG,"mIsStarted is false!!");
            else
                Log.e(TAG,"mIsStarted is true");
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
        Log.d(TAG, "startBluetooth()");
        Log.d(TAG,"mBluetoothAdapter: "+mBluetoothAdapter);
        // Device support bluetooth
        if (mBluetoothAdapter != null) {
            Log.d(TAG,"mBluetoothAdapter is not null ");
            if (mAudioManager.isBluetoothScoAvailableOffCall()) {
                mContext.registerReceiver(mBroadcastReceiver,
                        new IntentFilter(ACTION_ACL_CONNECTED)); // Bluetoothデバイスが接続されたとき
                mContext.registerReceiver(mBroadcastReceiver,
                        new IntentFilter(ACTION_ACL_DISCONNECTED)); // 切断されたとき
                mContext.registerReceiver(mBroadcastReceiver,
                        new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)); // SCO stateが変わったとき

                // Need to set audio mode to MODE_IN_CALL for call to startBluetoothSco() to succeed.
                mAudioManager.setMode(AudioManager.MODE_IN_CALL); //SCOモードにする前に変更
                Log.d(TAG,"MODE_IN_CALL");
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
    BroadcastReceiver mBroadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG,"enter onReceive in mBroadcastReceiver");
            if (action.equals(ACTION_ACL_CONNECTED)){ //Bluetooth接続された！
                Log.d(TAG, "onReceive(): ACTION_ACL_CONNECTED");

                BluetoothDevice mConnectedHeadset = intent.getParcelableExtra(EXTRA_DEVICE);
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
            }else if (action.equals(ACTION_ACL_DISCONNECTED)){ //切断されたとき
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
                    Log.d(TAG,"EnableButton");
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
    private boolean isInForeground(){
        return activitiesCount>0;
    }
}
