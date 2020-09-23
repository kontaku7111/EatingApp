package jp.ac.agu.wil;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

public class CsvHandle {
    String fileName;
    FileOutputStream fileOutputStream;
    OutputStreamWriter outputStreamWriter;
    BufferedWriter bufferedWriter;
    //コンストラクタ
    public CsvHandle(String path, String date) {
        fileName=path+"/"+date+"/"+date+".csv";
        try {
            fileOutputStream = new FileOutputStream(fileName, false);
            // 文字コード指定
            outputStreamWriter = new OutputStreamWriter(fileOutputStream, "UTF-8");
            //
            bufferedWriter = new BufferedWriter(outputStreamWriter);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    public CsvHandle(String path) {
        fileName=path+"/confirmSegmentation.csv";
        Log.d("csv",fileName);
        try {
            fileOutputStream = new FileOutputStream(fileName, false);
            // 文字コード指定
            outputStreamWriter = new OutputStreamWriter(fileOutputStream, "UTF-8");
            //
            bufferedWriter = new BufferedWriter(outputStreamWriter);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    //ファイル書き込み
    public void write(double[] rawData) throws IOException {
//        bufferedWriter.write(Arrays.toString(rawData));
//        bufferedWriter.newLine();
        for(int i=0;i<rawData.length;i++){
//            Log.d("csv", Double.toString(rawData[i]));
            bufferedWriter.write(Double.toString(rawData[i]));
//            Log.d("csv","have written a data");
            bufferedWriter.newLine();
        }
    }
    public void write(double[] rawData, boolean segmented) throws IOException {
        String segment;
        if(segmented)
            segment = ", 1";
        else
            segment = ", 0";

        for(int i=0;i<rawData.length;i++){
            Log.d("csv",rawData[i]+segment);
            bufferedWriter.write(rawData[i]+segment);
            bufferedWriter.newLine();
        }
    }

    //ファイルを閉じる
    public void close() throws IOException {
        bufferedWriter.flush();
        bufferedWriter.close();
    }

}
