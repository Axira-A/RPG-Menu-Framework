package dev.rpgmenu.framework.api.inventory;

/** Authoritative transaction outcome returned by a source/coordinator. */
public record TransactionResult(Status status, long moved, String messageKey) {
    public enum Status { SUCCESS, PARTIAL, REJECTED, STALE, UNAVAILABLE }

    public static TransactionResult rejected(String messageKey) {
        return new TransactionResult(Status.REJECTED, 0, messageKey);
    }
}
