package com.echo.pinterest.conf;

import java.util.List;

/**
 * Created by jiangecho on 16/3/15.
 */
public class BoardConf {

    public List<String> sourceBoard;
    /**
     * userName : HannahHutch1995
     * boardName : lets-get-fit
     */

    public List<UserBoardEntity> userBoard;

    public static class UserBoardEntity {
        public String userName;
        public String boardName;
    }
}
