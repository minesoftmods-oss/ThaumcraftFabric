#!/usr/bin/env python3
"""Convert Minecraft 1.7.10 .lang files to the JSON format used by 26.x.

Usage:  python tools/convert_lang.py <dir with *.lang> [<output dir>] [<locales, comma separated>]
Default output:  src/main/resources/assets/thaumcraft/lang
Default locales: en_US,ru_RU (the only languages kept in the port)

- reads UTF-8 (BOM tolerated), CRLF or LF
- skips blank lines and lines starting with '#'
- splits on the FIRST '=' only, keeps the value as is (only line ending removed)
- file names are lowercased (en_US.lang -> en_us.json) as required by 26.x
- the LAST occurrence wins on duplicate keys, duplicates are reported
- broken lines in the original translations are repaired and reported
Keys are kept exactly as in the original mod. They are remapped to the new
registry names later, once blocks/items/entities are registered.
"""
import json, pathlib, sys

# Old 1.7.10 key -> new 26.x key, written by gen_item_assets.py (and later by the other generators)
TOOLS_DIR = pathlib.Path(__file__).parent
# New keys that do not exist in the original lang files; added to every locale as is.
EXTRA_LANG_FILE = TOOLS_DIR / "extra_lang.json"

def load_key_map():
    """Merges every tools/key_map*.json (one file per generator)."""
    merged = {}
    for f in sorted(TOOLS_DIR.glob("key_map*.json")):
        merged.update(json.loads(f.read_text(encoding="utf-8")))
    return merged

# Known key typos in the original translations: locale -> {wrong key: right key}
KEY_RENAMES = {"ru_RU": {"champion.bold": "champion.mod.bold", "champion.spine": "champion.mod.spine"}}

def convert(src: pathlib.Path, ref_keys=()):
    """Returns (data, dups, repaired, skipped).
    Repairs two defects found in the original translations:
      * 'key' glued to 'value' (missing '=')  -> split on the longest known English key
      * value broken over two lines            -> appended to the previous value
    """
    data, dups, repaired, skipped = {}, [], [], []
    last = None
    keys_by_len = sorted(ref_keys, key=len, reverse=True)
    text = src.read_text(encoding="utf-8-sig")
    for n, line in enumerate(text.splitlines(), 1):
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        if "=" in line:
            k, v = line.split("=", 1)
            k = k.strip()
            if k in data: dups.append(k)
            data[k] = v
            last = k
            continue
        fix = next((k for k in keys_by_len if line.startswith(k)), None)
        if fix:
            data[fix] = line[len(fix):]
            last = fix
            repaired.append((n, "missing '=' after " + fix))
        elif last and len(line.strip()) > 3:
            data[last] += line
            repaired.append((n, "continuation of " + last))
        else:
            skipped.append(n)
    return data, dups, repaired, skipped

def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    src_dir = pathlib.Path(sys.argv[1])
    out_dir = pathlib.Path(sys.argv[2]) if len(sys.argv) > 2 else pathlib.Path("src/main/resources/assets/thaumcraft/lang")
    out_dir.mkdir(parents=True, exist_ok=True)
    locales = (sys.argv[3] if len(sys.argv) > 3 else "en_US,ru_RU").split(",")
    total = 0
    en_data = convert(src_dir / "en_US.lang")[0]
    ref = tuple(en_data.keys())
    for loc in locales:
        f = src_dir / (loc + ".lang")
        data, dups, repaired, skipped = convert(f, ref)
        notes = [f"repaired line {n}: {why}" for n, why in repaired]
        for bad, good in KEY_RENAMES.get(loc, {}).items():
            if bad in data and good not in data:
                data[good] = data.pop(bad); notes.append(f"renamed key {bad} -> {good}")
        missing = [k for k in en_data if k not in data]
        for k in missing:                       # fall back to English so no raw key is shown in game
            data[k] = en_data[k]
        if missing and loc != "en_US":
            notes.append(f"{len(missing)} missing keys filled from English: {missing}")
        if dups: notes.append(f"duplicate keys (last wins): {sorted(set(dups))}")
        if skipped: notes.append(f"skipped lines: {skipped}")
        key_map = load_key_map()
        if key_map:
            data = {key_map.get(k, k): v for k, v in data.items()}
            notes.append(f"{sum(1 for k in key_map if k in en_data)} keys remapped to 26.x names")
        if EXTRA_LANG_FILE.exists():
            extra = json.loads(EXTRA_LANG_FILE.read_text(encoding="utf-8"))
            data.update(extra)
            notes.append(f"{len(extra)} new keys added from extra_lang.json")
        out = out_dir / (loc.lower() + ".json")
        out.write_text(json.dumps(data, ensure_ascii=False, indent="\t") + "\n", encoding="utf-8")
        total += len(data)
        print(f"{f.name:12} -> {out.name:12} {len(data):5} keys" + "".join("\n    " + x for x in notes))
    print(f"total keys: {total}")

if __name__ == "__main__":
    main()
