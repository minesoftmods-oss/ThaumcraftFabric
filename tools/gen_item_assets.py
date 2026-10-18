#!/usr/bin/env python3
"""Generate item model files and the lang key map from tools/items.json.

Usage:  python tools/gen_item_assets.py        (run from the project root)

For every item in items.json it writes
  assets/thaumcraft/items/<id>.json         client item definition (26.x)
  assets/thaumcraft/models/item/<id>.json   flat generated model
  assets/thaumcraft/textures/item/<tex>.png (+ .png.mcmeta) copied from tools/legacy_textures/item
and tools/key_map.json (old 1.7.10 lang key -> new item.thaumcraft.<id>) that
tools/convert_lang.py applies. It also checks that every id is registered in
ModItems.java and that every texture exists.
"""
import json, pathlib, re, shutil, struct, sys

root = pathlib.Path(".")
res = root / "src/main/resources/assets/thaumcraft"
items = json.loads((root / "tools/items.json").read_text(encoding="utf-8"))
java = (root / "src/main/java/com/thaumcraft/fabric/registry/ModItems.java").read_text(encoding="utf-8")
registered = set(re.findall(r'register\("([a-z0-9_]+)"', java))
problems = []
(res / "items").mkdir(parents=True, exist_ok=True); (res / "models/item").mkdir(parents=True, exist_ok=True)
keymap = {}
for it in items:
    i, tex = it["id"], it["texture"]
    src = root / "tools/legacy_textures/item" / f"{tex}.png"
    if not src.exists():
        problems.append(f"missing texture item/{tex}.png for {i} in tools/legacy_textures")
    else:
        (res / "textures/item").mkdir(parents=True, exist_ok=True)
        for f in (src, src.with_name(src.name + ".mcmeta")):
            if f.exists(): shutil.copy2(f, res / "textures/item" / f.name)
        w, h = struct.unpack(">II", src.read_bytes()[16:24])
        if w != h and not src.with_name(src.name + ".mcmeta").exists():
            problems.append(f"{tex}.png is {w}x{h} and has no .mcmeta: it would render stretched")
    if i not in registered: problems.append(f"{i} is in items.json but not registered in ModItems.java")
    (res / "items" / f"{i}.json").write_text(json.dumps({"model": {"type": "minecraft:model", "model": f"thaumcraft:item/{i}"}}, indent="\t") + "\n", encoding="utf-8")
    (res / "models/item" / f"{i}.json").write_text(json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": f"thaumcraft:item/{tex}"}}, indent="\t") + "\n", encoding="utf-8")
    keymap[it["old_key"]] = f"item.thaumcraft.{i}"
for r in sorted(registered - {it["id"] for it in items}):
    problems.append(f"{r} is registered in ModItems.java but missing from items.json")
(root / "tools/key_map.json").write_text(json.dumps(keymap, indent="\t", ensure_ascii=False) + "\n", encoding="utf-8")
print(f"{len(items)} items, {len(keymap)} lang keys mapped")
if problems:
    print("PROBLEMS:"); [print(" -", p) for p in problems]; sys.exit(1)
print("consistency check OK")
