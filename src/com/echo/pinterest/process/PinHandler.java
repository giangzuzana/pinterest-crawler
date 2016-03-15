package com.echo.pinterest.process;

/**
 * Created by jiangecho on 16/3/15.
 */
public interface PinHandler {
    /**
     * @param imageUrl
     * @param userName
     * @param boardName
     */
    void handle(String imageUrl, String userName, String boardName);

    /**
     * @param imageUrl
     * @param sourceBoardName eg: the source name of https://jp.pinterest.com/source/bodybuilding.com is bodybuilding.com
     */
    void handle(String imageUrl, String sourceBoardName);

    boolean init();

    void deInit();
}
