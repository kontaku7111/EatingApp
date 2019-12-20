package jp.ac.agu.wil;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.util.Log;

public class Record {
    public AudioRecord audioRecord=null;
    private static final int SAMPLING_RATE=8000;
    private WaveFile wav=new WaveFile();
    private int bufferSize;//オーディオレコード用バッファのサイズ
    private short[] shortBuf; //オーディオレコード用バッファ
    String TAG="Record";

    public void initAudioRecord(String fileName, AudioManager audioManager){
        wav=new WaveFile();
        wav.createFile(fileName);
        //AudioRecordオブジェクトを作成
        //バッファサイズを計算
        bufferSize = AudioRecord.getMinBufferSize(
                SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        //インスタンス生成
        audioRecord = new AudioRecord(
                //MediaRecorder.AudioSource.MIC,
                audioManager.STREAM_VOICE_CALL,
                SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        shortBuf = new short[bufferSize/2];

        //コールバックを指定
        audioRecord.setRecordPositionUpdateListener(new AudioRecord.OnRecordPositionUpdateListener(){
            @Override
            public void onMarkerReached(AudioRecord recorder) {
                //TODO Auto-generated method stub
            }
            //フレームごとの処理
            @Override
            public void onPeriodicNotification(AudioRecord recorder) {
                //.d(TAG, "録音コールバック");
                audioRecord.read(shortBuf, 0, bufferSize/2 );
                wav.addBigEndianData(shortBuf);
            }
        });
        // コールバックが呼ばれる間隔を指定
        audioRecord.setPositionNotificationPeriod(bufferSize / 2);
    }

    public void startAudioRecord(){
        Log.d(TAG,"startAudioRecord(): start recording");
        audioRecord.startRecording();
        audioRecord.read(shortBuf,0,bufferSize/2);
    }
    //オーディオレコードを停止する
    public void stopAudioRecord(){
        Log.d(TAG,"stopAudioRecord(): stop recording");
        audioRecord.stop();
        audioRecord.release();
        wav.close();
    }

}
