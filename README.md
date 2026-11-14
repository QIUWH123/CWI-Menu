# CWI Menu

A client-only title and loading menu for Create Wasteland Industry, with steel
controls, rotating backgrounds and menu music.

## Features

- Singleplayer, Multiplayer, Options and Quit controls with blue hover outlines
  and pressed feedback.
- Continue the last successfully joined world or server, subject to normal
  Minecraft access checks.
- Shortcuts for Create, resource reload, Mods, Accessibility and Language.
- Background crossfades shared with supported loading screens, while Minecraft
  and NeoForge keep control of loading progress and error handling.
- English and Simplified Chinese labels; textures and text can be overridden
  through normal client resources.

## Usage

1. Install the JAR in the client's `mods/` folder.
2. With KubeJS, copy this repository's
   `kubejs/assets/cwi/textures/gui/menu/` tree into the instance at the same path.
   Keep all PNGs and their `.png.mcmeta` files. Alternatively, supply them in a
   resource pack under `assets/cwi/textures/gui/menu/`.
3. Restart the client. Missing required artwork keeps the native presentation.

Use the menu's resource-reload shortcut or **F3+T** after changing resources.
**Master** and **Music** volume control the track. Setting **Screen Effects** to
zero skips the title intro and selection animation; slideshow and loading fades
still run. Continue stores its local record in `cwi-menu-session.json`.

There is no dedicated configuration file, command or custom KubeJS API. For
example, a resource override in `kubejs/assets/cwi/lang/en_us.json` can rename a
main action:

```json
{"menu.cwi.singleplayer": "Singleplayer"}
```

## Requirements

- Minecraft **1.21.1**, **NeoForge 21.1.264 or newer within 21.1**, and **Java 21**.
- Client installation only; required artwork must be supplied separately from
  the JAR. KubeJS is one resource provider, not a Java dependency.
- Create is optional and enables its shortcut. FancyMenu and its companion
  libraries are not required.

Other mods' replacement title screens are preserved. Known limitations include
keyboard focus differing from pointer selection, unavailable controls lacking
keyboard help, small title controls that do not enlarge with GUI Scale, and
music stopping in some replacement video-settings screens.

## License

Original CWI Menu code is licensed under the GNU General Public License,
**version 3 only**, identified as **GPL-3.0-only**. See [LICENSE](LICENSE).

Supplied artwork and music are excluded from the code license; this repository
grants no additional rights to them. Their redistribution permissions are not
documented here. Gradle's wrapper retains its upstream license and notices.
See [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES).
