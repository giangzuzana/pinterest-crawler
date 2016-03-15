package com.echo.pinterest.process;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Created by jiangecho on 16/3/15.
 */
public class DownloadHandler implements PinHandler {
    private static final String ROOT_BASE_DIR = "images" + File.separator;
    private String rootDir = "images" + File.separator;

    /**
     * @param imageUrl
     * @param userName
     * @param boardName
     */
    @Override
    public void handle(String imageUrl, String userName, String boardName) {
        rootDir = ROOT_BASE_DIR + userName + File.separator + boardName;
        try {
            saveImage(imageUrl, rootDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param imageUrl
     * @param sourceBoardName eg: the source board name of https://jp.pinterest.com/source/bodybuilding.com is bodybuilding.com
     */
    @Override
    public void handle(String imageUrl, String sourceBoardName) {
        rootDir = ROOT_BASE_DIR + "source";
        try {
            saveImage(imageUrl, rootDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves an image from the specified URL to the path with the name count
     * TODO: allow gifs, maybe
     *
     * @param srcUrl url of image
     * @param path   path to save image (in root\board)
     * @throws IOException
     */
    private void saveImage(String srcUrl, String path) throws IOException {
        BufferedImage image;
        String imageName = srcUrl.substring(srcUrl.lastIndexOf('/') + 1, srcUrl.length());
        File pathFile = new File(path);
        if (!pathFile.exists()) {
            if (!pathFile.mkdirs()) {
                System.out.println("mkdirs failed: " + path);
                return;
            }
        }

        File imageFile = new File(path + File.separator + imageName);
        System.out.println("downloading " + srcUrl);

        if (imageFile.exists()) {
            System.out.println("image exists: " + imageName);
            return;
        }

        URL url = new URL(srcUrl);
        if (srcUrl.endsWith(".gif"))
            System.out.println("ERROR: .gifs not supported, continuing");
        try {
            image = ImageIO.read(url);
            ImageIO.write(image, "png", imageFile);
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.println("ERROR: Image too big, probably a .gif that didn't end with .gif, continuing");
        }
    }
}
