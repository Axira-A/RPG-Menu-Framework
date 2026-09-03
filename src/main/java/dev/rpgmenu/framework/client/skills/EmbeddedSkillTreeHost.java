package dev.rpgmenu.framework.client.skills;

import dev.rpgmenu.framework.RpgMenuFramework;
import dev.rpgmenu.framework.api.RpgMenuApi;
import dev.rpgmenu.framework.api.input.InputAction;
import dev.rpgmenu.framework.api.skills.SkillProvider;
import dev.rpgmenu.framework.api.skills.SkillViewport;
import dev.rpgmenu.framework.client.compat.epicskills.EpicSkillsEmbeddedView;
import dev.rpgmenu.framework.client.layout.UiRect;
import net.minecraft.client.gui.GuiGraphics;

/** Owns the currently active provider inside the single semantic Skills tab. */
public final class EmbeddedSkillTreeHost {
    public enum RenderState { UNAVAILABLE, INITIALIZING, READY, FAILED }

    private EpicSkillsEmbeddedView epicSkills;
    private UiRect viewport = new UiRect(0, 0, 1, 1);
    private boolean failedLogged;
    private boolean mouseCaptured;
    private int capturedButton = -1;
    private RenderState renderState = RenderState.UNAVAILABLE;

    public RenderState render(GuiGraphics graphics, UiRect target, int mouseX, int mouseY, float partialTick) {
        viewport = target;
        if (!epicSkillsAvailable()) return renderState = RenderState.UNAVAILABLE;
        if (epicSkills == null) epicSkills = new EpicSkillsEmbeddedView();
        SkillViewport skillViewport = skillViewport();
        if (!epicSkills.ensure(skillViewport)) return renderState = RenderState.FAILED;
        graphics.enableScissor(target.x(), target.y(), target.right(), target.bottom());
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(target.x(), target.y(), 0);
            if (!epicSkills.render(graphics, mouseX - target.x(), mouseY - target.y(), partialTick)) {
                return renderState = RenderState.FAILED;
            }
        } catch (LinkageError | RuntimeException exception) {
            logFailure(exception);
            epicSkills.markFailed(exception);
            return renderState = RenderState.FAILED;
        } finally {
            graphics.pose().popPose();
            graphics.disableScissor();
        }
        return renderState = epicSkills.isReady() ? RenderState.READY : RenderState.INITIALIZING;
    }

    public void tick() { if (epicSkills != null) epicSkills.tick(); }
    public void mouseMoved(double x, double y) {
        if (epicSkills != null && viewport.contains(x, y)) epicSkills.mouseMoved(x - viewport.x(), y - viewport.y());
    }
    public boolean mouseClicked(double x, double y, int button) {
        if (epicSkills == null || !epicSkills.isReady() || !viewport.contains(x, y)) return false;
        mouseCaptured = true;
        capturedButton = button;
        epicSkills.mouseClicked(x - viewport.x(), y - viewport.y(), button);
        // Blank canvas presses must still be consumed so Minecraft continues sending drag/release events.
        return true;
    }
    public boolean mouseReleased(double x, double y, int button) {
        if (epicSkills == null || !epicSkills.isReady()
                || !mouseCaptured && !viewport.contains(x, y)) return false;
        epicSkills.mouseReleased(x - viewport.x(), y - viewport.y(), button);
        if (button == capturedButton) {
            mouseCaptured = false;
            capturedButton = -1;
        }
        return true;
    }
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (epicSkills == null || !epicSkills.isReady()
                || !mouseCaptured && !viewport.contains(x, y)) return false;
        epicSkills.mouseDragged(x - viewport.x(), y - viewport.y(), button, dx, dy);
        return true;
    }
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (epicSkills == null || !epicSkills.isReady() || !viewport.contains(x, y)) return false;
        epicSkills.mouseScrolled(x - viewport.x(), y - viewport.y(), sx, sy);
        return true;
    }
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return epicSkills != null && epicSkills.keyPressed(keyCode, scanCode, modifiers);
    }
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return epicSkills != null && epicSkills.keyReleased(keyCode, scanCode, modifiers);
    }
    public boolean charTyped(char codePoint, int modifiers) {
        return epicSkills != null && epicSkills.charTyped(codePoint, modifiers);
    }
    public boolean controllerAction(InputAction action) {
        return epicSkills != null && epicSkills.controllerAction(action);
    }
    public boolean hasTextInputFocus() { return epicSkills != null && epicSkills.hasTextInputFocus(); }
    public boolean handleBack() { return epicSkills != null && epicSkills.handleBack(); }
    public RenderState renderState() { return renderState; }
    public void close() {
        if (epicSkills != null) epicSkills.close();
        epicSkills = null;
        mouseCaptured = false;
        capturedButton = -1;
        renderState = RenderState.UNAVAILABLE;
    }

    private SkillViewport skillViewport() {
        return new SkillViewport(viewport.x(), viewport.y(), Math.max(1, viewport.width()), Math.max(1, viewport.height()));
    }

    private boolean epicSkillsAvailable() {
        for (SkillProvider provider : RpgMenuApi.get().skillProviders().values()) {
            try {
                if (provider.id().equals(RpgMenuFramework.id("epicskills")) && provider.isAvailable()) return true;
            } catch (LinkageError | RuntimeException exception) {
                logFailure(exception);
            }
        }
        return false;
    }

    private void logFailure(Throwable exception) {
        if (!failedLogged) {
            failedLogged = true;
            RpgMenuFramework.LOGGER.warn("[RPGMF] Embedded Epic Skills host failed", exception);
        }
    }
}
