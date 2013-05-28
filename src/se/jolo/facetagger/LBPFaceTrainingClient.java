package se.jolo.facetagger;

import com.googlecode.javacv.*;

import javax.swing.*;

import java.awt.*;

import static com.googlecode.javacv.cpp.opencv_core.*;

public class LBPFaceTrainingClient {

    IplImage trainImages[] = new IplImage[21];
    LBPFaceRecognizer fr = LBPFaceRecognizer.getInstance();

    FrameGrabber grabber = null; //new OpenCVFrameGrabber(0);
    CanvasFrame frame = null;
    int imageCounter=0 ;


    public LBPFaceTrainingClient() {
    }

    public static void main(String[] args) {
        System.out.println("START");
        LBPFaceTrainingClient client = new LBPFaceTrainingClient();
        client.addPerson("Gereon");
    }

    public void addPerson(String personName) {

        try {
            LBPFaceRecognizer fr = LBPFaceRecognizer.getInstance();

            grabber = new OpenCVFrameGrabber(0);
            int width = 480;
            grabber.setImageWidth(width);
            frame = new CanvasFrame("FaceRec", CanvasFrame.getDefaultGamma()/grabber.getGamma());

            grabber.start();
            IplImage img;

            while (frame.isVisible() && (img = grabber.grab())!=null) {
                IplImage snapshot = cvCreateImage(cvGetSize(img), img.depth(), img.nChannels());
                cvFlip(img, snapshot, 1);
                CvSeq faces = fr.detectFace(img);
                int total = faces.total();
                System.out.println("total faces: " + faces);
                CvRect rect =null;
                for (int i=0; i<total; i++) {   //loop faces
                    System.out.println("face nr: " + i);
                    rect = new CvRect(cvGetSeqElem(faces, 0));
                    int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
                    cvRectangle(img, cvPoint(x, y), cvPoint(x+w, y+h), CvScalar.RED, 1, CV_AA, 0);
                    frame.showImage(img);
                    trainImage(img, rect);
                 }
            }

            grabber.stop();
            System.out.println("Stopped grabbing. Got: " + trainImages.length + " images");

            fr.learnNewFace(personName, trainImages);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
     }

     private void trainImage(IplImage img, CvRect rect) throws Exception {

        int xNew=0;
        int xOld=0;

        if (imageCounter<20) {  //if we need more training images...
            imageCounter++;
            if (rect!=null) {
                xNew=rect.width(); //only want the "best match", e.g. the largest sizes face...
                if (xNew<(xOld/2)) {
                    imageCounter--;
                }
                xOld=xNew;
            }
            trainImages[imageCounter] = fr.preprocessImage(img, rect);
        }
    }

    private void recPersons(IplImage img, CvRect rect) throws Exception {
        String person = fr.identifyFace(fr.preprocessImage(img, rect));
        frame.setLayout(new BorderLayout());
        frame.add(new JLabel(person), BorderLayout.EAST);

    }
}