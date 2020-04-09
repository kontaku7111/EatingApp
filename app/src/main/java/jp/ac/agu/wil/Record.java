package jp.ac.agu.wil;

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
    private boolean chewFlag;
    final double threshold=3;
    public int chewingCount;

    String TAG="Record";

    public void initAudioRecord(String path, String date, AudioManager audioManager){
        //waveファイル作成
        wav=new WaveFile();
        wav.createFile(path, date);
        //csvファイル作成
        csv=new CsvHandle(path, date);
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
        // short term energy
        ste=new ShortTermEnergy(shortBuf.length);

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
                double[] energy=ste.calculate(rawData);
                //
                //咀嚼計測
                //
                for(int i=0;i<energy.length;i++){
                    //閾値を上回っている
                    if (energy[i]>=threshold){
                        // 直前のデータが閾値を下回っている場合
                        if (!chewFlag){
                            chewFlag=true;
                        }
                    }
                    //閾値を下回る
                    else{
                        if(chewFlag){
                            chewFlag=false;
                            chewingCount=chewingCount+1;
                            Log.d("chewing","update chewingc ount");
                            MainActivity.chewCount.setText("Chew count: "+chewingCount);
                        }

                    }
                }
                //
                //
                //csvファイルに書き込み
                try {
                    csv.write(rawData);
                    Log.d(TAG,"rawData: "+rawData[0]);
                } catch (IOException e) {
                    Log.d(TAG,"fail to write audio to csv file");
                    e.printStackTrace();
                }
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
        csv.close();
    }

}
