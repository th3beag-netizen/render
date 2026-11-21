#!/usr/bin/env python3
"""Reproducible Beegi customization of upstream Meteor 26.3.

Meteor Client copyright and GPL-3.0 licensing are retained.
This is a Meteor-derived build, not a clean-room Peegi port.
"""
from pathlib import Path
import json

root = Path("meteor-upstream")
metadata = root / "src/main/resources/fabric.mod.json"
gui = root / "src/main/java/meteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme.java"
properties = root / "gradle.properties"

assert all(p.exists() for p in (metadata, gui, properties)), "Pinned Meteor source layout has changed"

# Keep 'meteor-client' as the internal mod id; changing it without refactoring
# all mixins, resources and constants would break the client.
data = json.loads(metadata.read_text())
assert data["id"] == "meteor-client"
data["name"] = "Beegi (Meteor-based)"
data["description"] = "Beegi development fork of Meteor Client, Minecraft 26.3. Meteor authors and GPL-3.0 attribution retained."
data.setdefault("custom", {})["beegi:upstream"] = "MeteorDevelopment/meteor-client@ec8cdb088af88582bade7cd3b0dfbe71ccd2285c"
metadata.write_text(json.dumps(data, indent=2) + "\n")

text = gui.read_text()
replacements = {
    "new SettingColor(145, 61, 226)": "new SettingColor(110, 185, 255)",
    "new SettingColor(130, 0, 255)": "new SettingColor(70, 145, 240)",
    "new SettingColor(140, 30, 255)": "new SettingColor(95, 175, 255)",
    "new SettingColor(150, 60, 255)": "new SettingColor(135, 200, 255)",
    "new SettingColor(100, 35, 170)": "new SettingColor(50, 120, 200)",
}
for a,b in replacements.items():
    assert a in text, f"Expected upstream theme default not found: {a}"
    text = text.replace(a,b)
gui.write_text(text)

s = properties.read_text()
assert "archives_base_name=meteor-client" in s
properties.write_text(s.replace("archives_base_name=meteor-client", "archives_base_name=beegi-meteor-client"))

print("Applied Meteor-based Beegi identity, cool-blue theme defaults, and distinct archive name.")
