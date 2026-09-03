package dev.rpgmenu.framework.common.compat.ftbquests;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.quests.QuestChapter;
import dev.rpgmenu.framework.api.quests.QuestEntry;
import dev.rpgmenu.framework.api.quests.QuestProgress;
import dev.rpgmenu.framework.api.quests.QuestProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Reflection keeps the framework a soft dependency while targeting FTB Quests 2101.x's client quest cache.
 * FTB's cache is already synchronized by FTB Quests, so this provider never manufactures progress client-side.
 */
public final class FtbQuestProvider implements QuestProvider {
    public static final ResourceLocation ID = RpgMenuFramework.id("ftbquests");
    private static final ResourceLocation EMPTY_ICON = ResourceLocation.withDefaultNamespace("textures/item/book.png");
    private boolean unavailableLogged;

    @Override public ResourceLocation id() { return ID; }
    @Override public int priority() { return 500; }

    @Override
    public List<QuestChapter> chapters(Player player) {
        try {
            Class<?> cacheClass = Class.forName("dev.ftb.mods.ftbquests.client.ClientQuestFile");
            if (!(boolean) cacheClass.getMethod("exists").invoke(null)) return List.of();
            Object questFile = cacheClass.getMethod("getInstance").invoke(null);
            List<?> chapters = (List<?>) questFile.getClass().getMethod("getAllChapters").invoke(questFile);
            List<QuestChapter> result = new ArrayList<>(chapters.size());
            for (Object chapter : chapters) result.add(chapter(chapter));
            return List.copyOf(result);
        } catch (ReflectiveOperationException | LinkageError exception) {
            logUnavailable(exception);
            return List.of();
        }
    }

    private QuestChapter chapter(Object chapter) throws ReflectiveOperationException {
        long id = ((Number) chapter.getClass().getMethod("getID").invoke(chapter)).longValue();
        String title = componentText(chapter.getClass().getMethod("getAltTitle").invoke(chapter));
        List<?> quests = (List<?>) chapter.getClass().getMethod("getQuests").invoke(chapter);
        List<QuestEntry> entries = new ArrayList<>(quests.size());
        for (Object quest : quests) entries.add(quest(quest));
        return new QuestChapter(id("chapter_" + id), title, entries);
    }

    private QuestEntry quest(Object quest) throws ReflectiveOperationException {
        long id = ((Number) quest.getClass().getMethod("getID").invoke(quest)).longValue();
        String title = componentText(quest.getClass().getMethod("getAltTitle").invoke(quest));
        String description = componentText(quest.getClass().getMethod("getSubtitle").invoke(quest));
        return new QuestEntry(id("quest_" + id), title, description, EMPTY_ICON,
                new QuestProgress(0, 0, false, false), List.of(), List.of());
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("ftbquests", path); }

    private static String componentText(Object value) {
        return value instanceof Component component ? component.getString() : String.valueOf(value);
    }

    private void logUnavailable(Throwable exception) {
        if (!unavailableLogged) {
            unavailableLogged = true;
            RpgMenuFramework.LOGGER.warn("[RPGMF] FTB Quests 2101.x data bridge is unavailable", exception);
        }
    }
}
