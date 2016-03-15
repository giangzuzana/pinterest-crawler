package com.echo.pinterest.process;

/**
 * Created by jiangecho on 16/3/15.
 */
public class DBHandler implements PinHandler {
    /**
     * @param imageUrl
     * @param userName
     * @param boardName
     */
    @Override
    public void handle(String imageUrl, String userName, String boardName) {
        // TODO
    }

    /**
     * @param imageUrl
     * @param sourceBoardName eg: the source name of https://jp.pinterest.com/source/bodybuilding.com is bodybuilding.com
     */
    @Override
    public void handle(String imageUrl, String sourceBoardName) {
        // TODO
    }
}
