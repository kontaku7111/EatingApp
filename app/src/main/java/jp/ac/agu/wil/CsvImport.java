package jp.ac.agu.wil;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import jp.ac.agu.wil.MFCC.MFCC;

/*
アセットにあるCSVファイルを読み込み
 */
public class CsvImport {
    AssetManager assetManager;
    String[] fileList;
    MFCC mfcc;
    FeatureExtraction featureExtraction;
    public CsvImport(Context context){
        mfcc = new MFCC();
        assetManager =context.getResources().getAssets();
        try {
            fileList = assetManager.list("csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String f: fileList){
            System.out.println("ファイル名: "+f);
            Log.d("CSV","ファイル名: "+f);
//            Double[][] csvRawData = readCsvData(assetManager,f);
            readCsvData(assetManager,f);
            break;
        }
    }
    private void readCsvData(AssetManager assetManager, String file_path){
        ArrayList<Double> oneLineData = new ArrayList<>();
        ArrayList<Double> chewData = new ArrayList<>();
        ArrayList<Double> swallowData = new ArrayList<>();
        ArrayList<Double> talkData = new ArrayList<>();
        ArrayList<Double> otherData = new ArrayList<>();

        InputStream inputStream = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        boolean [] labelFlag = new boolean[4];
        labelFlag[0]=false;
        labelFlag[1]=false;
        labelFlag[2]=false;
        labelFlag[3]=false;
        int chewCount = 0;
        int swallowCount = 0;
        int talkCount = 0;
        int otherCount = 0;

        try{
            inputStream = assetManager.open("csv/" + file_path);
            isr = new InputStreamReader(inputStream);
            br = new BufferedReader(isr);
            Log.d("CSV", "CSV 読み込み開始");
            String lines;
            //　行数
            int indexCount =0;
            //列名管理
            String[] arr =null;

            while((lines = br.readLine()) != null){
//                Log.d("CSV", "行: "+ indexCount);
                // 先頭行は列名
                // 0: sample_num, 1: time, 2: data, 3: chew, 4: swallow, 5: talk, 6: other
                if (indexCount == 0){
                    arr = lines.split(",");
                }
                else{
                    // カンマで分割した内容を配列に格納
                    String[] data = lines.split(",");
                    //
                    for(int j=2;j<arr.length;j++){
                        oneLineData.add(Double.parseDouble(data[j]));
                    }
                    // ラベル抽出
                    //chew
                    if (!labelFlag[0] && oneLineData.get(1)==1){
                        labelFlag[0] = true;
                        chewData.add(oneLineData.get(0));
                    }else if(labelFlag[0]&&oneLineData.get(1)==1){
                        chewData.add(oneLineData.get(0));
                    }else if(labelFlag[0] && oneLineData.get(1) != 1){
                        labelFlag[0] = false;
                        chewCount++;
                        //特徴量抽出
                        double[] chewSignal = arrayToDouble(chewData);
                        float[] feature = featureExtraction.process(chewSignal);
                        chewData.clear();
                    }
                    // swallow
                    if (!labelFlag[1] && oneLineData.get(2)==1){
                        labelFlag[1] = true;
                        swallowData.add(oneLineData.get(0));
                    }else if(labelFlag[1]&&oneLineData.get(2)==1){
                        swallowData.add(oneLineData.get(0));
                    }else if(labelFlag[1] && oneLineData.get(2) != 1){
                        labelFlag[1] = false;
                        swallowCount++;
                        //特徴量抽出
                        swallowData.clear();
                    }
                    // talk
                    if (!labelFlag[2] && oneLineData.get(3)==1){
                        labelFlag[2] = true;
                        talkData.add(oneLineData.get(0));
//                        System.out.print("talk "+ (talkCount) +": "+ indexCount);
                    }else if(labelFlag[2]&&oneLineData.get(3)==1){
                        talkData.add(oneLineData.get(0));
                    }else if(labelFlag[2] && oneLineData.get(3) != 1){
                        labelFlag[2] = false;
                        talkCount++;
                        //特徴量抽出
//                        System.out.println(", " + indexCount);
//                        Log.d("CSV", "talk "+ talkCount + ": "+ talkData.size());
                        talkData.clear();
                    }
                    //other
                    if (!labelFlag[3] && oneLineData.get(4)==1){
                        labelFlag[3] = true;
                        otherData.add(oneLineData.get(0));
                    }else if(labelFlag[3]&&oneLineData.get(4)==1){
                        otherData.add(oneLineData.get(0));
                    }else if(labelFlag[3] && oneLineData.get(4) != 1){
                        labelFlag[3] = false;
                        otherCount++;
                        //特徴量抽出
                        otherData.clear();
                    }


//                    rawData.add(oneLineData.toArray(new Double[oneLineData.size()]));

                    oneLineData.clear();
                }
                indexCount++;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        finally{
            try{
                br.close();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        chewData.clear();
        Log.d("CSV","chewCount: " +chewCount + ", swallowCount: "+ swallowCount +", talkCount: "+talkCount+", otherCount: "+otherCount);
        

        return;
    }

    private static double[] arrayToDouble(ArrayList<Double> array){
        double[] data = new double[array.size()];
        for (int i=0; i< array.size();i++){
            data[i] = array.get(i);
        }
        return data;
    }
}
