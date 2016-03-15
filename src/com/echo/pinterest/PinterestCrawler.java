package com.echo.pinterest;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * A simple app to download any Pinterest user's pins to a local directory.
 */
public class PinterestCrawler {

    private static final int TIMEOUT = 10000;
    private static final String PINTEREST_BASE_URL = "https://www.pinterest.com/";


    @Option(name = "-s", usage = "is source board?")
    private boolean isSourceBoard = false;

    @Option(name = "-d", usage = "domain name ")
    private String domain;

    @Option(name = "-u", usage = "user name")
    private String userName;

    @Option(name = "-b", usage = "board name")
    private String boardName = null;


    /**
     * Verify arguments, and handle some errors
     *
     * @param args arguments (needs a string for username or abort)
     */
    public static void main(final String[] args) {
        System.out.println("Welcome to PinCrawl, this may take a while...");
        new PinterestCrawler().doMain(args);
    }

    private void doMain(final String[] args) {
        CmdLineParser cmdLineParser = new CmdLineParser(this);

        try {
            cmdLineParser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.out.println("TODO");
            return;
        }

        try {
            if (isSourceBoard) {
                process(domain);
            } else {
                process(userName, boardName);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void processBoard(Element boardDoc, String boardName, String rootDir) throws IOException {
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
    private void process(String domainName) throws IOException {
        Document boardDoc;
        try {
            boardDoc = Jsoup.connect(PINTEREST_BASE_URL + "source/" + domainName).timeout(TIMEOUT).get();
        } catch (HttpStatusException e) {
            System.out.println("ERROR: not a valid user name, aborting.");
            return;
        }

        // make root directory
        String rootDir = "source";
        makeDir(rootDir);
        processBoard(boardDoc, domainName, rootDir);
    }

    /**
     * eg: https://jp.pinterest.com/HannahHutch1995/lets-get-fit/
     *
     * @param aUserName
     * @param aBoardName, when aBoardName is null, download all boards
     *                    attention: aBoardName is the string in the url, not the real board name
     *                    please check the url for the real board name
     */
    private void process(String aUserName, String aBoardName) throws IOException {

        // make root directory
        String rootDir = aUserName;
        makeDir(rootDir);

        // validate username and connect to their page
        Document doc;
        try {
            if (aBoardName != null) {

                doc = Jsoup.connect(PINTEREST_BASE_URL + aUserName + "/" + aBoardName).timeout(TIMEOUT).get();
                processBoard(doc, aBoardName, rootDir);
                return;
            }

            doc = Jsoup.connect(PINTEREST_BASE_URL + aUserName + "/").timeout(TIMEOUT).get();
        } catch (HttpStatusException e) {
            e.printStackTrace();
            System.out.println("ERROR: not a valid user name, aborting.");
            return;
        }
        System.out.println("will download all boards");
        // list of board urls
        final Elements boardLinks = doc.select("a[href].boardLinkWrapper");

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
    public boolean makeDir(String name) {
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
    public void saveImage(String srcUrl, String path) throws IOException {
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
