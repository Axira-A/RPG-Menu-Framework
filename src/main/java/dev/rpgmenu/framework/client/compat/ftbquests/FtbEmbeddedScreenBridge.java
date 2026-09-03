package dev.rpgmenu.framework.client.compat.ftbquests;

/**
 * Dispatch-scoped bridge used by the optional FTB Library mixin.  It only captures BaseScreen transitions
 * originating from an active embedded quest interaction; normal FTB screens remain completely untouched.
 */
public final class FtbEmbeddedScreenBridge {
    private static final ThreadLocal<FtbQuestEmbeddedView> ACTIVE_HOST = new ThreadLocal<>();

    private FtbEmbeddedScreenBridge() {
    }

    static void begin(FtbQuestEmbeddedView host) {
        ACTIVE_HOST.set(host);
    }

    static void end(FtbQuestEmbeddedView host) {
        if (ACTIVE_HOST.get() == host) ACTIVE_HOST.remove();
        host.finishFtbDispatch();
    }

    public static boolean captureOpen(Object baseScreen) {
        FtbQuestEmbeddedView host = ACTIVE_HOST.get();
        return host != null && host.queueNestedOpen(baseScreen);
    }

    public static boolean captureClose(Object baseScreen) {
        FtbQuestEmbeddedView host = ACTIVE_HOST.get();
        return host != null && host.queueNestedClose(baseScreen);
    }
}
