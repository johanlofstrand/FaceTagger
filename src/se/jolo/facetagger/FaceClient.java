package se.jolo.facetagger;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Created with IntelliJ IDEA.
 * User: johanlofstrand
 * Date: 2013-02-17
 * Time: 13:23
 * To change this template use File | Settings | File Templates.
 */
public class FaceClient {

    //http://www.svtplay.se/video/1028694/del-3-av-10?type=embed&position=0

    JTextField videoField = new JTextField(".mp4");
    JButton videoButton = new JButton("Ok");
    String videoPath;
    JLabel videoPathLabel = new JLabel();
    JLabel picInfoLabel = new JLabel();
    JFrame guiFrame = new JFrame();
    JLabel videoLabel = new JLabel();
    JLabel picLabel = new JLabel();


    public static void main(String args[]) {
        FaceClient t = new FaceClient();
        t.createGUI();
    }

    public FaceClient() {
        createGUI();
    }

    public void createGUI() {

        guiFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        guiFrame.setTitle("Thumbnail creator");
        guiFrame.setSize(1024,600);
        guiFrame.setLocationRelativeTo(null);   //centrera

        //-----------NORTH---------------
        //Panel för att välja videoström
        final JPanel northPanel = new JPanel();
        final JPanel northNorthPanel = new JPanel();
        final JLabel infoText = new JLabel("Välj video");
        videoButton = new JButton("Ok");
        videoButton.addActionListener(new VideoButtonListener());
        northNorthPanel.setLayout(new BorderLayout());
        northNorthPanel.add(infoText, BorderLayout.WEST);
        northNorthPanel.add(videoField, BorderLayout.CENTER);
        northNorthPanel.add(videoButton, BorderLayout.EAST);
        northPanel.setLayout(new BorderLayout());
        northPanel.add(northNorthPanel,BorderLayout.CENTER);
        guiFrame.add(northPanel, BorderLayout.NORTH);
        //---------------------------------

        //-----------CENTER---------------
        //Panel för att visa videoström och vald bild

        Border border = BorderFactory.createEmptyBorder(15,15,15,15);

        JPanel southWestPanel = new JPanel();
        southWestPanel.setLayout(new BorderLayout());

        JPanel southEastPanel = new JPanel();
        southEastPanel.setLayout(new BorderLayout());

        videoLabel.setBorder(border);
        southWestPanel.add(videoLabel, BorderLayout.NORTH);

        videoPathLabel.setBorder(border);
        southWestPanel.add(videoPathLabel,BorderLayout.CENTER);

        picLabel.setBorder(border);
        southEastPanel.add(picLabel, BorderLayout.NORTH);

        picInfoLabel.setBorder(border);
        southEastPanel.add(picInfoLabel,BorderLayout.CENTER);

        southWestPanel.setBorder(border);
        southEastPanel.setBorder(border);

        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BorderLayout());
        southPanel.setBorder(border);

        southPanel.add(southWestPanel, BorderLayout.WEST);
        southPanel.add(southEastPanel, BorderLayout.EAST);

        guiFrame.add(southPanel,BorderLayout.CENTER);

        //make sure the JFrame is visible
        guiFrame.setVisible(true);
    }

    class VideoButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            videoPath = videoField.getText();
            System.out.println(videoPath);

            FaceLogic faceLogic = new FaceLogic(videoPath,videoLabel,videoPathLabel,picLabel,picInfoLabel);
            try {
                Thread thumblerLogicThread = new Thread(faceLogic);
                thumblerLogicThread.start();
            }
            catch(Exception e) {
                System.err.println("Some error...");
                e.printStackTrace();
            }
        }
    }

}
