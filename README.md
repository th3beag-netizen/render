# Beegi — Meteor 26.3 source-based development branch

This branch is a **new base** for Beegi. It is not the earlier handwritten Beegi codebase. The GitHub Actions workflow checks out the official [Meteor Client](https://github.com/MeteorDevelopment/meteor-client) source, pins it to commit `ec8cdb088af88582bade7cd3b0dfbe71ccd2285c` (Minecraft 26.3), applies [patch_meteor.py](patch_meteor.py), and builds it on Java 25.

## Why this direction

Meteor already has a tested ClickGUI, working module toggles and settings, an extensive module system, configurable themes, ESP, Xray, movement and other utility modules. Instead of duplicating that functionality poorly, Beegi will add Peegi-like customization and only features not already available.

## This first build

- Based on Meteor Client for Minecraft 26.3.
- Display name **Beegi (Meteor-based)** and cool-blue default theme accents.
- Keeps Meteor's mod id `meteor-client` internally to avoid breaking its mixins and resource paths.
- Preserves Meteor authorship and GPL-3.0 notices and retains the complete upstream source in the downloadable source archive.
- **Do not install this together with the ordinary Meteor JAR.** Both use the same internal mod ID.
- This is not yet a Peegi-looking client. The next changes will adapt layout, colors, and module-specific settings on this more reliable base.

## Development

GitHub Actions produces two distinct artifacts on success: an installable Meteor-derived Beegi JAR and its corresponding complete patched source archive (GPL-3.0).

The build can be reproduced locally by cloning the upstream source at the pinned commit, copying the patch script into its parent folder, then running it followed by `./gradlew build`.

## License and acknowledgements

This project incorporates and modifies code from [MeteorDevelopment/meteor-client](https://github.com/MeteorDevelopment/meteor-client), copyright Meteor Development, licensed under **GPL-3.0**. All redistributed binary builds must be accompanied by the corresponding modified source and this attribution. See the upstream `LICENSE`, maintained in the source artifact. Beegi is not an official Meteor release.
