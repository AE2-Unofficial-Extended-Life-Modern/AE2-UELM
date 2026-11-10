---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: Super ME Replenisher
  icon: super_me_replenisher
  position: 210
categories:
- devices
item_ids:
- ae2:super_me_replenisher
---

# The Super ME Replenisher

### Three modes: Idle, Inserting, and Extracting

<Row gap="20">
<BlockImage id="super_me_replenisher" scale="8" />
<BlockImage id="super_me_replenisher" scale="8" p:activity="inserting" />
<BlockImage id="super_me_replenisher" scale="8" p:activity="extracting" />
</Row>

Closely related to the [Interface](../items-blocks-machines/interface.md), but with a few key differences, the Super ME Replenisher is a specialized version of the Interface
that allows you to filter and store up to 27 slots with items, fluids, etc. With technically no limit on how much you can configure. How much you actually can store is
determined by the storage cells you put in it, adding the maximum bytes of that cell to the total available bytes. No matter which type of cell, all that matters is how many bytes it can hold.

## How The Replenisher Works Internally
Unlike an interface, the replenisher will under no circumstances expose its connected network if you place a storage bus on it, only what you configure it to, you will still be able to input into it normally, 
but beware that pending inputs also take up cell space.

The replenisher will periodically check, based on the tick rate you set if any entry has less stocked than the set threshold amount.

Do note that it will not try to request anything on its own; all it does is try to refill itself.

Which makes it an improvement over normal interfaces for stuff like storage subnets, as it does reduce a part of the lag.

## Some notes
When you break it, the replenisher will always try to return its contents to the network it was connected to.
If either some contents could not be returned or if it was not connected to a network before being broken, it will try to fill the stored cells before dropping them.