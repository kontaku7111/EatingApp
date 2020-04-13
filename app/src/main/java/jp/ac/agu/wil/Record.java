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
    private int chewFlag;
    final double threshold=1;
    public int chewingCount;
    public int callbackCountRecordedLastChew; //咀嚼の検出後のコールバック回数
    public final int PERIOD_BTWN_CHEWS=12; // １回の咀嚼は0.3sぐらいだが適切なものに調整
    public boolean notCountChew; // 咀嚼と判定しない
    final double thresholdTalking=50; // STEの値が50を超えたら会話
    final int RESET_COUNT=75; // 3s間咀嚼が検出されなかったらリセット
    public int countAfterChew; // 咀嚼検出後コールバック呼び出しの際にインクリメント

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
        callbackCountRecordedLastChew=0;
        notCountChew=false;
        chewFlag=0;
        countAfterChew=-99;

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
                if (countAfterChew==RESET_COUNT){
                    countAfterChew=-99;
                    chewingCount=0;
                    MainActivity.chewCount.setText("Chew count: "+chewingCount);
                }
                if (notCountChew==true && callbackCountRecordedLastChew>PERIOD_BTWN_CHEWS){
                    notCountChew=false;
                    callbackCountRecordedLastChew=0;
                    Log.d("ChewCount","Reset Callback Count");
                }
                for(int i=0;i<energy.length;i++){
                    //閾値を上回っている
                    if (energy[i]>=threshold){
                        // 直前のデータが閾値を下回っている場合
                        if (0==chewFlag){
                            chewFlag=1;
                        }else if (1==chewFlag && thresholdTalking<=energy[i]){
                            chewFlag=2;
                        }
                    }
                    //閾値を下回る
                    else{
                        if(1<=chewFlag){
                            if(!notCountChew && chewFlag==1){
                                chewingCount=chewingCount+1;
                                Log.d("chewing","update chewingc ount");
                                MainActivity.chewCount.setText("Chew count: "+chewingCount);
                                notCountChew=true;
                                countAfterChew=0;
                            }
                            chewFlag=0;
                        }
                    }
                }

                // 咀嚼が検出されて0.3s以内
                if(notCountChew){
                    callbackCountRecordedLastChew++;
                    Log.d("ChewCount","callback count: "+callbackCountRecordedLastChew);
                }
                //咀嚼が検出後のみ
                if(countAfterChew!=-99){
                    countAfterChew++;
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
