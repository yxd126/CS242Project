package View;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static View.utils.ViewUtils.*;

/**
 * Created by dajun on 4/17/17.
 */
public class GamePanel extends JPanel {

    private File f;
    private BufferedImage image;
    private Image backgroundImage;

    public GamePanel() throws IOException {

        //initialize helper function
        f = new File("src/View/img/BoardBackground.jpg");
        image = ImageIO.read(f);
        backgroundImage = image.getScaledInstance(GAME_WIDTH,GAME_HEIGHT,Image.SCALE_SMOOTH);
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(backgroundImage, 0, 0, this); // see javadoc for more info on the parameters
    }

}
