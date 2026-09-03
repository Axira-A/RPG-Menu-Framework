package dev.rpgmenu.framework.api.skills;

/** GUI-logical bounds supplied to an embedded skill-tree implementation. */
public record SkillViewport(int x, int y, int width, int height) {
    public SkillViewport {
        if (width < 1 || height < 1) throw new IllegalArgumentException("Skill viewport must be positive");
    }
}
