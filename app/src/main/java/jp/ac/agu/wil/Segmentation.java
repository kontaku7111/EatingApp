package jp.ac.agu.wil;

import java.util.ArrayList;
import java.util.Arrays;

public class Segmentation {
    double [] preData;
    double[] signal;
    int frame_size=320; //sample (0.04s)
    int frame_shift=80; // sample (0.01s)
    boolean thresholdFlag =false;
    final double THRESHOLD = 0.01;
    boolean isWithin300msSegmented = false; //300msフラッグ
    boolean isCountTime = false;
    int count300ms=0;
    int spareCount300ms=0;
    ArrayList<Double> segmentedData = new ArrayList<>();
    ArrayList<Double> p3SegmentedData = new ArrayList<>();
    ArrayList<Double> spareSegmentedData = new ArrayList<>();

    public Segmentation(){
        preData = new double[frame_size-1];
        // ゼロで初期化
        Arrays.fill(preData, 0.0);
        // 処理で使う信号を格納する変数
        signal = new double[preData.length + frame_size];
    }

    void calculateSte(double [] newData){
        // Short term energyを音声信号にかけて得られた信号を格納する変数作成
        double [] energy = new double[(int)Math.floor((signal.length-frame_size)/frame_shift)+1];

        // 配列を連結
        System.arraycopy(preData,0, signal ,0, preData.length);
        System.arraycopy(newData,0, signal ,preData.length, newData.length);
        // short term energy 実装
        for(int i=0; i<=Math.floor((signal.length-frame_size)/frame_shift);i++)
        {
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
                        addSegmentedData(signal, i);
                    }
                }
                // 直前まで閾値を超えてない
                else
                {
                    thresholdFlag=true;
                    if (isWithin300msSegmented)
                    {
                        // 予備セグメント初期化とデータ追加
                        spareSegmentedData.clear();
                        addSpareSegmentedData(signal, i);
                        // どの時点で予備セグメントが作成されたか記録
                        spareCount300ms = count300ms + 1;
                    }
                    //　300ms以内にセグメンテーションが行われてない
                    else
                    {
                        isCountTime=true;
                        count300ms = -1;
                        // セグメントにデータ追加
                        addSegmentedData(signal, i);
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
                        segmentedData.addAll(p3SegmentedData);
                        segmentedData.addAll(spareSegmentedData);
                        p3SegmentedData.clear();
                        spareSegmentedData.clear();
                    }
                    else
                    {
                        isWithin300msSegmented = true;
                    }
                }
                if(thresholdFlag || isWithin300msSegmented)
                {
                    // P3区間のデータを追加
                    addP3SegmentedData(signal,i);
                }
            }
            // カウントアップ状態か
            if (isCountTime)
            {
                count300ms++;
                // セグメントの開始時点から300ms経った
                if (30 == count300ms)
                {
                    // 特徴量抽出
                    MainActivity.bite_chewingCount++;
                    MainActivity.chewCount.setText("Chew count (Bite): "+MainActivity.bite_chewingCount);
                    if (thresholdFlag)
                    {
                        count300ms = count300ms-spareCount300ms;
                        // スペアのセグメントデータをセグメントデータに変更
                        segmentedData.clear();
                        segmentedData.addAll(spareSegmentedData);
                        // スペアとP3セグメントを初期化
                        spareSegmentedData.clear();
                        p3SegmentedData.clear();

                    }
                    else
                    {
                        isCountTime = false;
                        count300ms = -1;
                        // 全てのセグメントデータを初期化
                        segmentedData.clear();
                        spareSegmentedData.clear();
                        p3SegmentedData.clear();
                    }
                    isWithin300msSegmented = false;
                }

            }
        }
        // 次の計算に必要なデータ保存
        System.arraycopy(newData,1, preData ,0, preData.length);
    }

    public void  addSegmentedData(double[] frame, int indexFrame)
    {
        for(int j=indexFrame*frame_shift+240;j<indexFrame*frame_shift+frame_size;j++)
        {
            segmentedData.add(frame[j]);
        }
    }
    public void addSpareSegmentedData(double [] frame, int indexFrame)
    {
        for(int j=indexFrame*frame_shift+240;j<indexFrame*frame_shift+frame_size;j++)
        {
            spareSegmentedData.add(frame[j]);
        }
    }

    public void addP3SegmentedData(double [] frame, int indexFrame)
    {
        for(int j=indexFrame*frame_shift+240;j<indexFrame*frame_shift+frame_size;j++)
        {
            p3SegmentedData.add(frame[j]);
        }
    }
}
