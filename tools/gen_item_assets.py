#!/usr/bin/env python3
"""Generate item model files and the lang key map from tools/items.json.

Usage:  python tools/gen_item_assets.py        (run from the project root)

For every item in items.json it writes
  assets/thaumcraft/items/<id>.json         client item definition (26.x)
  assets/thaumcraft/models/item/<id>.json   flat generated model
and tools/key_map.json (old 1.7.10 lang key -> new item.thaumcraft.<id>) that
tools/convert_lang.py applies. It also checks that every id is registered in
ModItems.java and that every texture exists.
"""
import json, pathlib, re, sys

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
    if not (res / "textures/item" / f"{tex}.png").exists(): problems.append(f"missing texture item/{tex}.png for {i}")
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
