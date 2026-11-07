# Changes

This document lists notable changes made in this fork compared to the original mod.

Each pull request is collapsed by default. Click an entry to expand it.

---

<!-- CHANGELOG-PR:1 -->
<details>
<summary><strong>Search predicates character switch</strong> · @ko-lja · merged 2026-06-21 02:36 UTC</summary>

Switch search characters #(for tooltips) and $(for tags) around, as to conform to modern standards

</details>

<!-- CHANGELOG-PR:2 -->
<details>
<summary><strong>Improve math expression parsing and add additional tests</strong> · @ko-lja · merged 2026-06-30 00:01 UTC</summary>

Gracefully copies the MathExpressionParser logic from [GTNHLib](https://github.com/GTNewHorizons/GTNHLib/blob/master/src/main/java/com/gtnewhorizon/gtnhlib/util/parsing/MathExpressionParser.java) and their test cases, many thanks!
Original Pull Request and description on how this works [here](https://github.com/GTNewHorizons/ModularUI/pull/60)!

</details>

<!-- CHANGELOG-PR:3 -->
<details>
<summary><strong>Strip formatting codes from item/fluid names before search</strong> · @trulyno · merged 2026-06-23 17:29 UTC</summary>

AE2's search has the annoyance to not strip formatting codes, meaning that if you search for a term and there is an item that has that term, but it has a formatting code somewhere in its name, it won't match.

This PR fixes it by stripping the formatting code from the item/fluid names before matching it.

</details>

<!-- CHANGELOG-PR:5 -->
<details>
<summary><strong>Allow additions to screen JSON files without overwriting</strong> · @ko-lja · merged 2026-06-30 01:59 UTC</summary>

You now have the ability to create additions onto existing screen json's without having to modify/override the additional one, all you have to do is register it within the FMLClientSetupEvent like so
```java
InitScreens.registerAdditionalStyle(
        "/screens/terminals/terminal.json", // path to existing screen
        new ResourceLocation("modid", "screens/terminal_extra.json")); // path to your additional screen
```
Now do note that, addition json's are not allowed to define everything as you would in a normal screen json.
You can add/modify, `widgets`, `text`, `images` and `tooltips`, example below
```json
{
  "widgets": {
    "extraInfoButton": {
      "left": 190,
      "top": 18,
      "width": 18,
      "height": 18
    },
    "extraConfigButton": {
      "left": 190,
      "top": 40,
      "width": 18,
      "height": 18
    }
  },
  "text": {
    "extraStatusLabel": {
      "position": {
        "left": 190,
        "top": 64
      },
      "text": {
        "translate": "gui.modid.extra_status"
      }
    },
    "extraStaticLabel": {
      "position": {
        "left": 190,
        "top": 76
      },
      "text": {
        "text": "Addon"
      }
    }
  },
  "images": {
    "extraInfoIcon": {
      "texture": "modid:guis/terminal_extra.png",
      "textureWidth": 256,
      "textureHeight": 256,
      "srcRect": [0, 0, 16, 16]
    },
    "extraConfigIcon": {
      "texture": "modid:guis/terminal_extra.png",
      "textureWidth": 256,
      "textureHeight": 256,
      "srcRect": [16, 0, 16, 16]
    }
  },
  "tooltips": {
    "extraInfoTooltip": {
      "left": 190,
      "top": 18,
      "width": 18,
      "height": 18,
      "tooltip": [
        {
          "translate": "gui.modid.extra_info.tooltip"
        },
        {
          "translate": "gui.modid.extra_info.tooltip_hint",
          "color": "#FF00FF"
        }
      ]
    },
    "extraConfigTooltip": {
      "left": 190,
      "top": 40,
      "width": 18,
      "height": 18,
      "tooltip": [
        {
          "translate": "gui.modid.extra_config.tooltip"
        }
      ]
    }
  }
}
```
For more info, check out #4

</details>

<!-- CHANGELOG-PR:7 -->
<details>
<summary><strong>Increase max encoding limit</strong> · @ko-lja · merged 2026-06-30 03:07 UTC</summary>

Increases the max encoding limit to Integer.MAX_VALUE (2^31 - 1)

</details>

<!-- CHANGELOG-PR:8 -->
<details>
<summary><strong>Add EMI/JEI support for transferring recipes into storage buses, interfaces, and export buses.</strong> · @fmbellomy · merged 2026-06-30 19:41 UTC</summary>

There's not much different about this compared to [my base AE2 PR](https://github.com/AppliedEnergistics/Applied-Energistics-2/pull/8915), other than that JEI's `IUniversalRecipeTransferHandler` doesn't exist and thus can't be implemented by the JEI `FilterTransferHandler`. 

I'm assuming this is because this fork compiles against an older version of JEI, but implementing the regular `RecipeTransferHandler` interface and returning null from `getRecipeType` works just the same.

</details>

<!-- CHANGELOG-PR:9 -->
<details>
<summary><strong>Support screen json layering</strong> · @ko-lja · merged 2026-07-01 06:33 UTC</summary>

Before this PR, if a player uses a resource pack that has a screen json that does not include a widget that may have been added recently, trying to open said screen will either fail or crash the game. Now, instead of doing that we will just fallback to the default value from ae2, while still keeping all the other parts from the resource pack intact.

</details>

<!-- CHANGELOG-PR:10 -->
<details>
<summary><strong>Show percentage used in craft confirm screen</strong> · @ko-lja · merged 2026-07-02 13:55 UTC</summary>

If more than available is used the percentage will also reflect this + SI formatting for big amounts

</details>

<!-- CHANGELOG-PR:11 -->
<details>
<summary><strong>Up encoding and request limits</strong> · @ko-lja · merged 2026-07-05 10:18 UTC</summary>

Upped both the pattern encoding terminal and the craft request limits to max long, also now clamping the value of the expression parser between 0 and max long.

</details>

<!-- CHANGELOG-PR:12 -->
<details>
<summary><strong>Add new pattern textures</strong> · @ko-lja · merged 2026-07-08 09:12 UTC</summary>

Add coloring to the different pattern types as to easily differentiate them.

</details>

<!-- CHANGELOG-PR:13 -->
<details>
<summary><strong>Better pattern tooltips</strong> · @ko-lja · merged 2026-07-08 13:49 UTC</summary>

Adds new tooltips to all encoded pattern types showing who encoded the pattern and if (fluid) substitutions are enabled, also adds coloring and better formatting to the inputs/outputs list

</details>

<!-- CHANGELOG-PR:14 -->
<details>
<summary><strong>Add manual pinning with per-world persistence</strong> · @ko-lja · merged 2026-07-22 02:32 UTC</summary>

You can now manually pin any entries from your terminal, they can mix and match with the crafting-pinned ones. Pinning a crafting pinned one will make it a manual one and remain pinned after the crafting job finishes. These are per world/server and persisted in your instance under `config/ae2/pinned/<filename>.dat`.

</details>

<!-- CHANGELOG-PR:15 -->
<details>
<summary><strong>Add EU energy display</strong> · @ko-lja · merged 2026-07-22 03:07 UTC</summary>

if gtceu is installed it'll add EU as an option for energy display

</details>

<!-- CHANGELOG-PR:17 -->
<details>
<summary><strong>Suspend crafting jobs</strong> · @ko-lja · merged 2026-07-22 19:00 UTC</summary>

Adds a suspend/resum button to crafting cpu's which appears when it has an active crafting job, allowing you to suspend all operations from that job until resumed.

</details>

<!-- CHANGELOG-PR:19 -->
<details>
<summary><strong>Render stack icons in pattern tooltips</strong> · @ko-lja · merged 2026-07-26 20:07 UTC</summary>

Since we pass Lists of AEKeys, the displayed stack will match the encoded one, like if you had item durability or a Programmed Circuit(GregTech).
Display the icons in half-size as otherwise the tooltip would have taken too much space on the screen.

</details>

<!-- CHANGELOG-PR:22 -->
<details>
<summary><strong>Message player when a craft finishes</strong> · @ko-lja · merged 2026-07-29 06:37 UTC</summary>

Will send a chat message to the player who requested the craft(might get some tweaks later on with a new "follow craft" mechanic), detailing how much has been requested, what, and how long the craft took to complete.

</details>

<!-- CHANGELOG-PR:23 -->
<details>
<summary><strong>Add super me replenisher</strong> · @ko-lja · merged 2026-08-03 19:39 UTC</summary>

Description from GuideME:
Closely related to the Interface, but with a few key differences, the Super ME Replenisher is a specialized version of the Interface
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

</details>

<!-- CHANGELOG-PR:25 -->
<details>
<summary><strong>Add a filter terminal for remotely configuring machines with an added highlight button</strong> · @ko-lja · merged 2026-09-01 14:06 UTC</summary>

Description from the GuideME entry:
# ME Filter Terminal
The ME Filter Terminal provides remote access to the usable configuration slots of active import buses, export buses,
storage buses, formation planes, storage level emitters, and interface hosts on its network. Machines are grouped by their icon and name, then sorted by
location.

## UI

The first search field matches target names, and the second matches configured item or fluid names. Blank fields do not
filter, and when both contain text, a target must match both. Middle-clicking an interface slot opens an
amount editor, and its tooltip shows the amount stocked by that interface.
The highlight button highlights the matching block or cable part in the world for 15 seconds.

</details>

<!-- CHANGELOG-PR:27 -->
<details>
<summary><strong>Force submit crafts with missing ingredients</strong> · @ko-lja · merged 2026-09-02 16:39 UTC</summary>

## Description
Pressing Shift and clicking the Start button will now allow players to submit incomplete crafting jobs with materials being supplied while the craft is ongoing, this was made with passive resource generation setups in mind, but also does solve some issues created by recursive recipes, or loops.
## Screenshots
<img width="775" height="469" alt="image" src="https://github.com/user-attachments/assets/d48fbfd6-70c7-4cd6-9ac6-e2af4b54f38b" />
<img width="894" height="430" alt="image" src="https://github.com/user-attachments/assets/fb7fd721-4352-490d-b2ea-c06df5a2185d" />

</details>

<!-- CHANGELOG-PR:29 -->
<details>
<summary><strong>Switch craft amount adjustment to 1, 16, 32, 64 while holding shift or ctrl</strong> · @ko-lja · merged 2026-09-03 16:43 UTC</summary>

title, seemed like a welcome alternative since these numbers just feel more natural

</details>

<!-- CHANGELOG-PR:31 -->
<details>
<summary><strong>Extendable craft confirm and status screens</strong> · @ko-lja · merged 2026-09-05 05:57 UTC</summary>

The craft confirm and status screens will now adapt to the terminal size chosen.

</details>

<!-- CHANGELOG-PR:33 -->
<details>
<summary><strong>Add colorable controllers</strong> · @ko-lja · merged 2026-09-13 16:19 UTC</summary>

Can be placed next to other controllers of a different color without connecting or causing a conflict.

## Videos
Default look

https://github.com/user-attachments/assets/654dad3e-60fb-42d6-b9ed-35fcb253f883

Dark mode compatibility

https://github.com/user-attachments/assets/8d8575da-8e12-4592-be1e-69453223e81d

</details>

<!-- CHANGELOG-PR:35 -->
<details>
<summary><strong>Add compatibility for pick-block keys other than middle-click</strong> · @ko-lja · merged 2026-09-17 03:34 UTC</summary>

Previously if you bound your pick block key to anything other than middle-click, said keybind would not work for requesting crafts or setting amounts.

</details>

<!-- CHANGELOG-PR:39 -->
<details>
<summary><strong>Rework filter terminal to be more extensible</strong> · @ko-lja · merged 2026-10-07 18:49 UTC</summary>

This PR aims to make the filter terminal more extensible by adding an API for it allowing any generic parts/machines to be recognized by it.

</details>

<!-- CHANGELOG-PR:46 -->
<details>
<summary><strong>Export crafting tree to image</strong> · @ko-lja · merged 2026-10-27 13:10 UTC</summary>

Will export the current crafting tree as an image, such as the one below, this should also respect any applied resource packs
<img width="1702" height="1616" alt="2026-10-27_13 27 57" src="https://github.com/user-attachments/assets/1140d1e1-f2b1-4e94-9e58-78e9b9e51d28" />

</details>

<!-- CHANGELOG-PR:47 -->
<details>
<summary><strong>Add favoriting to the pattern access and filter terminals</strong> · @ko-lja · merged 2026-10-27 13:19 UTC</summary>

Add a small button to groups (pattern access terminal) and individual machines (filter terminal), which when favorited will show at the top of the terminal, if multiple are favorited it'll follow the normal sorting order.

</details>

<!-- CHANGELOG-PR:49 -->
<details>
<summary><strong>Add sticky card</strong> · @ko-lja · merged 2026-11-06 20:10 UTC</summary>

Port of the Sticky card from GTNH AE2 with some improvements

### Description from the GuideME entry, but it describes it pretty well:
Sticky cards can be applied to storage cells in a cell workbench and to
storage buses. Anything that matches the partition or filter of a sticky storage
can only be stored in sticky storages that accept it, no matter their priority. If those
are full, the stacks will not be stored anywhere else on the network. Stacks returning to an
autocrafting job are not affected. A sticky card does nothing on a storage without a
partition or filter.

### Performance Impact
- Networks with no sticky card only do an empty list check per inseret
- The cell’s sticky upgrade is read once when the cell is loaded. GTNH re reads the upgrade inventory from the ItemStack on every insert.
- Storage buses and drives keep the sticky state in a cached field.
- Inserts now stop looping over priority groups once everything has been placed. This speeds up normal inserts too.

</details>

<!-- CHANGELOG-PR:50 -->
<details>
<summary><strong>Port some performance work from upstream</strong> · @ko-lja · merged 2026-11-07 23:12 UTC</summary>

Ports a batch of upstream performance fixes to 1.20.1, one commit per upstream change, plus the JMH setup used to measure the storage and crafting ones.

## Benchmarks: baseline (commit 1) vs HEAD

| Benchmark | Baseline (ops/s) | HEAD (ops/s) | Change |
|---|---|---|---|
| `CraftingProvidersBenchmark.getCraftingFor_manyProviders` | 71,309 ± 2,400 | 99,684,000 ± 3,374,000 | 1398x |
| `InventoryBenchmark.getAvailableStacks_emptyCell` | 17,953,000 ± 524,300 | 34,088,000 ± 803,600 | 1.90x |
| `InventoryBenchmark.getAvailableStacks_fullCell` | 158,980 ± 2,929 | 613,200 ± 29,220 | 3.86x |
| `InventoryBenchmark.getAvailableStacks_inventoryHandler` | 1,950 ± 61 | 4,107 ± 89 | 2.11x |
| `InventoryBenchmark.getAvailableStacks_networkStorage` | 1,892 ± 80 | 4,749 ± 99 | 2.51x |
| `InventoryBenchmark.getAvailableStacks_service` | 122.2 ± 7.4 | 220.7 ± 34.3 | 1.81x |

&lt;details&gt;
&lt;summary&gt;Per-commit trend (5 iterations)&lt;/summary&gt;

| # | Commit | emptyCell | fullCell | inventoryHandler | networkStorage | service | craftingFor |
|---|---|---|---|---|---|---|---|
| 1 | 0b012b4b4 | 1.00x | 1.00x | 1.00x | 1.00x | 1.00x | 1.00x |
| 2 | 0ca3f2463 (#8965) | 0.91x | 2.09x | 1.39x | 0.89x | 1.10x | 1.10x |
| 3 | 41c56cf11 (#8966) | 1.33x | 3.81x | 1.76x | 1.63x | 0.86x | 0.95x |
| 4 | 5406c6dba (#8967) | 1.55x | 3.78x | 1.73x | 1.75x | 1.09x | 0.91x |
| 5 | b9f1fc5b2 (#8891) | 1.25x | 3.40x | 2.17x | 2.42x | 1.81x | 1.05x |
| 6 | c932c618c (#9000) | 1.46x | 3.24x | 2.30x | 2.34x | 1.78x | 1376x |
| 7 | fdf31ca12 (#8989) | 1.41x | 3.19x | 2.59x | 2.28x | 1.82x | 1359x |
| 8 | e99d00a1b (#9004) | 2.03x | 4.45x | 2.30x | 2.37x | 1.77x | 1313x |
| 9 | 2c9a3d652 (#8992) | 1.71x | 4.63x | 2.47x | 2.17x | 1.82x | 1367x |
| 10 | 95d30d97b (#9042) | 1.91x | 4.51x | 2.53x | 2.39x | 1.67x | 1264x |
| 11 | f80efbf6e (#9011) | 1.66x | 4.17x | 2.15x | 2.11x | 1.62x | 1279x |

&lt;/details&gt;

| Benchmark | 5406c6dba (#8967) | b9f1fc5b2 (#8891) | Change |
|---|---|---|---|
| emptyCell | 26,830,000 ± 1,455,000 | 34,655,000 ± 1,701,000 | +29% |
| fullCell | 500,610 ± 33,930 | 515,720 ± 19,980 | +3% |
| inventoryHandler | 3,126 ± 276 | 4,591 ± 423 | +47% |
| networkStorage | 3,485 ± 121 | 4,323 ± 267 | +24% |
| service | 128.9 ± 9.7 | 220.9 ± 12.3 | +71% |
| craftingFor | 68,583 ± 1,922 | 77,437 ± 1,179 | +13% |

# Graphs

<img width="1475" height="914" alt="image" src="https://github.com/user-attachments/assets/94b14983-0d63-421a-91fa-dc8c7233a28a" />
<img width="1798" height="1228" alt="image" src="https://github.com/user-attachments/assets/66f1e8f6-619d-4414-a283-4d1605e9aa79" />

&lt;details&gt;
&lt;summary&gt;Crafting specific improvements&lt;/summary&gt;
<img width="1745" height="1160" alt="image" src="https://github.com/user-attachments/assets/da836947-0ff1-4b37-947b-c648176b5730" />
<img width="1839" height="1217" alt="image" src="https://github.com/user-attachments/assets/f48e101b-3d22-4c31-b7e6-5202f2d98249" />

&lt;/details&gt;

</details>

<!-- CHANGES:ENTRIES -->
