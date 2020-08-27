package jp.ac.agu.wil;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.Arrays;

import jp.ac.agu.wil.MFCC.MFCC;

public class FeatureExtraction {
    MFCC mfcc = new MFCC();
    ShortTermEnergy shortTermEnergy;
    private DoubleFFT_1D mFFT;
    int SAMPLING_FREQUENCY = 8000;

    FeatureExtraction(){
//        shortTermEnergy= new ShortTermEnergy();
    }
    public float [] process(double[] signal){
        float [] m;
        // MFCCクラスのFFTサイズが2048のため、元の信号のサイズを満たす必要あり
        if(signal.length < 2048){
            double [] signal_nfft = new double[2048];
            Arrays.fill(signal_nfft, 0);
            System.arraycopy(signal,0,signal_nfft,0,signal.length);
            m =mfcc.process(signal_nfft);
        }
        else {
            // 12次のMFCC
            m = mfcc.process(signal);
        }
        // STEから特徴量抽出
//        float[] ste = shortTermEnergy.calculate_float(signal, 8000,0.04,0.01);
//        float[] steFeature = timeFeature(ste);

        // ADA
        float[] ada = amplitudeDifferenceAccumulation(signal, 8000,0.02,0.005);
//        float[] adaFeature = timeFeature(ada);  // 元の信号サイズが約300サンプルのときがあったので、Nanを返してしまう
        float [] adaFeature = new float [2];
        // 合計
        for(int i=0; i<ada.length;i++)
            adaFeature[0] += ada[i];
        // 平均
        adaFeature[1] = adaFeature[0] / ada.length;

        //　生信号の時間領域の特徴
        float [] rawFeature = timeFeature(signal);


        // 零交差数
        float zcc = zeroCrossCount(signal);

        // 最大周波数
        float maxFreq = fftFeature(signal);

        // 零交差率
        float [] zcr = zeroCrossingRate(signal, 8000, 0.02,0.005);
//        float [] zcrFeature = timeFeature(zcr);
        float [] zcrFeature = new float [2];
        // 合計
        for(int i=0; i<zcr.length;i++)
            zcrFeature[0] += zcr[i];
        // 平均
        zcrFeature[1] = zcrFeature[0] / zcr.length;


        float [] feature =new float[m.length+adaFeature.length+rawFeature.length+1+1+ zcrFeature.length];

        System.arraycopy(m,0,feature,0,m.length);
        System.arraycopy(adaFeature,0,feature,m.length,adaFeature.length);
        System.arraycopy(rawFeature,0,feature,m.length +adaFeature.length,rawFeature.length);
        feature[m.length+adaFeature.length+rawFeature.length]=zcc;
        feature[m.length+adaFeature.length+rawFeature.length+1] = maxFreq;
        System.arraycopy(zcrFeature, 0, feature, m.length + adaFeature.length + rawFeature.length + 1 + 1, zcrFeature.length);

        return feature;
    }

    public float fftFeature(double[] signal){
        int fftSize = signal.length;
        double[] ampFFT = new double [fftSize/2];
        // 計算用に引数の信号を格納
        double[] mFFTBuffer = signal;
        mFFT = new DoubleFFT_1D(fftSize);
        double maxAmp = 0;
        int maxIndex = 0;

        // FFT 実行
        mFFT.realForward(mFFTBuffer);
        // 振幅スペクトル
        for (int i = 0; i < fftSize/2; i++){
            double real = mFFTBuffer[i * 2];
            double img = mFFTBuffer[i * 2 + 1];
            double amp = Math.sqrt(real * real + img * img);
            if (amp > maxAmp) {
                maxAmp = amp;
                maxIndex = i;
            }
        }

        // 最大周波数計算
        float maxFrequency = maxIndex * SAMPLING_FREQUENCY / fftSize;
        float feature =maxFrequency;
        return feature;
    }

    public float [] timeFeature(float[] signal){
        float[] feature = new float[5];
        float sumSignal=0;
        float meanSignal;
        float varSignal=0;
        float sdSignal;
        float skew=0;
        float kurt=0;
        // 合計、面積
        for(int i=0; i<signal.length;i++)
            sumSignal += signal[i];
        feature[0] = sumSignal;
        // 平均
        meanSignal = sumSignal / signal.length;
        feature[1] = meanSignal;
        // 分散
        for(int i=0; i<signal.length;i++)
            varSignal +=(signal[i]-meanSignal)*(signal[i]-meanSignal);
        varSignal = varSignal/signal.length;
        feature[2]=varSignal;
        //　標準偏差
        sdSignal = (float) Math.sqrt(varSignal);

        for(int i=0; i<signal.length;i++) {
            skew += ((signal[i] - meanSignal )/ sdSignal) * ((signal[i] - meanSignal) / sdSignal) * ((signal[i] - meanSignal )/ sdSignal);
            kurt+= ((signal[i] - meanSignal )/ sdSignal)*((signal[i] - meanSignal ) / sdSignal) * ((signal[i] - meanSignal )/ sdSignal) * ((signal[i] - meanSignal ) / sdSignal);
        }
        skew = skew/signal.length;
        kurt = kurt/signal.length-3;
        feature[3] = skew;
        feature[4] = kurt;
        return feature;
    }

    public float [] timeFeature(double[] signal){
        // 引数がdouble型
        float[] feature = new float[5];
        float sumSignal=0;
        float meanSignal;
        float varSignal=0;
        float sdSignal;
        float skew=0;
        float kurt=0;
        // 合計、面積
        for(int i=0; i<signal.length;i++)
            sumSignal += signal[i];
        feature[0] = sumSignal;
        // 平均
        meanSignal = sumSignal / signal.length;
        feature[1] = meanSignal;
        // 分散
        for(int i=0; i<signal.length;i++)
            varSignal +=(signal[i]-meanSignal)*(signal[i]-meanSignal);
        varSignal = varSignal/signal.length;
        feature[2]=varSignal;
        //　標準偏差
        sdSignal = (float) Math.sqrt(varSignal);

        for(int i=0; i<signal.length;i++) {
            skew += ((signal[i] - meanSignal )/ sdSignal) * ((signal[i] - meanSignal) / sdSignal) * ((signal[i] - meanSignal )/ sdSignal);
            kurt+= ((signal[i] - meanSignal )/ sdSignal)*((signal[i] - meanSignal ) / sdSignal) * ((signal[i] - meanSignal )/ sdSignal) * ((signal[i] - meanSignal ) / sdSignal);
        }
        skew = skew/signal.length;
        kurt = kurt/signal.length-3;
        feature[3] = skew;
        feature[4] = kurt;
        return feature;
    }

    public float[] zeroCrossingRate(double[] y, int Fs, double frameSize, double frameShift){
        /**
         *  double[] y: 生の信号
         *  int Fs: サンプリング周波数
         *  double frameSize: フレームサイズ [s]
         *  double frameShift: フレームシフト[s]
         */
        int Len = y.length;
        int fsize = (int)(frameSize * Fs);
        int fshift = (int) (frameShift * Fs);
        int slideCount = (int) Math.floor((Len-fsize)/fshift);
        float[] zcr = new float[slideCount];
        for (int i=0; i<slideCount; i++){
            zcr[i]= 0;
            for(int j =i*fshift+1; j< i*fshift+fsize; j++){
                if(y[j] * y[j-1] < 0)
                    zcr[i] ++;
            }
            zcr[i] = zcr[i] / (fsize-1);
        }
        return zcr;
    }

    public float[] amplitudeDifferenceAccumulation(double[] y, int Fs, double frameSize, double frameShift){
        int len = y.length;
        int fsize = (int)(frameSize * Fs);
        int fshift = (int) (frameShift * Fs);
        int slideCount = (int) Math.floor((len-fsize)/fshift);
        float[] ada = new float[slideCount];
        for (int i=0; i<slideCount; i++){
            ada[i]= 0;
            for(int j =i*fshift+1; j< i*fshift+fsize; j++){
                ada[i] += Math.abs(y[j] - y[j-1]);
            }
        }
        return ada;
    }

    public float zeroCrossCount(double[] y){
        /**
         * double[] y: 生の信号
         * float zcc: 信号がゼロを交差した回数
         */
        float zcc = 0;
        for (int i=1; i < y.length; i++){
            if(y[i] * y[i-1] <  0)
                zcc++;
        }
        return zcc;
    }
}
