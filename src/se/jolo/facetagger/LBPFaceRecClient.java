package se.jolo.facetagger;

import com.googlecode.javacv.*;

import javax.swing.*;

import java.awt.*;

import static com.googlecode.javacv.cpp.opencv_core.*;

public class LBPFaceRecClient {

    LBPFaceRecognizer fr = LBPFaceRecognizer.getInstance();

    FrameGrabber grabber = null; //new OpenCVFrameGrabber(0);
    CanvasFrame frame = null;
    JFrame infoFrame= null;
    JTextArea infoArea = null;

    public LBPFaceRecClient() {
        infoFrame = new JFrame("Info");
        infoFrame.getContentPane().add(new JLabel("Hittade personer"));

        infoArea = new JTextArea();
        infoArea.setColumns(15);
        infoArea.setRows(30);
        infoArea.setLineWrap(true);

        infoFrame.getContentPane().add(infoArea);
        infoFrame.pack();
        infoFrame.setVisible(true);
    }

    public static void main(String[] args) {
        System.out.println("START");
        LBPFaceRecClient client = new LBPFaceRecClient();

        client.findPerson();
    }

    public void findPerson() {

        try {
            LBPFaceRecognizer fr = LBPFaceRecognizer.getInstance();
            fr.prepareForIdentifyFace();

            grabber = new OpenCVFrameGrabber(0);
            int width = 480;
            grabber.setImageWidth(width);
            frame = new CanvasFrame("FaceRec", CanvasFrame.getDefaultGamma()/grabber.getGamma());
            frame.setLayout(new BorderLayout());
            frame.pack();
            frame.setVisible(true);
            frame.setLocationRelativeTo(null);   //centrera

            grabber.start();
            IplImage img;

            while (frame.isVisible() && (img = grabber.grab())!=null) {
                IplImage snapshot = cvCreateImage(cvGetSize(img), img.depth(), img.nChannels());
                cvFlip(img, snapshot, 1);
                CvSeq faces = fr.detectFace(img);
                int total = faces.total();
                //System.out.println("total faces: " + total);
                CvRect rect =null;
                for (int i=0; i<total; i++) {   //loop faces
                    //System.out.println("face nr: " + i);
                    rect = new CvRect(cvGetSeqElem(faces, 0));
                    int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
                    cvRectangle(img, cvPoint(x, y), cvPoint(x+w, y+h), CvScalar.RED, 1, CV_AA, 0);
                    frame.showImage(img);

                    //Thread.sleep(500);
                    recPersons(img, rect);
                    //Thread.sleep(500);
                }
            }

            grabber.stop();

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void recPersons(IplImage img, CvRect rect) throws Exception {
        String person = fr.identifyFace(fr.preprocessImage(img, rect));
        infoArea.append(person+"\n");

    }

}