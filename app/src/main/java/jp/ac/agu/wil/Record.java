package jp.ac.agu.wil;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.util.Log;

import java.io.IOException;

public class Record {
    public AudioRecord audioRecord=null;
    private static final int SAMPLING_RATE=8000;
    private WaveFile wav;
    private CsvHandle csv;
    private ShortTermEnergy ste;
    private int bufferSize;//オーディオレコード用バッファのサイズ
    private short[] shortBuf; //オーディオレコード用バッファ
    private Segmentation seg;
    Context mcontext;
    String TAG="Record";

    public Record(Context context){
        mcontext = context;
    }

    public void initAudioRecord(String path, String date, AudioManager audioManager){
        //waveファイル作成
        wav=new WaveFile();
        wav.createFile(path, date);
        //csvファイル作成
//        csv=new CsvHandle(path, date);
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

        seg = new Segmentation(mcontext);

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
//                Log.d("shortbuf size: ", String.valueOf(shortBuf.length)); //320
                wav.addBigEndianData(shortBuf);
                // double型、32768を割ることにより、waveファイル読み込み時に得る信号と同じ値に変換
                double [] rawData = new double[shortBuf.length];
                for(int i=0;i<shortBuf.length;i++){
                    rawData[i]=(double) shortBuf[i]/ 32768;
                }
                //
                //咀嚼計測
                //
                try {
                    seg.calculateSte(rawData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                //
//                //csvファイルに書き込み
//                try {
//                    csv.write(rawData);
////                    Log.d(TAG,"rawData: "+rawData[0]);
//                } catch (IOException e) {
////                    Log.d(TAG,"fail to write audio to csv file");
//                    e.printStackTrace();
//                }
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
    public void stopAudioRecord() throws IOException {
        Log.d(TAG,"stopAudioRecord(): stop recording");
        audioRecord.stop();
        audioRecord.release();
        wav.close();
//        csv.close();
        seg.csvClose();
    }

}
