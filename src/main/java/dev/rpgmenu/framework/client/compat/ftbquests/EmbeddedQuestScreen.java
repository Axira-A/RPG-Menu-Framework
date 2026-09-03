package dev.rpgmenu.framework.client.compat.ftbquests;

import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestLink;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import dev.ftb.mods.ftbquests.quest.task.Task;

/**
 * Thin 2101.x adapter that removes only QuestScreen's assumptions that it owns Minecraft's current Screen.
 * All chapter, graph, detail, task and reward widgets remain the original FTB implementations.
 */
public final class EmbeddedQuestScreen extends QuestScreen {
    public EmbeddedQuestScreen(ClientQuestFile file, PersistedData persistedData) {
        super(file, persistedData);
    }

    /** Embedded sizing is supplied by the host; QuestScreen#setFullscreen() must not run. */
    @Override
    public boolean onInit() {
        return true;
    }

    /** Keep FTB's real panel ticks (notably ChapterPanel's slide animation) without owning a ScreenWrapper. */
    @Override
    public void tick() {
        super.tick();
    }

    /**
     * Mirrors QuestScreen's public navigation dispatch without its final `openGui()` call.  This method is the
     * route used by chapter/quest widgets, links and task links in normal player mode.
     */
    @Override
    public void open(QuestObject object, boolean scrollToObject) {
        if (object instanceof Chapter chapter) {
            selectChapter(chapter);
        } else if (object instanceof Quest quest) {
            selectChapter(quest.getChapter());
            viewQuest(quest);
            if (scrollToObject) scrollTo(quest);
        } else if (object instanceof QuestLink link) {
            selectChapter(link.getChapter());
            if (scrollToObject) scrollTo(link);
            link.getQuest().ifPresent(this::viewQuest);
        } else if (object instanceof Task task) {
            Quest quest = task.getQuest();
            selectChapter(quest.getChapter());
            viewQuest(quest);
        }
    }
}
