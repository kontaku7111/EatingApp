package jp.ac.agu.wil;

import java.util.Arrays;

public class ShortTermEnergy {
    double [] preData;
    double[] signal;
    int frame_size=320; //sample (0.04s)
    int frame_shift=80; // sample (0.01s)
    public ShortTermEnergy(){

    }
    public ShortTermEnergy(int size){
        //一つ前にに読み込みした音声データを入れる変数。サイズを-1にしたのは1番目の要素のデータはSTEで使わないため（つまりデータ数は319）
        preData=new double[size-1];
        //ゼロで初期化
        Arrays.fill(preData,0.0);
        // short term energy で使う変数宣言
        signal=new double[preData.length+size];
    }

    double [] calculate(double [] newData){
        // Short term energyを音声信号にかけて得られた信号を格納する変数作成
        double [] energy = new double[(int)Math.floor((signal.length-frame_size)/frame_shift)+1];

        // 配列を連結
        System.arraycopy(preData,0, signal ,0, preData.length);
        System.arraycopy(newData,0, signal ,preData.length, newData.length);
        // short term energy 実装
        for(int i=0; i<=Math.floor((signal.length-frame_size)/frame_shift);i++){
            for(int j=0;j<i*frame_shift+frame_size;j++){
                // 窓はrect windowなので窓をかけずに計算
                energy[i]=energy[i]+signal[j]*signal[j];
            }
        }
        // 次の計算に必要なデータ保存
        System.arraycopy(newData,1, preData ,0, preData.length);
        return energy;
    }

    float[] calculate_float(double[] signal, double Fs, double frameSizeTime, double frameShiftTime){
        int sigLen = signal.length;
        int fsize = (int)(frameSizeTime * 8000);
        int fshift = (int) (frameShiftTime * 8000);
        int slideCount = (int)Math.floor((sigLen-fsize)/fshift);
        float[] ste = new float[slideCount];
        for (int i=0; i<slideCount; i++){
            ste[i]= 0;
            for(int j =i*fshift; j< i*fshift+fsize; j++){
                ste[i] = ste[i] + (float)(signal[j] * signal[j]);
            }
        }
        return ste;
    }


}
