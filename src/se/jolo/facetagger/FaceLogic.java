package se.jolo.facetagger;

import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.OpenCVFrameGrabber;
import com.googlecode.javacv.cpp.opencv_objdetect;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvResize;

public class FaceLogic implements Runnable {

    public static final int MAX_IMAGE_WIDTH = 420;
    String videoFile =null;
    JLabel videoLabel = null;
    JLabel videoPathLabel = null;
    JLabel picLabel = null;
    JLabel picInfoLabel = null;
    //LBPFaceRecognizer fr = LBPFaceRecognizer.getInstance();
    FrameGrabber grabber =null;
    int calcValue=0;
    boolean showInGUI = true;  //if set to false no images are shown in client... process time is way faster :-)

    public FaceLogic(String videoFile, JLabel videoLabel, JLabel videoPathLabel, JLabel picLabel, JLabel picInfoLabel) {
        this.videoFile = videoFile;
        this.videoLabel = videoLabel;
        this.videoPathLabel  = videoPathLabel;
        this.picLabel = picLabel;
        this.picInfoLabel = picInfoLabel;
        this.initGrabber();
    }

    public void run() {
        try {
           // fr.prepareForIdentifyFace();   //if recognition is done...
            IplImage grabbedImage;
            int noOfFrames = grabber.getLengthInFrames();
            int middleFrame = noOfFrames / 2;
            FaceDetector faceDetectorLE = new FaceDetector(FaceDetector.LeftEyeClassifier,CvScalar.RED);
            FaceDetector faceDetectorRE = new FaceDetector(FaceDetector.RightEyeClassifier,CvScalar.GREEN);
            FaceDetector faceDetectorFaces = new FaceDetector(FaceDetector.FaceClassifier,CvScalar.BLUE);
            //start in the middle -30 frames to simulate thumb creation for svtplay
            for (int fnr=middleFrame-60;fnr<middleFrame+60;fnr++) {
                grabber.setFrameNumber(fnr);
                grabbedImage = grabber.grab();

                //Save the "middle" file for reference:
                if (fnr==middleFrame) saveThumbs(grabbedImage,fnr, "svtplayref");

                //Resize given image size to a maximum width
                int percent;
                if (grabbedImage.width() > MAX_IMAGE_WIDTH) {
                    float percentD = (MAX_IMAGE_WIDTH/(float)grabbedImage.width())*100;
                    percent = (int)percentD;
                }
                else percent = 100;
                IplImage destImage = cvCreateImage(new CvSize(grabbedImage.width()*percent/100,grabbedImage.height()*percent/100),grabbedImage.depth(),grabbedImage.nChannels());
                cvResize(grabbedImage,destImage);

                //Clone grabbed image to use for thumbnail creation
                IplImage thumbClone = destImage.clone();

                //Use different detectors
                destImage = faceDetectorLE.getDetectionImage(destImage);
                destImage = faceDetectorRE.getDetectionImage(destImage);
                destImage = faceDetectorFaces.getDetectionImage(destImage);

                //Call thumbnail creation logic
                if (faceDetectorFaces.getRects(thumbClone).total() > 0) {
                    thumbPicker(faceDetectorFaces.getRects(thumbClone),faceDetectorLE.getRects(thumbClone),faceDetectorRE.getRects(thumbClone),fnr,thumbClone);
                }

                //Displays detection image
                if (showInGUI) {
                    videoLabel.setIcon(new ImageIcon( destImage.getBufferedImage() ));
                    videoPathLabel.setText("FrameA: " + fnr);
                }
            }
            grabber.stop();
        }
        catch (Exception e) {
            System.err.println("Error in run...: ");
            e.printStackTrace();
        }
    }

    /*
    Logic for thumb creation
     */
    private void thumbPicker(CvSeq faces, CvSeq rEye, CvSeq lEye, int fnr, IplImage grabbedImage) throws Exception {
        IplImage modifiedImg=cvCloneImage(grabbedImage);
        int value=30;
        //Light...
        cvAddS(modifiedImg,cvScalar(value,value,value,0),modifiedImg,null);
        if (showInGUI) {
            picLabel.setIcon(new ImageIcon(modifiedImg.getBufferedImage()));
            picInfoLabel.setText("FrameB:" + fnr);
        }

        int calcValueNew = faces.first().sizeof();
        if (!rEye.isNull()) {
            calcValueNew += rEye.total();
            calcValueNew += rEye.sizeof();
        }
        if (!lEye.isNull()) {
            calcValueNew += lEye.total();
            calcValueNew += lEye.sizeof();
        }
        if (calcValueNew > this.calcValue) {
            System.out.println("Old calc: " + calcValue);
            System.out.println("New calc: " + calcValueNew);
            this.calcValue = calcValueNew;
            saveThumbs(modifiedImg, fnr);
        }
    }

    private void saveThumbs(IplImage img, int fnr) throws Exception {
        this.saveThumbs(img, fnr, null);
    }

    /*
    Saving thumbnails...
     */
    private void saveThumbs(IplImage img, int fnr, String name) throws Exception {
        try {
            int percent=100;
            if (img.width() > 225) {
                float percentD = (225/(float)img.width())*100;
                percent = (int)percentD;
            }
            IplImage destImage = cvCreateImage(new CvSize(img.width()*percent/100,img.height()*percent/100),img.depth(),img.nChannels());
            cvResize(img,destImage);

            BufferedImage buff = destImage.getBufferedImage();
            System.out.println("Saving thumb");
            String defaultName = videoFile.replace('.','_') + "-" + fnr;
            if (name == null) {
                name = defaultName;
            }
            else {
                name = name+"_"+defaultName;
            }
            ImageIO.write(buff, "jpg", new File("./genimg/" + name + ".jpg"));
        } catch (Exception e) {
            e.printStackTrace();
          }
    }
    /*
    Initiate frame grabber...
     */
    private void initGrabber() {
        // Preload the opencv_objdetect module to work around a known bug.
        try {
            Loader.load(opencv_objdetect.class);
            // The available FrameGrabber classes include OpenCVFrameGrabber (opencv_highgui),
            // DC1394FrameGrabber, FlyCaptureFrameGrabber, OpenKinectFrameGrabber,
            // PS3EyeFrameGrabber, VideoInputFrameGrabber, and FFmpegFrameGrabber.
           /// grabber = new FFmpegFrameGrabber(videoFile);

            grabber = new OpenCVFrameGrabber(videoFile);
            //grabber = FrameGrabber.createDefault(0); //camera

            grabber.start();
        }
        catch(Exception e) {
            System.err.println("Error getting grabber: ");
            e.printStackTrace();
        }
    }

   /* private String recPersons(IplImage img, CvRect rect) throws Exception {
        String person = fr.identifyFace(fr.preprocessImage(img, rect));
        return person;
    }*/


}
