package jp.ac.agu.wil;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

public class ClassificationModel {

    Context mContext=null;

    static private final Attribute feature0  = new Attribute("feature0");
    static private final Attribute feature1  = new Attribute("feature1");
    static private final Attribute feature2  = new Attribute("feature2");
    static private final Attribute feature3  = new Attribute("feature3");
    static private final Attribute feature4  = new Attribute("feature4");
    static private final Attribute feature5  = new Attribute("feature5");
    static private final Attribute feature6  = new Attribute("feature6");
    static private final Attribute feature7  = new Attribute("feature7");
    static private final Attribute feature8  = new Attribute("feature8");
    static private final Attribute feature9  = new Attribute("feature9");
    static private final Attribute feature10 = new Attribute("feature10");
    static private final Attribute feature11 = new Attribute("feature11");
    static private final Attribute feature12 = new Attribute("feature12");
    static private final Attribute feature13 = new Attribute("feature13");
    static private final Attribute feature14 = new Attribute("feature14");
    static private final Attribute feature15 = new Attribute("feature15");
    static private final Attribute feature16 = new Attribute("feature16");
    static private final Attribute feature17 = new Attribute("feature17");
    static private final Attribute feature18 = new Attribute("feature18");
    static private final Attribute feature19 = new Attribute("feature19");
    static private final Attribute feature20 = new Attribute("feature20");
    static private final Attribute feature21 = new Attribute("feature21");
    static private final Attribute feature22 = new Attribute("feature22");
    static private final Attribute feature23 = new Attribute("feature23");
    static private final Attribute feature24 = new Attribute("feature24");
    static private final Attribute feature25 = new Attribute("feature25");
    static private final Attribute feature26 = new Attribute("feature26");
    static private final Attribute feature27 = new Attribute("feature27");
    static private final Attribute feature28 = new Attribute("feature28");
    static private final Attribute feature29 = new Attribute("feature29");
    static private final Attribute feature30 = new Attribute("feature30");

    private final static List<String> classList = new ArrayList<String>()
    {
        {
            /*
             ********************************************** IMPORTANT
             * ***********************************************
             Please check this order if you don't wanna waste your time debugging and
             troubleshooting like I did. This order should be strictly maintained!
             Every time you train a new classifier, check the .arrf file to make sure that you are
             respecting the order of
              @attribute environment {cold,hot,neutral} in the training set
             *********************************************************************************************************
             */
            add("chew");
            add("swallow");
            add("talk");
            add("other");
        }
    };

    private final static ArrayList<Attribute> attributeList = new ArrayList<Attribute>(){
        {
            add(feature0);
            add(feature1);
            add(feature2);
            add(feature3);
            add(feature4);
            add(feature5);
            add(feature6);
            add(feature7);
            add(feature8);
            add(feature9);
            add(feature10);
            add(feature11);
            add(feature12);
            add(feature13);
            add(feature14);
            add(feature15);
            add(feature16);
            add(feature17);
            add(feature18);
            add(feature19);
            add(feature20);
            add(feature21);
            add(feature22);
            add(feature23);
            add(feature24);
            add(feature25);
            add(feature26);
            add(feature27);
            add(feature28);
            add(feature29);
            add(feature30);
            Attribute attributeClass = new Attribute("@@class@@", classList);
            add(attributeClass);
        }
    };

    private Classifier clf = null;

    public ClassificationModel(String model, Context context){
        mContext = context;
        getClassificationModel(model);
    }

    public String predict(final float[] features){
        Instances unknownSanple = new Instances("UNKNOWNSAMPLE", attributeList,1);
        unknownSanple.setClassIndex(unknownSanple.numAttributes()-1);
        DenseInstance newInstance = new DenseInstance(unknownSanple.numAttributes()){
            {
                setValue(feature0,features[0]);
                setValue(feature1,features[1]);
                setValue(feature2,features[2]);
                setValue(feature3,features[3]);
                setValue(feature4,features[4]);
                setValue(feature5,features[5]);
                setValue(feature6,features[6]);
                setValue(feature7,features[7]);
                setValue(feature8,features[8]);
                setValue(feature9,features[9]);
                setValue(feature10,features[10]);
                setValue(feature11,features[11]);
                setValue(feature12,features[12]);
                setValue(feature13,features[13]);
                setValue(feature14,features[14]);
                setValue(feature15,features[15]);
                setValue(feature16,features[16]);
                setValue(feature17,features[17]);
                setValue(feature18,features[18]);
                setValue(feature19,features[19]);
                setValue(feature20,features[20]);
                setValue(feature21,features[21]);
                setValue(feature22,features[22]);
                setValue(feature23,features[23]);
                setValue(feature24,features[24]);
                setValue(feature25,features[25]);
                setValue(feature26,features[26]);
                setValue(feature27,features[27]);
                setValue(feature28,features[28]);
                setValue(feature29,features[29]);
                setValue(feature30,features[30]);
            }
        };

        String predictedLabel=null;
        newInstance.setDataset(unknownSanple);
        double result = 0;
        try {
            result = clf.classifyInstance(newInstance);
            predictedLabel =classList.get(Double.valueOf(result).intValue());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  predictedLabel;
    }

    public void getClassificationModel(String modelName){
        ObjectInputStream objectInputStream = null;
        AssetManager assetManager= mContext.getAssets();
        InputStream inputStream= null;
        try {
            inputStream = assetManager.open(modelName);
            objectInputStream = new ObjectInputStream(inputStream);
            clf = (Classifier) objectInputStream.readObject();
            objectInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}
