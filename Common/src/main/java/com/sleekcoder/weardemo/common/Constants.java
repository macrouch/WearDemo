package com.sleekcoder.weardemo.common;

/**
 * Created by matthew on 7/8/14.
 */
public final class Constants {

    private Constants() {};

    public static final int WATCH_ONLY_ID = 2;
    public static final int PHONE_ONLY_ID = 3;
    public static final int BOTH_ID = 4;

    public static final String BOTH_PATH = "/both";
    public static final String WATCH_ONLY_PATH = "/watch-only";
    public static final String KEY_NOTIFICATION_ID = "notification-id";
    public static final String KEY_TITLE = "title";
    public static final String KEY_CONTENT = "content";
    public static final String START_PHONE_ACTIVITY_PATH = "/start/PhoneActivity";
    public static final String START_WEAR_ACTIVITY_PATH = "/start/WearActivity";
    public static final String IMAGE_PATH = "/image";

    public static final String ACTION_DISMISS
            = "com.sleekcoder.weardemo.DISMISS";
}
