package jp.ac.agu.wil;

import jp.ac.agu.wil.MFCC.MFCC;

public class FeatureExtraction {
    MFCC mfcc = new MFCC();
    ShortTermEnergy shortTermEnergy= new ShortTermEnergy();
    public float [] process(double[] signal){
        float[] feature = new float[12];
        float [] m =mfcc.process(signal);
        float[] ste = shortTermEnergy.calculate_float(signal, 8000,0.04,0.01);
        float steSum = 0;
        for(int i=0; i<ste.length;i++)
            steSum += ste[i];
        float steMean = steSum / ste.length;
        float steSSum = 0;
        for(int i=0; i<ste.length;i++)
            steSSum +=(ste[i]-steMean)*(ste[i]-steMean);
        float steVar = steSSum/ste.length;
        float steSd = (float) Math.sqrt(steVar);
        float steSkew ;
        /////////　ここから作業再開
        return feature;
    }
}
