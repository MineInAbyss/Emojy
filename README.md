<div style="text-align: center">

# Emojy
[![Java CI with Gradle](https://github.com/MineInAbyss/Emojy/actions/workflows/gradle-ci.yml/badge.svg)](https://github.com/MineInAbyss/Chatty/actions/workflows/gradle-ci.yml)
[![Maven](https://img.shields.io/maven-metadata/v?metadataUrl=https://repo.mineinabyss.com/releases/com/mineinabyss/emojy/maven-metadata.xml)](https://repo.mineinabyss.com/#/releases/com/mineinabyss/chatty)
[![Wiki](https://img.shields.io/badge/-Project%20Wiki-blueviolet?logo=Wikipedia&labelColor=gray)](https://github.com/MineInAbyss/Emojy/wiki)
[![Contribute](https://shields.io/badge/Contribute-e57be5?logo=github%20sponsors&style=flat&logoColor=white)](https://github.com/MineInAbyss/MineInAbyss/wiki/Setup-and-Contribution-Guide)
</div>

## Overview
Emojy is a plugin that lets you make custom emotes and gifs to use just about anywhere.\
It works by replacing :id: with the assigned unicode.\
Compared to a lot of other plugins this does not purely rely on the default font.\
This will prevent players from simply copying unicodes and spamming them in chat.

__Small feature-list:__
- Replace _:id:_ in chat, books, signs, inventory titles, tablist footer/header, titles, subtitles and actionbars
  - **Note:** Titles, tablist, actionbars and inventories require ProtocolLib
- Support for "GIFs" which are just a series of images that rely on the obfuscation tag to make it look like a gif
  - This is done by having every gif be its own font, but it has the downside of being in random order and way too fast
- Automatically split gifs into images depending on specified framecount
- Automatically generate a resourcepack with all the emotes and gifs

Whilst this is primarily for emojis and gifs, it is also a useful tool for everything else related to unicodes.\
For example it lets you easily make custom menus which can be put in titles simply by doing :id:


## Requirements
- [Idofront Platform](https://github.com/MineInAbyss/Idofront)
- [ProtocolLib](https://ci.dmulloy2.net/job/ProtocolLib/lastBuild/) (Optional, but recommended)

## Recommended Plugins
- [Chatty](https://github.com/MineInAbyss/Chatty) - Highly customizable chat plugin with optional support for 1.19 Chat Signing

## Setup
1. Download the latest version of Emojy
2. Put it in `server/plugins` and start your server
3. Add your emotes and gifs to config.yml, the bare minimum it needs is the `id: emoteid`
4. Add all your emote textures inside `/plugins/Emojy/textures` and gifs inside `/plugins/Emojy/gifs`
5. Restart your server or run `/emojy reload` to reload the config
6. Copy the generated resourcepack from `/plugins/Emojy/assets` to your resourcepack folder
