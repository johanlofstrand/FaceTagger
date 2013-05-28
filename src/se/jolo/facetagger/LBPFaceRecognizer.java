package se.jolo.facetagger;

import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.cpp.opencv_contrib.FaceRecognizer;
import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.*;
import com.googlecode.javacv.cpp.opencv_objdetect;
import com.googlecode.javacv.cpp.opencv_objdetect.CvHaarClassifierCascade;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import static com.googlecode.javacv.cpp.opencv_contrib.createFisherFaceRecognizer;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;
import static com.googlecode.javacv.cpp.opencv_highgui.cvSaveImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;
import static com.googlecode.javacv.cpp.opencv_objdetect.cvHaarDetectObjects;

//import com.googlecode.javacv.cpp.opencv_contrib.FaceRecognizerPtr;

public class LBPFaceRecognizer {

    private static String faceDataFolder = "data";
    public static String imageDataFolder = faceDataFolder;// + "images/";
    private static final String CASCADE_FILE = "haarcascade_frontalface_alt.xml";
    private static final String frBinary_DataFile = faceDataFolder + "frBinary.dat";
    public static final String personNameMappingFileName = faceDataFolder + "personNumberMap.properties";

    final CvHaarClassifierCascade cascade = new CvHaarClassifierCascade(cvLoad(CASCADE_FILE));
    private Properties dataMap = new Properties();
    private static LBPFaceRecognizer instance = new LBPFaceRecognizer();

    public static final int NUM_IMAGES_PER_PERSON =15;
    double binaryTreshold = 100;
    int highConfidenceLevel = 200;

   // FaceRecognizerPtr ptr_binary = null;
    private FaceRecognizer fr_binary = null;

    private LBPFaceRecognizer() {
        createModels();
        loadTrainingData();
    }

    public static LBPFaceRecognizer getInstance() {
        return instance;
    }

    private void createModels() {
        //fr_binary = createLBPHFaceRecognizer(1, 8, 8, 8, binaryTreshold);
        fr_binary = createFisherFaceRecognizer();
        //fr_binary = createEigenFaceRecognizer();
        //
      //  fr_binary = ptr_binary
    }

    protected CvSeq detectFace(IplImage originalImage) {
        CvSeq faces = null;
        Loader.load(opencv_objdetect.class);
        try {
            IplImage grayImage = IplImage.create(originalImage.width(), originalImage.height(), IPL_DEPTH_8U, 1);
            cvCvtColor(originalImage, grayImage, CV_BGR2GRAY);
            CvMemStorage storage = CvMemStorage.create();

            faces = cvHaarDetectObjects(grayImage, cascade, storage, 1.1, 3, CV_HAAR_DO_CANNY_PRUNING);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return faces;
    }

    public void prepareForIdentifyFace() throws Exception {
        this.loadTrainingData(); //to populate dataMap with existing persons
        this.retrainAll(); //loops all persons, get images and store binary data
    }

    public String identifyFace(IplImage image) {
        String personName = "";

        Set keys = dataMap.keySet();

        if (keys.size() > 0) {
            int[] ids = new int[1];
            double[] distance = new double[1];
            int result = -1;

            fr_binary.predict(image, ids, distance);
            //just deriving a confidence number against treshold
            result = ids[0];

            System.out.println("Prediction res: " + result);
            System.out.println("distance[0] res: " + distance[0]);

            //removed distance check...
            if (result > -1) {
                personName = (String) dataMap.get("" + result);
            }
        }
        System.out.println("found: " + personName);
        return personName;
    }


    //The logic to learn a new face is to store the recorded images to a folder and retrain the model
    //will be replaced once update feature is available
    public boolean learnNewFace(String personName, IplImage[] images) throws Exception {
        int memberCounter = dataMap.size();
        if(dataMap.containsValue(personName)){
            Set keys = dataMap.keySet();
            Iterator ite = keys.iterator();
            while (ite.hasNext()) {
                String personKeyForTraining = (String) ite.next();
                String personNameForTraining = (String) dataMap.getProperty(personKeyForTraining);
                if(personNameForTraining.equals(personName)){
                    memberCounter = Integer.parseInt(personKeyForTraining);
                    System.err.println("Person already exist.. re-learning..");
                }
            }
        }
        dataMap.put("" + memberCounter, personName);
        storeTrainingImages(personName, images);
        retrainAll();

        return true;
    }


    public IplImage preprocessImage(IplImage image, CvRect r){
        IplImage gray = cvCreateImage(cvGetSize(image), IPL_DEPTH_8U, 1);
        IplImage roi = cvCreateImage(cvGetSize(image), IPL_DEPTH_8U, 1);
        CvRect r1 = new CvRect(r.x()-10, r.y()-10, r.width()+10, r.height()+10);
        cvCvtColor(image, gray, CV_BGR2GRAY);
        cvSetImageROI(gray, r1);
        cvResize(gray, roi, CV_INTER_LINEAR);
        cvEqualizeHist(roi, roi);
        return roi;
    }

    private void retrainAll() throws Exception {
        Set keys = dataMap.keySet();
        if (keys.size() > 0) {
            MatVector trainImages = new MatVector(keys.size() * NUM_IMAGES_PER_PERSON);
            CvMat trainLabels = CvMat.create(keys.size() * NUM_IMAGES_PER_PERSON, 1, CV_32SC1);
            Iterator ite = keys.iterator();
            int count = 0;

            System.err.print("Loading images for training...");
            while (ite.hasNext()) {
                String personKeyForTraining = (String) ite.next();
                String personNameForTraining = (String) dataMap.getProperty(personKeyForTraining);
                IplImage[] imagesForTraining = readImages(personNameForTraining);
                IplImage grayImage = IplImage.create(imagesForTraining[0].width(), imagesForTraining[0].height(), IPL_DEPTH_8U, 1);

                for (int i = 0; i < imagesForTraining.length; i++) {
                    trainLabels.put(count, 0, Integer.parseInt(personKeyForTraining));
                    cvCvtColor(imagesForTraining[i], grayImage, CV_BGR2GRAY);
                    trainImages.put(count,grayImage);
                    count++;
                }
                //storeNormalizedImages(personNameForTraining, imagesForTraining);
            }

            System.err.println("done.");

            System.err.print("Training Binary model ....");
            fr_binary.train(trainImages, trainLabels);
            System.err.println("done.");
            storeTrainingData();
        }

    }

    private void loadTrainingData() {

        try {
            File personNameMapFile = new File(personNameMappingFileName);
            if (personNameMapFile.exists()) {
                FileInputStream fis = new FileInputStream(personNameMappingFileName);
                dataMap.load(fis);
                fis.close();
            }

            File binaryDataFile = new File(frBinary_DataFile);
            if (binaryDataFile.exists()) {
                System.out.println("Loading Binary model ....");
                fr_binary.load(frBinary_DataFile);
                System.out.println("done");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void storeTrainingData() throws Exception {
        System.err.print("Storing training models ....");

        File binaryDataFile = new File(frBinary_DataFile);
        if (binaryDataFile.exists()) {
            binaryDataFile.delete();
        }
        fr_binary.save(frBinary_DataFile);

        File personNameMapFile = new File(personNameMappingFileName);
        if (personNameMapFile.exists()) {
            personNameMapFile.delete();
        }
        FileOutputStream fos = new FileOutputStream(personNameMapFile, false);
        dataMap.store(fos, "");
        fos.close();

        System.err.println("done.");
    }


    public void storeTrainingImages(String personName, IplImage[] images) {
        for (int i = 0; i < images.length; i++) {
            String imageFileName = imageDataFolder + System.getProperty("file.separator") + personName + "_" + i + ".bmp";
            File imgFile = new File(imageFileName);
            if (imgFile.exists()) {
                imgFile.delete();
            }
            cvSaveImage(imageFileName, images[i]);
        }
    }

    private IplImage[] readImages(String personName) {
        File imgFolder = new File(imageDataFolder);
        IplImage[] images = null;
        if (imgFolder.isDirectory() && imgFolder.exists()) {
            images = new IplImage[NUM_IMAGES_PER_PERSON];
            int picid = 2;
            for (int i = 0; i < NUM_IMAGES_PER_PERSON; i++) {
                picid++; //verkar bli dåliga bilder i början av kameran...hoppar över de två första
                String imageFileName = imageDataFolder + System.getProperty("file.separator")  + personName + "_" + picid + ".bmp";
                opencv_core.IplImage img = cvLoadImage(imageFileName);
                images[i] = img;
            }

        }
        return images;
    }


}