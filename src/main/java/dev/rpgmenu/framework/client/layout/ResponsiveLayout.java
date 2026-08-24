package dev.rpgmenu.framework.client.layout;

/** Constraint-based layout calculated from the current scaled window, never a 1920x1080 template. */
public record ResponsiveLayout(Profile profile, UiRect frame, UiRect topTabs, UiRect leftPanel, UiRect rightPanel,
                               UiRect subTabs, UiRect search, UiRect grid, UiRect details, UiRect character,
                               UiRect status, UiRect footer) {
    public enum Profile { DESKTOP_LARGE, DESKTOP, COMPACT, SMALL }

    public static ResponsiveLayout calculate(int width, int height) {
        Profile profile = width >= 1180 && height >= 700 ? Profile.DESKTOP_LARGE
                : width >= 860 && height >= 520 ? Profile.DESKTOP
                : width >= 620 && height >= 390 ? Profile.COMPACT : Profile.SMALL;
        int margin = profile == Profile.SMALL ? 5 : profile == Profile.COMPACT ? 8 : 12;
        int topHeight = profile == Profile.SMALL ? 28 : 34;
        int footerHeight = profile == Profile.SMALL ? 20 : 25;
        // width/height are already Minecraft GUI logical pixels. Never re-apply guiScale, and never let
        // minimum design dimensions push the frame outside very small logical windows (for example 720p at scale 4).
        UiRect frame = new UiRect(margin, margin, Math.max(1, width - margin * 2), Math.max(1, height - margin * 2));
        UiRect top = new UiRect(frame.x(), frame.y(), frame.width(), topHeight);
        UiRect footer = new UiRect(frame.x(), frame.bottom() - footerHeight, frame.width(), footerHeight);
        int contentY = top.bottom() + 2;
        int contentHeight = Math.max(1, footer.y() - contentY - 2);
        int leftWidth = switch (profile) {
            case DESKTOP_LARGE -> Math.max(430, (int)(frame.width() * 0.47));
            case DESKTOP -> (int)(frame.width() * 0.53);
            case COMPACT -> (int)(frame.width() * 0.60);
            case SMALL -> frame.width();
        };
        leftWidth = Math.min(frame.width(), leftWidth);
        UiRect left = new UiRect(frame.x(), contentY, leftWidth, contentHeight);
        UiRect right = new UiRect(left.right() + 2, contentY, Math.max(0, frame.right() - left.right() - 2), contentHeight);
        int subHeight = profile == Profile.SMALL ? 22 : 27;
        int searchHeight = 20;
        UiRect sub = new UiRect(left.x() + 8, left.y() + 7, left.width() - 16, subHeight);
        UiRect search = new UiRect(left.x() + 8, sub.bottom() + 5, left.width() - 16, searchHeight);
        int detailsHeight = profile == Profile.SMALL ? 0 : Math.min(100, Math.max(64, left.height() / 4));
        UiRect details = new UiRect(left.x() + 8, left.bottom() - detailsHeight - 8, left.width() - 16, detailsHeight);
        UiRect grid = new UiRect(left.x() + 8, search.bottom() + 6, left.width() - 16,
                Math.max(30, (detailsHeight == 0 ? left.bottom() - 7 : details.y() - 6) - (search.bottom() + 6)));
        int statusWidth = right.width() < 310 ? 0 : Math.min(190, right.width() / 3);
        UiRect status = new UiRect(right.right() - statusWidth - 8, right.y() + 8, statusWidth, Math.max(0, right.height() - 16));
        UiRect character = new UiRect(right.x() + 8, right.y() + 8,
                Math.max(0, status.x() - right.x() - 16), Math.max(0, right.height() - 16));
        return new ResponsiveLayout(profile, frame, top, left, right, sub, search, grid, details, character, status, footer);
    }

    public int gridColumns(int slotSize) { return Math.max(1, grid.width() / Math.max(18, slotSize)); }
    public int gridRows(int slotSize) { return Math.max(1, grid.height() / Math.max(18, slotSize)); }
}
