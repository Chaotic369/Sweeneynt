package com.sweeney.hunter;

/** The only two things you may ever need to change. */
public final class Config {
    private Config() {}

    /** Opened when you tap a notification. Change if your site URL differs. */
    public static final String SITE_URL = "https://chaotic369.github.io/Sweeneynt/";
    /** Realtime Database URL (REST). Same project as the website. */
    public static final String DB_URL = "https://hunter-prey-default-rtdb.firebaseio.com";

    public static final String CHANNEL_ID = "prey_messages";
}
