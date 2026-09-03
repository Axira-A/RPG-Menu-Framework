package dev.rpgmenu.framework.api.map;

/** Capabilities advertised by a map provider; callers must not infer these from a mod id. */
public enum MapCapability {
    CAN_RENDER_EMBEDDED,
    CAN_OPEN_EXTERNAL,
    CAN_READ_WAYPOINTS,
    CAN_CREATE_WAYPOINTS,
    CAN_EDIT_WAYPOINTS,
    CAN_ZOOM,
    CAN_PAN,
    CAN_CONTEXT_MENU
}
