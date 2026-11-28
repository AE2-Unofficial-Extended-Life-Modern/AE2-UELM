package appeng.me.storage;

import appeng.api.storage.MEStorage;

/**
 * A storage whose contents can change without going through the network that mounted it, but which counts those
 * changes, so the network can notice them without listing the storage.
 */
public interface ChangeCountedStorage extends MEStorage {
    long NOT_COUNTED = -1;

    /**
     * @return A number that is different after every change of the contents, or {@link #NOT_COUNTED}.
     */
    long getChangeCount();
}
