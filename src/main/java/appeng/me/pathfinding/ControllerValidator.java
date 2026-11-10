/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.me.pathfinding;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridVisitor;
import appeng.api.networking.pathing.ControllerState;
import appeng.api.util.AEColor;
import appeng.blockentity.networking.ColorableControllerBlockEntity;
import appeng.blockentity.networking.ControllerBlockEntity;

/**
 * Validates that the controller shape doesn't exceed the max size, and counts the number of adjacent controllers.
 */
public class ControllerValidator implements IGridVisitor {

    /**
     * Maximum size of controller structure on each axis.
     */
    public static final int MAX_SIZE = 7;

    private boolean valid = true;
    private int found = 0;
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;
    private final StructureKey structureKey;
    private final Set<IGridNode> physicalNodes = new HashSet<>();

    private ControllerValidator(BlockPos pos, ControllerBlockEntity startingController) {
        this.structureKey = StructureKey.of(startingController);
        this.minX = pos.getX();
        this.maxX = pos.getX();
        this.minY = pos.getY();
        this.maxY = pos.getY();
        this.minZ = pos.getZ();
        this.maxZ = pos.getZ();
    }

    /**
     * Conditions for valid controllers in grids: 1) The controller structure must not exceed max size (isValid()) 2)
     * All controllers of this grid must be connected to the first controller. This is true if the validator could reach
     * all controllers from the first. 3) There must not be any crosses.
     */
    public static ControllerState calculateState(Collection<ControllerBlockEntity> controllers) {
        if (controllers.isEmpty()) {
            return ControllerState.NO_CONTROLLER;
        }

        var startingController = controllers.iterator().next();
        var startingNode = startingController.getGridNode();
        if (startingNode == null) {
            return ControllerState.CONTROLLER_CONFLICT;
        }

        var validator = new ControllerValidator(startingController.getBlockPos(), startingController);
        if (startingController instanceof ColorableControllerBlockEntity) {
            validator.visitPhysicalStructure(startingNode);
        } else {
            startingNode.beginVisit(validator);
        }

        if (!validator.isValid() || validator.getFound() != controllers.size()) {
            return ControllerState.CONTROLLER_CONFLICT;
        }

        if (hasControllerCross(controllers)) {
            return ControllerState.CONTROLLER_CONFLICT;
        }

        return ControllerState.CONTROLLER_ONLINE;
    }

    @Override
    public boolean visitNode(IGridNode node) {
        if (!this.isValid()) {
            return false;
        }

        if (!(node.getOwner() instanceof ControllerBlockEntity controller)) {
            return false;
        }

        if (!this.structureKey.matches(controller)) {
            this.valid = false;
            return false;
        }

        return this.accept(controller);
    }

    private boolean accept(ControllerBlockEntity controller) {
        var pos = controller.getBlockPos();

        this.minX = Math.min(pos.getX(), this.minX);
        this.maxX = Math.max(pos.getX(), this.maxX);
        this.minY = Math.min(pos.getY(), this.minY);
        this.maxY = Math.max(pos.getY(), this.maxY);
        this.minZ = Math.min(pos.getZ(), this.minZ);
        this.maxZ = Math.max(pos.getZ(), this.maxZ);

        if (this.maxX - this.minX < MAX_SIZE
                && this.maxY - this.minY < MAX_SIZE
                && this.maxZ - this.minZ < MAX_SIZE) {
            this.found++;
            return true;
        }

        this.valid = false;
        return false;
    }

    private void visitPhysicalStructure(IGridNode node) {
        if (!this.valid || !this.physicalNodes.add(node)) {
            return;
        }

        if (!this.visitNode(node)) {
            return;
        }

        var controller = (ControllerBlockEntity) node.getOwner();
        for (var connection : node.getInWorldConnections().values()) {
            var otherNode = connection.getOtherSide(node);
            if (!(otherNode.getOwner() instanceof ControllerBlockEntity otherController)) {
                continue;
            }

            if (!connection.isInWorld() || !arePhysicallyAdjacent(controller, otherController)) {
                this.valid = false;
                return;
            }

            visitPhysicalStructure(otherNode);
        }
    }

    private static boolean arePhysicallyAdjacent(ControllerBlockEntity first, ControllerBlockEntity second) {
        var firstPos = first.getBlockPos();
        var secondPos = second.getBlockPos();
        var distance = Math.abs(firstPos.getX() - secondPos.getX())
                + Math.abs(firstPos.getY() - secondPos.getY())
                + Math.abs(firstPos.getZ() - secondPos.getZ());
        return distance == 1;
    }

    /**
     * Return true if controllers have a cross pattern, i.e. two neighbors on two or three axes.
     */
    private static boolean hasControllerCross(Collection<ControllerBlockEntity> controllers) {
        Set<BlockPos> posSet = new HashSet<>(controllers.size());
        for (var controller : controllers) {
            posSet.add(controller.getBlockPos().immutable());
        }

        for (var pos : posSet) {
            boolean northSouth = posSet.contains(pos.relative(Direction.NORTH))
                    && posSet.contains(pos.relative(Direction.SOUTH));
            boolean eastWest = posSet.contains(pos.relative(Direction.EAST))
                    && posSet.contains(pos.relative(Direction.WEST));
            boolean upDown = posSet.contains(pos.relative(Direction.UP))
                    && posSet.contains(pos.relative(Direction.DOWN));

            if ((northSouth ? 1 : 0) + (eastWest ? 1 : 0) + (upDown ? 1 : 0) > 1) {
                return true;
            }
        }

        return false;
    }

    public boolean isValid() {
        return this.valid;
    }

    public int getFound() {
        return this.found;
    }

    private record StructureKey(boolean colorable, AEColor color) {
        private static StructureKey of(ControllerBlockEntity controller) {
            if (controller instanceof ColorableControllerBlockEntity colorableController) {
                return new StructureKey(true, colorableController.getColor());
            }
            return new StructureKey(false, null);
        }

        private boolean matches(ControllerBlockEntity controller) {
            if (this.colorable) {
                return controller instanceof ColorableControllerBlockEntity colorableController
                        && colorableController.getColor() == this.color;
            }
            return !(controller instanceof ColorableControllerBlockEntity);
        }
    }
}
