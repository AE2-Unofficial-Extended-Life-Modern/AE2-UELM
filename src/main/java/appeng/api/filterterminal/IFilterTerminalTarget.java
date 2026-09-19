package appeng.api.filterterminal;

import appeng.api.networking.IGridNode;

/**
 * A loaded, AE2 connected machine that can be shown in a filter terminal.
 * <p>
 * Targets whose configuration view denies all edits are still valid as read only targets.
 */
public interface IFilterTerminalTarget {

    /**
     * Returns the stable object used to recognize this target between discovery passes while it remains loaded.
     * Consumers compare this value by reference, so implementations must not return a new object per call.
     */
    Object getIdentity();

    /**
     * @return the grid node connecting this target to the network
     */
    IGridNode getGridNode();

    /**
     * @return display, grouping, and world-location information for this target
     */
    FilterTerminalTargetMetadata getMetadata();

    /**
     * @return the target's filter configuration and stock view
     */
    IFilterTerminalConfigView getConfigView();
}
