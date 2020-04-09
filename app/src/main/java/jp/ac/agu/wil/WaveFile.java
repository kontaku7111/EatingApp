package jp.ac.agu.wil;

import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class WaveFile {
    private static final String TAG="WaveFile";
    private static final int FILE_SIZE_SEEK=4;
    private static final int DATA_SIZE_SEEK=40;

    private final int SAMPLING_RATE=8000;
    private RandomAccessFile raf; //リアルタイム処理なのでランダムアクセスファイルクラスを使用する
    private File recFile; //録音後の書き込み、読み込みようファイル
    private byte[] RIFF = {'R','I','F','F'}; //wavファイルリフチャンクに書き込むチャンクID用
    private int fileSize = 36;
    private byte[] WAVE = {'W','A','V','E'}; //WAV形式でRIFFフォーマットを使用する
    private byte[] fmt = {'f','m','t',' '}; //fmtチャンク　スペースも必要
    private int fmtSize = 16; //fmtチャンクのバイト数
    private byte[] fmtID = {1, 0}; // フォーマットID リニアPCMの場合01 00 2byte
    private short chCount = 1; //チャネルカウント モノラルなので1 ステレオなら2にする
    private int bytePerSec = SAMPLING_RATE * (fmtSize / 8) * chCount; //データ速度
    private short blockSize = (short) ((fmtSize / 8) * chCount); //ブロックサイズ (Byte/サンプリングレート * チャンネル数)
    private short bitPerSample = 16; //サンプルあたりのビット数 WAVでは8bitか16ビットが選べる
    private byte[] data = {'d', 'a', 't', 'a'}; //dataチャンク
    private int dataSize = 0; //波形データのバイト数
    //short型は16ビットで-32768～32767

    public	void	createFile(String path,String date){
        //String型をUri型に変換
        Uri newUri=Uri.parse(path+"/"+date+"/"+date+".wav");
        //	ファイルを作成
        recFile	=	new	File(newUri.getPath());

        if(recFile.exists()){
            Log.d(TAG,"createFile(): recfile exists!!");
            recFile.delete();
        }
        if(recFile.canRead()){
            //
        }
        else{
            //
        }
        if (recFile.canWrite()){
            //
        }
        else{
            //
        }
        try	{
            recFile.createNewFile();
            Log.d(TAG,"createFile(): succeed createNewFile()");
        }	catch	(IOException e)	{
            //	TODO	Auto-generated	catch	block
            Log.d(TAG,"createFile(): failed createNewFile()");
            e.printStackTrace();
        }

        try	{
            raf	=	new	RandomAccessFile(recFile,"rw");
        }	catch	(FileNotFoundException e)	{
            //	TODO	Auto-generated	catch	block
            e.printStackTrace();
        }

        //wavのヘッダを書き込み
        try	{
            raf.seek(0);
            //RIFFヘッダ [4byte]
            raf.write(RIFF);
            // これ以降のファイルサイズ（ファイルサイズ-8）, -8はRIFFヘッダとこのファイルサイズ指定分 [4byte]
            raf.write(littleEndianInteger(fileSize));
            // WAVE ヘッダ。RIFFの種類がWAVEであることを表す [4byte]
            raf.write(WAVE);
            // fmtチャンク。フォーマットの定義 [4byte]
            raf.write(fmt);
            //バイト数。fmtチャンクのバイト数。リニアPCMならば16。本アプリはリニアPCM [4byte]
            raf.write(littleEndianInteger(fmtSize));
            // フォーマットID。リニアPCMなら１ [2byte]
            raf.write(fmtID);
            //チャンネル数。モノラルならば1, ステレオならば2 [2byte]
            raf.write(littleEndianShort(chCount));
            // サンプリングレート [4byte]
            raf.write(littleEndianInteger(SAMPLING_RATE));
            // データ速度 (Byte/sec) = サンプリングレート * (8bitの場合１、16bitの場合２) * (モノラルならば１、ステレオなら２) [4byte]
            raf.write(littleEndianInteger(bytePerSec));
            // ブロックサイズ (Byte/sample*チャンネル数) = (8bitの場合１、16bitの場合２) * (モノラルならば１、ステレオなら２) [2byte]
            raf.write(littleEndianShort(blockSize));
            // サンプルあたりのビット数 (bit/sample) = 8 or 16 (bit) [2byte]
            raf.write(littleEndianShort(bitPerSample));
            // dataチャンク [4byte]
            raf.write(data);
            // 波形データのバイト数 [4byte]
            raf.write(littleEndianInteger(dataSize));

        }	catch	(IOException	e)	{
            //	TODO	Auto-generated	catch	block
            e.printStackTrace();
        }

    }
    // int型をリトルエンディアンのbyte配列に変更
    private byte[] littleEndianInteger(int i) {
        byte[] buffer = new byte[4];
        buffer[0] = (byte) i;
        buffer[1] = (byte) (i >> 8);
        buffer[2] = (byte) (i >> 16);
        buffer[3] = (byte) (i >> 24);
        return buffer;
    }

    // PCMデータを追記するメソッド
    public void addBigEndianData(short[] shortData){
        Log.d(TAG, "addBigEndianData: " + shortData[1]);//shortData[0]);
        Log.d(TAG,"addBigEndianData: "+shortData.length);
        // ファイルにデータを追記
        try {
            raf.seek(raf.length());
            raf.write(littleEndianShorts(shortData));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // ファイルサイズを更新
        updateFileSize();

        // データサイズを更新
        updateDataSize();

    }

    // ファイルサイズを更新
    private void updateFileSize(){
        fileSize = (int) (recFile.length() - 8);
        byte[] fileSizeBytes = littleEndianInteger(fileSize);
        try {
            raf.seek(FILE_SIZE_SEEK);
            raf.write(fileSizeBytes);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // データサイズを更新
    private void updateDataSize(){
        dataSize = (int) (recFile.length() - 44);
        byte[] dataSizeBytes = littleEndianInteger(dataSize);
        try {
            raf.seek(DATA_SIZE_SEEK);
            raf.write(dataSizeBytes);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // short型変数をリトルエンディアンのbyte配列に変更
    private byte[] littleEndianShort(short s){

        byte[] buffer = new byte[2];

        buffer[0] = (byte) s;
        buffer[1] = (byte) (s >> 8);

        return buffer;
    }

    // short型配列をリトルエンディアンのbyte配列に変更
    private byte[] littleEndianShorts(short[] s){

        byte[] buffer = new byte[s.length * 2];
        int i;

        for(i = 0; i < s.length; i++){
            buffer[2 * i] = (byte) s[i];
            buffer[2 * i + 1] = (byte) (s[i] >> 8);
        }
        return buffer;
    }

    // ファイルを閉じる
    public void close(){
        try {
            raf.close();
            Log.d(TAG, "ファイルクローズ");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
