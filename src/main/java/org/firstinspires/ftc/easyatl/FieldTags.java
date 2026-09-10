package org.firstinspires.ftc.easyatl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A named set of field AprilTags (ID, X, Y, facing) for {@link EasyATL#addTag}.
 *
 * <p>Defaults: {@link #latest()} is the newest season bundled here
 * ({@link Season#DECODE}). Previous games: {@link #season(Season)}. Your own
 * practice field: {@link #custom(String)}.</p>
 *
 * <p>On a robot with the matching FTC SDK, {@link FtcEasyATL#addCurrentGameTags()} still
 * reads FIRST’s live library and can beat a hardcoded table after a CAD tweak.</p>
 */
public final class FieldTags {
    /**
     * FTC games with a hardcoded planar map in this library.
     * {@link #latest()} always points at the newest of these.
     */
    public enum Season {
        CENTERSTAGE("CENTERSTAGE", "2023-2024"),
        INTO_THE_DEEP("INTO THE DEEP", "2024-2025"),
        DECODE("DECODE", "2025-2026");

        final String title;
        final String years;

        Season(String title, String years) {
            this.title = title;
            this.years = years;
        }

        /** Newest season this library ships a table for. */
        public static Season latest() {
            return DECODE;
        }
    }

    /** One field tag: ID, center X/Y in inches, facing out of the printed face toward the camera. */
    public static final class Tag {
        public final int id;
        public final double x;
        public final double y;
        public final double facingRadians;
        public final String name;

        public Tag(int id, double x, double y, double facingRadians) {
            this(id, x, y, facingRadians, "");
        }

        public Tag(int id, double x, double y, double facingRadians, String name) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.facingRadians = facingRadians;
            this.name = name == null ? "" : name;
        }
    }

    /** Builds a custom field set for a practice field or extra tags. */
    public static final class Builder {
        private final String seasonName;
        private final String seasonYears;
        private final List<Tag> tags = new ArrayList<Tag>();

        Builder(String seasonName, String seasonYears) {
            this.seasonName = seasonName == null || seasonName.length() == 0 ? "Custom" : seasonName;
            this.seasonYears = seasonYears == null ? "" : seasonYears;
        }

        public Builder add(int id, double x, double y, double facingRadians) {
            return add(id, x, y, facingRadians, "");
        }

        public Builder add(int id, double x, double y, double facingRadians, String name) {
            tags.add(new Tag(id, x, y, facingRadians, name));
            return this;
        }

        public Builder add(Tag tag) {
            if (tag == null) throw new IllegalArgumentException("tag cannot be null");
            tags.add(tag);
            return this;
        }

        /** Copies every tag from another set (for “official season + my extra tag”). */
        public Builder addAll(FieldTags other) {
            if (other == null) throw new IllegalArgumentException("field tags cannot be null");
            tags.addAll(other.tags);
            return this;
        }

        public FieldTags build() {
            if (tags.isEmpty()) {
                throw new IllegalArgumentException("custom field set must contain at least one tag");
            }
            return new FieldTags(seasonName, seasonYears, tags);
        }
    }

    private final String seasonName;
    private final String seasonYears;
    private final List<Tag> tags;

    public FieldTags(String seasonName, String seasonYears, List<Tag> tags) {
        this.seasonName = seasonName;
        this.seasonYears = seasonYears;
        this.tags = Collections.unmodifiableList(new ArrayList<Tag>(tags));
    }

    /** Newest bundled season (currently DECODE 2025–26). */
    public static FieldTags latest() {
        return season(Season.latest());
    }

    /**
     * Official table for a past or current game.
     *
     * <p>{@link Season#CENTERSTAGE} is not hardcoded (backdrop CAD lived in that year’s SDK).
     * Use {@link FtcEasyATL#addCurrentGameTags()} on a CENTERSTAGE SDK, or
     * {@link #custom(String)} from your drawings.</p>
     */
    public static FieldTags season(Season season) {
        if (season == null) throw new IllegalArgumentException("season cannot be null");
        switch (season) {
            case DECODE:
                return decode();
            case INTO_THE_DEEP:
                return intoTheDeep();
            case CENTERSTAGE:
                throw new IllegalArgumentException(
                        "CENTERSTAGE is not bundled as coordinates. "
                                + "On that season's SDK call addCurrentGameTags(), "
                                + "or FieldTags.custom(\"CENTERSTAGE\") from CAD. See docs/FieldTagSets.md");
            default:
                throw new IllegalArgumentException("unknown season: " + season);
        }
    }

    /** Start a custom set. Call {@code .add(id, x, y, facing).build()}. */
    public static Builder custom(String name) {
        return new Builder(name, "custom");
    }

    /**
     * DECODE presented by RTX (2025–26). Goal tags only.
     *
     * <p>Obelisk tags 21–23 move each match and are omitted. Positions are FTC coordinates
     * (inches, origin at field center). Tape-verify at your event.</p>
     */
    public static FieldTags decode() {
        return new FieldTags(Season.DECODE.title, Season.DECODE.years, Arrays.asList(
                new Tag(20, -58.35, -55.63, Math.toRadians(54), "Blue goal"),
                new Tag(24, -58.35, 55.63, Math.toRadians(-54), "Red goal")
        ));
    }

    /**
     * INTO THE DEEP (2024–25) perimeter tags 11–16.
     *
     * <p>Tag 11 matches the FTC localization tutorial ({@code X=-72}, {@code Y=48}).</p>
     */
    public static FieldTags intoTheDeep() {
        return new FieldTags(Season.INTO_THE_DEEP.title, Season.INTO_THE_DEEP.years, Arrays.asList(
                new Tag(11, -72, 48, 0, "Blue wall +Y"),
                new Tag(12, -72, 0, 0, "Blue wall center"),
                new Tag(13, -72, -48, 0, "Blue wall -Y"),
                new Tag(14, 72, -48, Math.PI, "Red wall -Y"),
                new Tag(15, 72, 0, Math.PI, "Red wall center"),
                new Tag(16, 72, 48, Math.PI, "Red wall +Y")
        ));
    }

    public String seasonName() {
        return seasonName;
    }

    public String seasonYears() {
        return seasonYears;
    }

    public List<Tag> tags() {
        return tags;
    }

    /** This set plus extra tags (same IDs overwrite when applied later). */
    public FieldTags plus(Tag extra) {
        return custom(seasonName).addAll(this).add(extra).build();
    }

    public FieldTags plus(FieldTags extra) {
        return custom(seasonName).addAll(this).addAll(extra).build();
    }

    /** Registers every tag on a core localizer (does not clear existing IDs). */
    public FieldTags apply(EasyATL localizer) {
        if (localizer == null) throw new IllegalArgumentException("localizer cannot be null");
        for (Tag tag : tags) localizer.addTag(tag.id, tag.x, tag.y, tag.facingRadians);
        return this;
    }

    /** Registers every tag on the FTC adapter (does not clear existing IDs). */
    public FieldTags apply(FtcEasyATL localizer) {
        if (localizer == null) throw new IllegalArgumentException("localizer cannot be null");
        for (Tag tag : tags) localizer.addTag(tag.id, tag.x, tag.y, tag.facingRadians);
        return this;
    }
}
