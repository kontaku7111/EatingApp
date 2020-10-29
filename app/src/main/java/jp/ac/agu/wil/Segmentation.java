package jp.ac.agu.wil;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Segmentation{
    double [] preData;
    double[] signal;
    int frame_size=320; //sample (0.04s, 40ms)
    int frame_shift=80; // sample (0.01s, 10ms)
    boolean thresholdFlag =false;
    final double THRESHOLD = 0.01;
    boolean isWithin300msSegmented = false; //300msフラッグ
    boolean isCountTime = false;
    int count300ms=0;
    int spareCount300ms=0;
    // 23個の特徴を格納する変数
    float [] feature = new float[23];
    // 分類モデル
    ClassificationModel clfModel;
    Context mcontext;
    String predictedResult;

    ArrayList<Double> segmentedData = new ArrayList<>();
    ArrayList<Double> p2SegmentedData = new ArrayList<>();
    ArrayList<Double> spareSegmentedData = new ArrayList<>();
    FeatureExtraction featureExtraction;
    double time=0;
    double preTime = 0;
    String TAG = "Segmentation";
    CsvHandle csv;

    Thread m_thread;

    public Segmentation(Context context){
        preData = new double[frame_size-frame_shift];
        // ゼロで初期化
        Arrays.fill(preData, 0.0);
        featureExtraction = new FeatureExtraction();
        // 処理で使う信号を格納する変数
        signal = new double[preData.length + frame_size];
        // モデル呼び出し
        mcontext=context;
        clfModel = new ClassificationModel("rf_eating.model", mcontext);
        preTime = System.currentTimeMillis();
        // セグメンテーション書き出し
        csv = new CsvHandle("/storage/emulated/0/Android/data/jp.ac.agu.wil/files/Music");
    }

    void calculateSte(double [] newData) throws IOException {
        // Short term energyを音声信号にかけて得られた信号を格納する変数作成
        double [] energy = new double[(int)Math.floor((signal.length-frame_size)/frame_shift)+1];
        signal = new double[preData.length + frame_size]; // 念のため追加（20200923）
        // 配列を連結
        System.arraycopy(preData,0, signal ,0, preData.length);
        System.arraycopy(newData,0, signal ,preData.length, newData.length);
        // short term energy 実装
        for(int i=0; i<=Math.floor((signal.length-frame_size)/frame_shift);i++)
        {
//            Log.d(TAG, "frame shift count: "+i);
            // STE計算
            for(int j=i*frame_shift;j<i*frame_shift+frame_size;j++)
            {
                // 窓はrect windowなので窓をかけずに計算
                energy[i]=energy[i]+signal[j]*signal[j];
            }
            /**
             *  セグメンテーションアルゴリズム
             */
            //　閾値を超えている
            if(energy[i] >= THRESHOLD)
            {
                // 既に閾値を超えている
                if(thresholdFlag)
                {
                    if(isWithin300msSegmented)
                    {
                        // 予備セグメントにデータ追加
                        addSpareSegmentedData(signal, i);
                    }
                    //　300ms以内にセグメンテーションが行われてない
                    else
                    {
                        // セグメントにデータ追加
                        addSegmentedData(signal, i,false);
                    }
                }
                // 直前まで閾値を超えてない
                else
                {
                    thresholdFlag=true;
                    if (isWithin300msSegmented)
                    {
                        // 予備セグメント初期化とデータ追加
                        spareCount300ms=0;
                        isWithin300msSegmented=false;
                        spareSegmentedData.clear();
                        addSpareSegmentedData(signal, i);
                        // どの時点で予備セグメントが作成されたか記録
                        spareCount300ms = count300ms + 1;
                    }
                    //　300ms以内にセグメントがない
                    else
                    {
                        isCountTime=true;
                        count300ms = -1;
                        // セグメントにデータ追加
                        addSegmentedData(signal, i,true);
                    }
                }
            }
            //　閾値を超えてない
            else
            {
                // 直前まで閾値を超えていた
                if (thresholdFlag)
                {
                    thresholdFlag = false;
                    if (isWithin300msSegmented)
                    {
                        // 2つ前のセグメントと直前のセグメント、その間の区間P3をマージ
                        segmentedData.addAll(p2SegmentedData);
                        segmentedData.addAll(spareSegmentedData);
                        p2SegmentedData.clear();
                        spareSegmentedData.clear();
                        addP2SegmentedData(signal,i);
                    }
                    else
                    {
                        isWithin300msSegmented = true;
                        addP2SegmentedData(signal,i);
                    }
                }
                else{
                    // セグメントが300ms以内に存在する
                    if (isWithin300msSegmented){
                        addP2SegmentedData(signal,i);
                    }
                    else{
                        double [] rawData =new double [80];
                        int raw_index =0;
                        for (int j = i * frame_shift + 240; j < i * frame_shift + frame_size; j++) {
                            rawData[raw_index] = signal[j];
                            raw_index++;
                        }
                        csv.write(rawData,false);
                    }
                }
            }
            // カウントアップ状態か
            if (isCountTime)
            {
                count300ms++;
                Log.d(TAG, "count300Time: "+ count300ms);
                // セグメントの開始時点から300ms経った →　450msに変更
                if (45 <= count300ms)
                {
                    if (thresholdFlag){
                        // 300ms以内にセグメントがあるが、既に新しいセグメンテーションを行っている
                        if (isWithin300msSegmented){
                            /// 特徴量抽出
                            new Thread(new Thread(new Runnable() {
                                @Override
                                public void run() {
                                        classification();
                                }
                            })).start();
                            count300ms = count300ms-spareCount300ms;

                            spareCount300ms=0;
                            isWithin300msSegmented = false;
                            csv.write(removeInitial240Sample(wrapperToPrimitive(segmentedData)),true);
                            csv.write(wrapperToPrimitive(p2SegmentedData),false);
                            // スペアのセグメントデータをセグメントデータに変更
                            segmentedData.clear();
                            segmentedData.addAll(spareSegmentedData);
                            // スペアとP3セグメントを初期化
                            spareSegmentedData.clear();
                            p2SegmentedData.clear();
                        }
                        // 300ms以内にセグメントがなく、セグメンテーションを行っている途中
                        else{
                            break;
                        }
                    }
                    else{
                        // 特徴量抽出
                        classification();
                        csv.write(removeInitial240Sample(wrapperToPrimitive(segmentedData)),true);
                        csv.write(wrapperToPrimitive(p2SegmentedData),false);
                        isCountTime = false;
                        count300ms = -1;
                        // 全てのセグメントデータを初期化
                        segmentedData.clear();
                        spareSegmentedData.clear();
                        p2SegmentedData.clear();
                    }
                    isWithin300msSegmented = false;
                }

            }
        }
        // 次の計算に必要なデータ保存
        System.arraycopy(newData,1, preData ,0, preData.length);
    }

    public double []  wrapperToPrimitive(ArrayList<Double> data){
        double[] doubleData = new double[data.size()];
        for (int data_i = 0; data_i<data.size();data_i++){
            doubleData[data_i] = data.get(data_i);
        }
        return doubleData;
    }

    public double [] removeInitial240Sample(double[] segmentedData){
        double [] removedData = new double[segmentedData.length-240];
        for (int data_i = 0; data_i < removedData.length; data_i++){
            removedData[data_i] = segmentedData[data_i+240];
        }
        return removedData;
    }

    public void csvClose() throws IOException {
        csv.close();
    }

    public void classification(){
        // データ抽出
        double[] signal = new double[segmentedData.size()];
        for (int signal_i = 0; signal_i<segmentedData.size();signal_i++){
            signal[signal_i] = segmentedData.get(signal_i);
        }
        Log.d(TAG, "segment size: "+signal.length);
        time = System.currentTimeMillis();
        // 特徴量抽出
        feature = featureExtraction.process(signal);
        // 分類
        predictedResult = clfModel.predict(feature);
        Log.d("prediction", "Prediction Time: "+String.valueOf(time-preTime) + "ms");
        preTime = time;
        switch(predictedResult){
            case "chew":
                MainActivity.bite_chewingCount++;
                MainActivity.total_chewingCount++;
                MainActivity.DebugMessage.setText("Chewing");
                MainActivity.chewCount.setText("Chew count (Bite): "+MainActivity.bite_chewingCount);
                Log.d("prediction","chew");
                break;
            case "swallow":
                Log.d("prediction","swallow");
                MainActivity.DebugMessage.setText("Swallowing");
                MainActivity.bite_chewingCount = 0;
                MainActivity.chewCount.setText("Chew count (Bite): "+MainActivity.bite_chewingCount);
                break;
            case "talk":
                MainActivity.DebugMessage.setText("Talking");
                Log.d("prediction","talk");
                break;
            case "other":
                MainActivity.DebugMessage.setText("Other");
                Log.d("prediction","other");
                break;
        }
    }

    public void  addSegmentedData(double[] frame, int indexFrame, boolean isForward) {
        if (isForward){
            for (int j = indexFrame * frame_shift; j < indexFrame * frame_shift + frame_size; j++) {
                segmentedData.add(frame[j]);
            }
        }
        else {
            for (int j = indexFrame * frame_shift + 240; j < indexFrame * frame_shift + frame_size; j++) {
                segmentedData.add(frame[j]);
            }
        }
    }

    public void addSpareSegmentedData(double [] frame, int indexFrame)
    {
        for(int j=indexFrame*frame_shift+240;j<indexFrame*frame_shift+frame_size;j++)
        {
            spareSegmentedData.add(frame[j]);
        }
    }

    public void addP2SegmentedData(double [] frame, int indexFrame)
    {
        for(int j=indexFrame*frame_shift+240;j<indexFrame*frame_shift+frame_size;j++)
        {
            p2SegmentedData.add(frame[j]);
        }
    }
}
