package main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.net.URLEncoder;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;

/**
 * A simple app to download any Pinterest user's pins to a local directory.
 */
public class Main {

    private static final int TIMEOUT = 10000;

    /**
     * Verify arguments, and handle some errors
     *
     * @param args arguments (needs a string for username or abort)
     */
    public static void main(final String[] args) {
        System.out.println("Welcome to PinCrawl, this may take a while...");

        // get username
        String _username;
        if (args.length > 0) {
            _username = args[0];
        } else {
            System.out.println("ERROR: please enter a user name, aborting.");
            return;
        }

        try {
            //process(_username, "Fitness Inspiration");
            process(_username, null);
        } catch (IOException e) {
            System.out.println("ERROR: IOException, probably a messed up URL.");
        }
    }


    private static void processBoard(Element boardDoc, String boardName, String rootDir) throws IOException {
        makeDir(rootDir + File.separator + boardName);

        System.out.println("...Downloading '" + boardName + "'...");
        final Elements pageLinks = boardDoc.select("a[href].pinImageWrapper");
        for (final Element pageLink : pageLinks) {
            // connect to image page and get direct link to image then save it
            final Document pageDoc = Jsoup.connect(pageLink.absUrl("href")).timeout(TIMEOUT).get();

            // TODO yeah, I just need the image url
            String imageUrl = pageDoc.select("meta[property=twitter:image:src]").get(0).attr("content");
//                final Elements imgLinks = pageDoc.select("img[src].pinImage");
//                for (final Element imgLink : imgLinks) {
//                    saveImage(imgLink.absUrl("src"), rootDir + "\\" + boardName, imgCount);
//                }

            if (imageUrl != null) {
                saveImage(imageUrl, rootDir + File.separator + boardName);
            }
        }

    }

    /**
     * eg: https://jp.pinterest.com/source/bodybuilding.com
     *
     * @param domainName
     */
    private static void process(String domainName) {

    }

    /**
     * eg: https://jp.pinterest.com/HannahHutch1995/lets-get-fit/
     *
     * @param aUserName
     * @param aBoardName, when aBoardName is null, download all boards
     */
    private static void process(String aUserName, String aBoardName) throws IOException {
        // validate username and connect to their page
        Document doc;
        try {
            // usernames:
            // ihealthjournal
            // younghipfit
            doc = Jsoup.connect("https://www.pinterest.com/" + aUserName + "/").timeout(TIMEOUT).get();
        } catch (HttpStatusException e) {
            System.out.println("ERROR: not a valid user name, aborting.");
            return;
        }
        // list of board urls
        final Elements boardLinks = doc.select("a[href].boardLinkWrapper");

        // make root directory
        String rootDir = aUserName;
        makeDir(rootDir);

        for (final Element boardLink : boardLinks) {
            // connect to board via url and get all page urls
            final Document boardDoc = Jsoup.connect(boardLink.absUrl("href")).timeout(TIMEOUT).get();

            // parse and format board name and make its directory
            // new, get name from Module User boardRepTitle hasText thumb title inside, instead of hover
            // hate having to use all these loops, wasn't getting selector and .attr working properly and give up
            // cause I was tired, so loops it is
            String boardName = null;
            for (Element el : boardLink.children()) {
                if (el.className().equals("boardName hasBoardContext")) {
                    for (Element el2 : el.children()) {
                        if (el2.className().equals("Module User boardRepTitle hasText thumb")) {
                            for (Element el3 : el2.children()) {
                                if (el3.className().equals("title")) {
                                    boardName = el3.childNode(0).outerHtml();
                                }
                            }
                        }
                    }
                }
            }
            if (boardName == null || boardName.isEmpty()) {
                System.out.println("ERROR: couldn't find name of board, it's the developer's fault. Aborting.");
                return;
            }

            if (aBoardName != null && !boardName.equals(aBoardName)) {
                return;
            }

            boardName = URLEncoder.encode(boardName, "UTF-8");
            boardName = boardName.replace('+', ' ');
            // plus extra length safety now
            if (boardName.length() > 256) {
                boardName = boardName.substring(0, 256);
            }

            processBoard(boardDoc, boardName, rootDir);
        }

        System.out.println("All pins downloaded, to " + System.getProperty("user.dir")
                + File.separator + rootDir + File.separator);
        System.out.println("Thanks for using PinCrawl!");
    }

    /**
     * Makes a directory with the filename provided, fails if it already exists
     * TODO: allow arguments for overwrite, subtractive, and additive changes
     *
     * @param name name of the file
     */
    public static boolean makeDir(String name) {
        File file = new File(name);
        if (!file.exists()) {
            if (file.mkdir()) {
                return true;
            } else {
                System.out.println("ERROR: Failed to create directory '" + name + "', aborting.");
            }
        } else {
            System.out.println("ERROR: Directory '" + name + "' already exists, aborting.");
        }
        return false;
    }

    /**
     * Saves an image from the specified URL to the path with the name count
     * TODO: allow gifs, maybe
     *
     * @param srcUrl url of image
     * @param path   path to save image (in root\board)
     * @throws IOException
     */
    public static void saveImage(String srcUrl, String path) throws IOException {
        BufferedImage image;
        String imageName = srcUrl.substring(srcUrl.lastIndexOf('/') + 1, srcUrl.length());
        File imageFile = new File(path + File.separator + imageName);
        if (imageFile.exists()) {
            System.out.println(imageName + " exists");
            return;
        } else {
            System.out.println("downloading " + srcUrl);
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
