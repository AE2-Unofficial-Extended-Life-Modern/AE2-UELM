package appeng.api.storage;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import appeng.api.stacks.AEKey;

/**
 * Describes which keys a sticky {@link MEStorage} claims, see {@link MEStorage#getStickyClaim()}.
 *
 * @param keys Every key the storage may claim, so the network can look claims up by key. May contain keys that are not
 *             accepted, since {@link MEStorage#acceptsSticky(AEKey)} still decides each claim, but must contain every
 *             key that is. Null if the keys can't be listed, for example for fuzzy or inverted filters.
 */
public record StickyClaim(@Nullable Set<AEKey> keys) {
    public static final StickyClaim UNINDEXED = new StickyClaim(null);
}
