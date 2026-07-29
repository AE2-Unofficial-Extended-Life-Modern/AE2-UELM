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

<!-- CHANGES:ENTRIES -->
