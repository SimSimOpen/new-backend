#!/usr/bin/env python3
"""Extend the Tashkent location seed data from a Yandex suggest-geo sweep.

Append-only: every district and place already in the JSON keeps its id and names, because listings
store those ids. New areas get the next free ids. After adding rows, bump ADDITIONS_VERSION in
product/.../config/DatabaseSeeder so the next boot inserts them.

Also regenerates the admin map-center constant, using the point Yandex reports for each district.

Usage:
    python3 tools/generate_tashkent_seed.py <raw.jsonl> <uz_latin.jsonl> <mahallas-26-dir>

raw.jsonl lines:      {"id", "path": "Узбекистан, Ташкент, <район>, ...", "title", "point": [lon, lat]}
uz_latin.jsonl lines: {"id", "uz"}
mahallas-26-dir:      mahallalar/mahallas/26, used only to place areas Yandex lists without a district.
"""
import glob
import json
import os
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
DATA = REPO / "product" / "src" / "main" / "resources" / "data"
ADMIN_CONST = REPO.parents[1] / "sim-sim-admin" / "src" / "app" / "shared" / "constants" / "district-centers.ts"

REGION_ID = 1
NEW_DISTRICTS = [{"name_uz": "Yakkasaroy tumani", "name_ru": "Яккасарайский район"}]
GOV_DISTRICTS = {"2601": "Мирабадский район", "2602": "Мирзо-Улугбекский район", "2603": "Юнусабадский район",
                 "2604": "Яккасарайский район", "2605": "Шайхантахурский район", "2606": "Чиланзарский район",
                 "2607": "Сергелийский район", "2608": "Яшнабадский район", "2609": "Алмазарский район",
                 "2610": "Учтепинский район", "2611": "Бектемирский район", "2612": "Янгихаётский район"}

# Not neighbourhoods: housing complexes, business centres and index pages.
SKIP = re.compile(r"жилой комплекс|^ЖК |коттеджный|деловой центр|Индекс", re.I)
MAHALLA = "махаллинский сход граждан"

LATIN = dict(zip("абвгдеёжзийклмнопрстуфхцчшщъыьэюя",
                 ["a", "b", "v", "g", "d", "e", "yo", "j", "z", "i", "y", "k", "l", "m", "n", "o", "p", "r", "s", "t",
                  "u", "f", "x", "s", "ch", "sh", "sh", "", "i", "", "e", "yu", "ya"]))


def latin(text: str) -> str:
    return "".join(LATIN.get(c.lower(), c).capitalize() if c.isupper() else LATIN.get(c, c) for c in text)


def uz_fallback(title: str) -> str:
    """Latin rendering for areas Yandex returned no Uzbek title for."""
    if title.startswith(MAHALLA):
        return f"{latin(title[len(MAHALLA):].strip())} mahalla fuqarolar yigʻini"
    m = re.fullmatch(r"(\d+)-й квартал", title)
    if m:
        return f"{m.group(1)}-mavze"
    if title.startswith("массив "):
        return f"{latin(title[7:])} dahasi"
    if title.startswith("квартал "):
        return f"{latin(title[8:])} mavzesi"
    return latin(title)


def read_json(path: Path) -> list[dict]:
    return json.loads(path.read_text(encoding="utf-8-sig"))


def write_json(path: Path, rows: list[dict]) -> None:
    body = ",\n".join(json.dumps(row, ensure_ascii=False) for row in rows)
    path.write_text("[\n" + body + "]\n", encoding="utf-8")


def in_ring(lon: float, lat: float, ring: list) -> bool:
    inside = False
    for (x1, y1), (x2, y2) in zip(ring, ring[1:]):
        if (y1 > lat) != (y2 > lat) and lon < (x2 - x1) * (lat - y1) / (y2 - y1) + x1:
            inside = not inside
    return inside


def load_gov_polygons(folder: str) -> list[tuple[str, list]]:
    polygons = []
    for f in glob.glob(os.path.join(folder, "*.json")):
        rayon = GOV_DISTRICTS[os.path.basename(f)[:-5]]
        for m in json.loads(Path(f).read_text(encoding="utf-8-sig"))["mahalla"]:
            try:
                geo = json.loads(json.loads(m["geo_polygon"])["value"])
            except (TypeError, KeyError, ValueError):
                continue
            rings = [p[0] for p in geo["coordinates"]] if geo["type"] == "MultiPolygon" else [geo["coordinates"][0]]
            polygons.extend((rayon, r) for r in rings)
    return polygons


def natural(text: str) -> list:
    return [(0, int(t), "") if t.isdigit() else (1, 0, t.lower()) for t in re.split(r"(\d+)", text) if t]


def main() -> None:
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    rows: dict[str, dict] = {}
    for line in Path(sys.argv[1]).read_text(encoding="utf-8").splitlines():
        r = json.loads(line)
        rows.setdefault(r["id"], r)
    uz = {}
    for line in Path(sys.argv[2]).read_text(encoding="utf-8").splitlines():
        r = json.loads(line)
        uz[r["id"]] = r["uz"]
    polygons = load_gov_polygons(sys.argv[3])
    uz_by_path = {r["path"]: uz[i] for i, r in rows.items() if i in uz}

    districts = read_json(DATA / "districts.json")
    villages = read_json(DATA / "villages.json")
    known_ru = {d["name_ru"] for d in districts}
    for d in NEW_DISTRICTS:
        if d["name_ru"] not in known_ru:
            districts.append({"id": max(x["id"] for x in districts) + 1, "region_id": REGION_ID,
                              "name_uz": d["name_uz"], "name_oz": d["name_uz"], "name_ru": d["name_ru"]})
    by_ru = {d["name_ru"]: d["id"] for d in districts}

    centers: dict[int, list[float]] = {}
    candidates: dict[int, list[tuple[str, str, str]]] = {d["id"]: [] for d in districts}
    skipped, unplaced, by_polygon, fallback = [], [], [], []
    for r in rows.values():
        parts = [p.strip() for p in r["path"].split(",")]
        if parts[:2] != ["Узбекистан", "Ташкент"] or len(parts) < 3:
            skipped.append(r["path"])
            continue
        rayon, trail = parts[2], parts[3:]
        if rayon in by_ru and not trail:
            centers[by_ru[rayon]] = r["point"]
            continue
        if rayon not in by_ru:
            # Listed directly under the city: find its district from the official mahalla polygons.
            trail = parts[2:]
            rayon = next((name for name, ring in polygons if in_ring(*r["point"], ring)), None)
            if rayon is None:
                unplaced.append(r["path"])
                continue
            by_polygon.append(f"{r['path']} -> {rayon}")
        if any(SKIP.search(p) for p in trail):
            skipped.append(r["path"])
            continue
        title = trail[-1]
        name_uz = uz.get(r["id"])
        if not name_uz:
            name_uz = uz_fallback(title)
            fallback.append(r["path"])
        # A few Uzbek titles from Yandex are still in Cyrillic.
        name_uz = latin(name_uz)
        parent = trail[-2] if len(trail) > 1 else None
        full_ru = title
        if parent and parent.startswith("массив ") and not title.startswith(MAHALLA):
            full_ru = f"{parent[7:]}, {title}"
            parent_path = r["path"].rsplit(",", 1)[0]
            name_uz = f"{uz_by_path.get(parent_path) or uz_fallback(parent)}, {name_uz}"
        candidates[by_ru[rayon]].append((title, full_ru, name_uz))

    next_id = max(v["id"] for v in villages) + 1
    added = {}
    for district_id, items in candidates.items():
        existing = [v for v in villages if v["district_id"] == district_id]
        taken_ru = {v["name_ru"] for v in existing}
        taken_uz = {v["name_uz"] for v in existing}
        # Massivs and quarters first, then mahallas, each in natural order.
        for title, full_ru, name_uz in sorted(items, key=lambda i: (i[0].startswith(MAHALLA), natural(i[1]))):
            if title in taken_ru or full_ru in taken_ru or name_uz in taken_uz:
                continue
            villages.append({"id": next_id, "district_id": district_id, "name_uz": name_uz,
                             "name_oz": name_uz, "name_ru": full_ru})
            taken_ru.update({title, full_ru})
            taken_uz.add(name_uz)
            next_id += 1
            added[district_id] = added.get(district_id, 0) + 1

    missing = [d["name_uz"] for d in districts if d["id"] not in centers]
    if missing:
        sys.exit(f"no Yandex point for districts: {missing}")

    write_json(DATA / "districts.json", districts)
    write_json(DATA / "villages.json", villages)
    body = ",\n".join(f"  {d['id']}: {{ lon: {centers[d['id']][0]}, lat: {centers[d['id']][1]} }}" for d in districts)
    ADMIN_CONST.write_text(
        "// Generated by new-backend/simsim/tools/generate_tashkent_seed.py - do not edit.\n"
        "// Map center (lon/lat) per district id, used to pan the admin publish-form map.\n"
        "export const DISTRICT_CENTERS: Record<number, { lon: number; lat: number }> = {\n"
        + body + "\n};\n",
        encoding="utf-8",
    )

    print(f"districts={len(districts)} villages={len(villages)} added={sum(added.values())} skipped={len(skipped)}")
    for d in districts:
        total = sum(1 for v in villages if v["district_id"] == d["id"])
        print(f"  {d['id']:>2} {d['name_uz']:<24} +{added.get(d['id'], 0):<4} total {total}")
    print("placed by polygon:", *by_polygon, sep="\n  ")
    print("unplaced:", *unplaced, sep="\n  ")
    print("uz fallback:", len(fallback))


if __name__ == "__main__":
    main()
