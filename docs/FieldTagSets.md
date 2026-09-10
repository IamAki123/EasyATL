# AprilTag field sets

[README](../README.md) · [Library files](LibraryFiles.md) · [API](API.md)

EasyATL only uses tags you **register**. A tag on the camera that is not in the set is ignored. This page is how to pick a set: latest season (default), an older game, or your own coordinates.

## Pick one line in Constants

`DefaultSdkConstants.addTags` (in the AAR) already calls `useLatestSeason()`. Copy-in files do the same from `createLocalizer`. You can run the SDK tuner on those AAR tags without copying a constants file ([Tuning → AAR defaults](Tuning.md#aar-defaults-no-constants-file)). To change the set, copy Constants and change **one** of these:

```java
public static void addTags(FtcEasyATL localizer) {
    localizer.useLatestSeason();
    // localizer.useSeason(FieldTags.Season.INTO_THE_DEEP);
    // localizer.useFieldSet(customAprilTags());
    // localizer.addCurrentGameTags();
}

public static FieldTags customAprilTags() {
    return FieldTags.custom("Practice field")
            .add(21, 8, 8, Math.toRadians(45), "left wall")
            .add(22, 72, 8, Math.toRadians(90), "audience")
            .build();
}
```

| Call | When to use |
| --- | --- |
| `useLatestSeason()` | **Default.** Newest table bundled in EasyATL (DECODE 2025–26). |
| `useSeason(FieldTags.Season.INTO_THE_DEEP)` | You are practicing or replaying **that** year’s field. |
| `useSeason(FieldTags.Season.DECODE)` | Same as latest, written out so it stays DECODE after a future library update. |
| `useFieldSet(customAprilTags())` | Practice field, homemade tags, or CAD you typed yourself. |
| `addCurrentGameTags()` | Trust the **installed FTC SDK** library (can include CAD fixes FIRST ships mid-season). Skips tags with no field pose (DECODE obelisk 21–23). |
| `addTag(id, x, y, facing)` | One extra tag on top of a season (`useLatestSeason()` first, then `addTag`). Same ID overwrites. |

`useLatestSeason()`, `useSeason(...)`, and `useFieldSet(...)` **replace** the whole map. `addTag` / `addTags` / `addCurrentGameTags` **merge**.

## What X, Y, and facing mean

Origin is the **center of the field**. Inches. Heading `0` is field `+X`, CCW-positive (same as EasyATL / Pedro).

**Facing** is the direction **out of the printed face toward the camera** — the way a robot looking at the tag square-on would see the tag. It is **not** “the wall’s heading” unless those happen to match.

Tape the tag center, not the plastic frame. Event setup is sloppy; CAD is a starting point.

## Bundled seasons

`FieldTags.Season.latest()` is always the newest table in this library.

### DECODE (2025–26) — `latest()`

Localization tags only. **Do not** add obelisk 21, 22, 23 (they move every match).

| ID | X (in) | Y (in) | Facing | Name |
| ---: | ---: | ---: | ---: | --- |
| 20 | −58.35 | −55.63 | 54° | Blue goal |
| 24 | −58.35 | 55.63 | −54° | Red goal |

```java
FieldTags.latest();
FieldTags.season(FieldTags.Season.DECODE);
FieldTags.decode();
```

### INTO THE DEEP (2024–25)

Perimeter tags. Tag 11 matches the FTC localization tutorial (`X = -72`, `Y = 48`).

| ID | X (in) | Y (in) | Facing | Name |
| ---: | ---: | ---: | ---: | --- |
| 11 | −72 | 48 | 0° (`+X`) | Blue wall +Y |
| 12 | −72 | 0 | 0° | Blue wall center |
| 13 | −72 | −48 | 0° | Blue wall −Y |
| 14 | 72 | −48 | 180° | Red wall −Y |
| 15 | 72 | 0 | 180° | Red wall center |
| 16 | 72 | 48 | 180° | Red wall +Y |

```java
localizer.useSeason(FieldTags.Season.INTO_THE_DEEP);
```

### CENTERSTAGE (2023–24)

Not bundled as a coordinate table (backdrop CAD lived in that year’s SDK). Options:

```java
localizer.addCurrentGameTags(); // if your SDK is still CENTERSTAGE
// or
localizer.useFieldSet(customAprilTags()); // type IDs 1–6 (backdrops) from your CAD
```

`useSeason(FieldTags.Season.CENTERSTAGE)` throws on purpose so you do not silently get an empty map. IDs were 1–3 blue backdrop, 4–6 red backdrop, 7–10 audience wall (small; usually skip for pose).

### Next season

When FIRST publishes a new game, EasyATL should add a `Season` value, a `FieldTags` table, and move `Season.latest()` to that game. Pin `useSeason(FieldTags.Season.DECODE)` if you do not want the default to move under you.

## Custom sets (`customAprilTags()`)

Put this in **your** Constants (already stubbed in the copy-in files):

```java
public static FieldTags customAprilTags() {
    return FieldTags.custom("Practice field")
            .add(21, 8, 8, Math.toRadians(45), "left wall")
            .add(22, 72, 8, Math.toRadians(90), "audience")
            .build();
}
```

Then `localizer.useFieldSet(customAprilTags())`.

Official season **plus** one extra tag:

```java
localizer.useLatestSeason();
localizer.addTag(21, 8, 8, Math.toRadians(45));
```

or

```java
FieldTags.decode().plus(new FieldTags.Tag(21, 8, 8, Math.toRadians(45), "practice"));
```

`FieldTags.custom("...").build()` with zero tags throws.

## SDK vs hardcoded

| | Hardcoded `useLatestSeason()` | `addCurrentGameTags()` |
| --- | --- | --- |
| Works without Vision metadata | Yes | Needs FTC Vision SDK at runtime |
| Updates when FIRST patches CAD | When EasyATL ships a new table | When you update the FTC SDK |
| Omits moving tags | DECODE table omits 21–23 | Skips entries with no `fieldPosition` |

For a match this year, either is fine. Tape-verify.

---

[How it works](MathButDumbed.md) · [Prerequisites](Prerequisites.md) · [Tuning](Tuning.md)
