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

<!-- CHANGES:ENTRIES -->
