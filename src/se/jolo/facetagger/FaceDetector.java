package se.jolo.facetagger;

import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_objdetect;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;
import static com.googlecode.javacv.cpp.opencv_objdetect.cvHaarDetectObjects;

/**
 * User: johanlofstrand
 */
public class FaceDetector {

    private CvScalar colorForDetectionRectangle;
    private opencv_objdetect.CvHaarClassifierCascade classifier;
    public static final String FaceClassifier = "haarcascade_frontalface_alt.xml";
    public static final String LeftEyeClassifier = "haarcascade_lefteye_2splits.xml";
    public static final String RightEyeClassifier = "haarcascade_righteye_2splits.xml";


    public FaceDetector(String classifier,
                        CvScalar colorForDetectionRectangle) {
        this.classifier = (new FaceDetectorClassifier(classifier)).getClassifier();
        this.colorForDetectionRectangle = colorForDetectionRectangle;
    }

    /*
    Returns given imageForDetection with identified area marked with a coloured rectangle
     */
    public IplImage getDetectionImage(IplImage imageForDetection) throws Exception {
        CvSeq rects = this.getRects(imageForDetection);
        for (int i = 0; i < rects.total(); i++) {
            CvRect r = new CvRect(cvGetSeqElem(rects, i));
            int x = r.x(), y = r.y(), w = r.width(), h = r.height();
            cvRectangle(imageForDetection, cvPoint(x, y), cvPoint(x+w, y+h), colorForDetectionRectangle, 1, CV_AA, 0);
        }
        return imageForDetection;
    }
    /*
    Returns detected sequence for given imageForDetection
     */
    public CvSeq getRects(IplImage imageForDetection) throws Exception {
        CvMemStorage storage = opencv_core.CvMemStorage.create();
        cvClearMemStorage(storage);
        opencv_core.IplImage grayImage = opencv_core.IplImage.create(imageForDetection.width(),
                imageForDetection.height(), IPL_DEPTH_8U, 1);
        cvCvtColor(imageForDetection, grayImage, CV_BGR2GRAY);
        opencv_core.CvSeq rects = cvHaarDetectObjects(grayImage, classifier, storage,
                1.1, 5, CV_HAAR_DO_CANNY_PRUNING);
        return rects;
   }

    /*
    Initiates a classifier
     */
    private class FaceDetectorClassifier {

        private String classifierString;
        private opencv_objdetect.CvHaarClassifierCascade classifier;
        public FaceDetectorClassifier(String classifierString) {
             this.classifierString = classifierString;
        }
        public opencv_objdetect.CvHaarClassifierCascade getClassifier() {
            this.initClassifier(classifierString);
            return classifier;
        }
        private void initClassifier(String classifierName) {
            // We can "cast" Pointer objects by instantiating a new object of the desired class.
            classifier = new opencv_objdetect.CvHaarClassifierCascade(cvLoad(classifierName));
            if (classifier.isNull()) {
                System.err.println("Error loading classifier file \"" + classifierName + "\".");
                System.exit(1);
            }
        }
    }

      /* private void showDetecionOfContours(IplImage grayImage) throws Exception {
        // Let's find some contours! but first some thresholding...
        cvThreshold(grayImage, grayImage, 64, 255, CV_THRESH_BINARY);

        // To check if an output argument is null we may call either isNull() or equals(null).
        CvSeq contour = new CvSeq(null);
        cvFindContours(grayImage, storage, contour, Loader.sizeof(CvContour.class),
                CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);
        while (contour != null && !contour.isNull()) {
            if (contour.elem_size() > 0) {
                CvSeq points = cvApproxPoly(contour, Loader.sizeof(CvContour.class),
                        storage, CV_POLY_APPROX_DP, cvContourPerimeter(contour)*0.02, 0);
                cvDrawContours(imageForDetection, points, CvScalar.BLUE, CvScalar.BLUE, -1, 1, CV_AA);
            }
            contour = contour.h_next();
        }
    } */

}
