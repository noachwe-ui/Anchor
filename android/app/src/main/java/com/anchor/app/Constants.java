package com.anchor.app;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class Constants {
    private Constants() {}

    // Single source of truth for monitored packages. Previously duplicated
    // identically in AnchorAppMonitorService and FloatingBubbleService —
    // update this list here only, both services read from it.
    public static final Set<String> TARGETS = new HashSet<>(Arrays.asList(
        "com.android.chrome", "com.chrome.beta", "com.chrome.dev",
        "com.sec.android.app.sbrowser", "org.mozilla.firefox",
        "org.mozilla.firefox_beta", "com.opera.browser", "com.brave.browser",
        "com.microsoft.emmx", "com.duckduckgo.mobile.android",
        "com.google.android.youtube",
        "com.whatsapp", "com.whatsapp.w4b",
        "com.instagram.android", "com.facebook.katana", "com.facebook.lite",
        "com.facebook.orca", "com.facebook.mlite",
        "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
        "com.twitter.android", "com.snapchat.android", "com.reddit.frontpage",
        "org.telegram.messenger", "org.telegram.messenger.web",
        "com.discord", "com.pinterest", "com.linkedin.android"
    ));
}
