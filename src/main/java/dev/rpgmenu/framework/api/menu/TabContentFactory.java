package dev.rpgmenu.framework.api.menu;

/** Creates a logical page model. Rendering remains in client API/implementation code. */
@FunctionalInterface
public interface TabContentFactory {
    Object create(TabContext context);

    static TabContentFactory marker(String semanticType) {
        return context -> semanticType;
    }
}
