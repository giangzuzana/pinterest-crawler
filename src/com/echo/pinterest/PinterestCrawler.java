package com.echo.pinterest;

import com.echo.pinterest.process.DBHandler;
import com.echo.pinterest.process.DownloadHandler;
import com.echo.pinterest.process.PinHandler;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.net.URLEncoder;

/**
 * A simple app to download any Pinterest user's pins to a local directory.
 */
public class PinterestCrawler {

    private static final int TIMEOUT = 10000;
    private static final String PINTEREST_BASE_URL = "https://www.pinterest.com/";


    @Option(name = "-s", usage = "is source board?")
    private boolean isSourceBoard = false;

    @Option(name = "-u", usage = "user name")
    private String userName;

    @Option(name = "-b", usage = "board name")
    private String boardName = null;

    @Option(name = "-h", usage = "handler: download or db")
    private String handler;

    private PinHandler pinHandler;

    /**
     * Verify arguments, and handle some errors
     *
     * @param args arguments (needs a string for username or abort)
     */
    // -u HannahHutch1995 -b lets-get-fit -h download
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

        if (handler == null) {
            System.err.println("-h download or handler");
            return;
        } else {
            if (handler.equalsIgnoreCase("download")) {
                pinHandler = new DownloadHandler();
            } else {
                pinHandler = new DBHandler();
            }
        }

        if (!pinHandler.init()) {
            System.out.println("init error");
            return;
        }

        try {
            if (isSourceBoard) {
                crawl(boardName);
            } else {
                crawl(userName, boardName);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        pinHandler.deInit();
    }


    /**
     * @param boardDoc
     * @param userName      when isSourceBoard is true, please ignore this argument
     * @param boardName
     * @param isSourceBoard
     * @throws IOException
     */
    private void crawlBoard(Element boardDoc, String userName, String boardName, boolean isSourceBoard) throws IOException {

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
                if (isSourceBoard) {
                    pinHandler.handle(imageUrl, boardName);
                } else {
                    pinHandler.handle(imageUrl, userName, boardName);
                }
            }
        }

    }

    /**
     * eg: https://jp.pinterest.com/source/bodybuilding.com
     *
     * @param domainName
     */
    private void crawl(String domainName) throws IOException {
        Document boardDoc;
        try {
            boardDoc = Jsoup.connect(PINTEREST_BASE_URL + "source/" + domainName).timeout(TIMEOUT).get();
        } catch (HttpStatusException e) {
            System.out.println("ERROR: not a valid user name, aborting.");
            return;
        }

        // make root directory
        crawlBoard(boardDoc, null, domainName, true);
    }

    /**
     * eg: https://jp.pinterest.com/HannahHutch1995/lets-get-fit/
     *
     * @param aUserName
     * @param aBoardName, when aBoardName is null, download all boards
     *                    attention: aBoardName is the string in the url, not the real board name
     *                    please check the url for the real board name
     */
    private void crawl(String aUserName, String aBoardName) throws IOException {

        // validate username and connect to their page
        Document doc;
        try {
            if (aBoardName != null) {

                doc = Jsoup.connect(PINTEREST_BASE_URL + aUserName + "/" + aBoardName).timeout(TIMEOUT).get();
                crawlBoard(doc, aUserName, aBoardName, false);
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

            crawlBoard(boardDoc, aUserName, boardName, false);
        }

        System.out.println("Thanks for using PinCrawl!");
    }

}
