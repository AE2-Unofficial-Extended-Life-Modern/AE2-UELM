[![Build master](https://img.shields.io/github/actions/workflow/status/AE2-Unofficial-Extended-Life-Modern/AE2-UELM/build.yml?style=flat-square&branch=master)](https://github.com/AE2-Unofficial-Extended-Life-Modern/AE2-UELM/actions?query=workflow%3A%22Build+master%22)
[![Latest Release](https://img.shields.io/github/v/release/AE2-Unofficial-Extended-Life-Modern/AE2-UELM?style=flat-square&label=Release)](https://github.com/AE2-Unofficial-Extended-Life-Modern/AE2-UELM/releases)
[![Latest PreRelease](https://img.shields.io/github/v/release/AE2-Unofficial-Extended-Life-Modern/AE2-UELM?include_prereleases&style=flat-square&label=Pre)](https://github.com/AE2-Unofficial-Extended-Life-Modern/AE2-UELM/releases)

# Applied Energistics 2

## Table of Contents

* [About](#about)
* [Contacts](#contacts)
* [Downloads](#downloads)
* [Installation](#installation)
* [Issues](#issues)
* [API](#applied-energistics-2-api)
* [Contributing](#contributing)
* [Localization](#applied-energistics-2-localization)
* [License](#license)
* [Credits](#credits)

## About

A Fork of the Mod about Matter, Energy and using them to conquer the world..

There are a few reasons behind the creation of this fork, here are a few
- ### Mixin mess
  Yes you may argue that some of the features the fork adds/will add are doable with mixins, but at some point it just gets too much for a single addon to manage everything properly.
- ### Addon disparity
  A lot of the currently existing addons either have a some form of feature overlap, forcing you to install a lot of them to probably not even get all the features you desire.
- ### Version abandonment
  The upstream Forge 1.20.1 version has not seen meaningful changes in a long time as development focus has shifted to more modern versions, even tho forge 1.20.1 is currently still the version with the most modding activity out there.

For a comprehensive list of all changes, check out [CHANGES.md](https://github.com/AE2-Unofficial-Extended-Life-Modern/AE2-UELM/blob/forge/1.20.1/CHANGES.md)

## Contacts

* [Discord](https://discord.gg/CVUK3kCn55)
* [GitHub](https://github.com/AE2-Unofficial-Extended-Life-Modern)

## Downloads

Downloads can be found on CurseForge or on GitHub very soon!

## Installation

You install this mod by putting it into the `minecraft/mods/` folder. It has no additional hard dependencies except [GuideME](https://www.curseforge.com/minecraft/mc-mods/guideme).

## Issues

Applied Energistics 2 crashing, have a suggestion, found a bug?  Create an issue now!

1. Make sure your issue has not already been answered or fixed and you are using the latest version. Also think about whether your issue is a valid one before submitting it.
2. Go to [the issues page](https://github.com/AE2-Unofficial-Extended-Life-Modern/AE2-UELM/issues) and click [new issue](https://github.com/AE2-Unofficial-Extended-Life-Modern/AE2-UELM/issues/new)
3. If applicable, use one of the provided templates. It will also contain further details about required or useful information to add.
4. Click `Submit New Issue`, and wait for feedback!

Providing as many details as possible does help us to find and resolve the issue faster and also you getting a fixed version as fast as possible.

Please note that we might close any issue not matching these requirements. 

## Applied Energistics 2 API

The API for Applied Energistics 2. It is open source to discuss changes, improve documentation, and provide better add-on support in general.

### Maven

The fork will be available on the Maven Repository below. You can use the following snippet as example on how to add it to your gradle build file.
```groovy
repositories {
    maven {
        name "expandiumReleases"
        url "https://repo.expandium.net/releases"
    }
}
```

For working with the fork, add the following gradle dependency
```groovy
dependencies {
    // For ModDevGradle users
    modImplementation "appeng:appliedenergistics2-forge:VERSION"
    // For ForgeGradle users
    implementation fg.deobf("appeng:appliedenergistics2-forge:VERSION")
}
```

Replace `VERSION` with the desired one, preferably try to always use the latest one. 
It is highly recommended following its specification and further considering an upper bound for the dependency version.
A change of the `MAJOR` version will be an API break and can lead to various crashes. Better to inform a player about the addon not supporting the new version until it could be tested or updated.

An example string would be `appeng:appliedenergistics2-forge:15.4.10:api` for the API only or `appeng:appliedenergistics2-forge:15.4.10` for the whole mod.

## Contributing

Contributions are always welcome and appreciated!

Here are a few things to keep in mind that will help get your PR approved.

* A PR should be focused on content. Any PRs where the changes are only syntax will be rejected.
* Use the file you are editing as a style guide.
* Consider your feature.
  - Make sure your feature isn't already in the works, or hasn't been rejected previously.
  - If your feature can be done by any popular mod, discuss with us first.

Before contributing major changes, you should probably discuss them with us first, to waste noone's time.
You can either open an issue or send a message in the development channel on the Discord.

Still want to contribute? Great!

### Getting started:

1. [Fork the repository](https://github.com/AE2-Unofficial-Extended-Life-Modern/fork)
2. Open a new [pull request](https://github.com/AE2-Unofficial-Extended-Life-Modern/pulls) targeting the `forge/1.20.1` branch
  * Make sure you target the right repository, ie. this one and not upstream
  * Build check failing? You might have forgotten to run the `spotlessApply` or `runData` task
3. Changes requested by maintainers? Do your best to solve them
4. Pull request merged? Congrats and thank you for your contribution!

If you are only doing single file pull requests, GitHub supports using a quick way without the need of cloning your fork. Also read up about [synching](https://help.github.com/articles/syncing-a-fork) if you plan to contribute on regular basis.

## Applied Energistics 2 Localization

### English Text

`en_US` is included in this repository, fixes to typos are welcome.

### Encoding

Files must be encoded as UTF-8.

### New or updated Translations

Please keep in mind that we use [String format](https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html) to pass additional data to the text for displaying.
Therefore you should preserve parts like `%s` or `%1$d%%`, which allows us to replace them with the correct values while you still have the option to change their order for match the rules of grammar.
This might not be possible for some languages. Should this be the case, please contact us.

## License

* Applied Energistics 2 API
  - (c) 2013 - 2026 AlgorithmX2 et al
  - [![License](https://img.shields.io/badge/License-MIT-red.svg?style=flat-square)](http://opensource.org/licenses/MIT)
* Applied Energistics 2
  - (c) 2013 - 2026 AlgorithmX2 et al
  - [![License](https://img.shields.io/badge/License-LGPLv3-blue.svg?style=flat-square)](https://raw.githubusercontent.com/AppliedEnergistics/Applied-Energistics-2/rv2/LICENSE)
* Textures and Models
  - (c) 2026, [Ridanisaurus Rid](https://github.com/Ridanisaurus/), (c) 2013 - 2026 AlgorithmX2 et al
  - [![License](https://img.shields.io/badge/License-CC%20BY--NC--SA%203.0-yellow.svg?style=flat-square)](https://creativecommons.org/licenses/by-nc-sa/3.0/)
* Text and Translations
  - [![License](https://img.shields.io/badge/License-No%20Restriction-green.svg?style=flat-square)](https://creativecommons.org/publicdomain/zero/1.0/)
* Additional Sound Licenses
  - Guidebook Click Sound
    - [EminYILDIRIM](https://freesound.org/people/EminYILDIRIM/sounds/536108/) 
    - [![License](https://img.shields.io/badge/License-CC%20BY%204.0-yellow.svg?style=flat-square)](https://creativecommons.org/licenses/by/4.0/)

## Credits

Thanks to
 
* Notch et al for Minecraft
* Lex et al for MinecraftForge
* AlgorithmX2 for AppliedEnergistics2
* [Ridanisaurus Rid](https://github.com/Ridanisaurus/) for the new 2020 textures
* all [contributors](https://github.com/AppliedEnergistics/Applied-Energistics-2/graphs/contributors)
* Upstream [AE2](https://github.com/AppliedEnergistics/Applied-Energistics-2/) for having a permissive license allowing us to make this fork a reality!
