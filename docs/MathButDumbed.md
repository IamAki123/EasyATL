# How EasyATL works (simple version)

[README](../README.md) · [Full formulas](Math.md)

This is the same pipeline as [Math.md](Math.md), explained like you are new to vision. No formulas required.

## The one-sentence version

The camera sees AprilTags. EasyATL throws away the bad looks, trusts the good ones, and turns that into “the robot is about *here*, facing *that way*.”

It does **not** drive the robot. It does **not** open the camera. Your OpMode already gets a list of tags; EasyATL only decides which of those looks are worth believing.

## What “pose” means

Three numbers:

- **X and Y** — where the robot is on the field, in inches
- **Heading** — which way it is facing (like a compass, but in FTC’s field directions)

That is the output: `getPose()`.

## A loop on the robot, in English

Every time you call `localize()`:

### 1. Forget old vision a little

If you used to see tags and now you do not, the **quality** number slowly drops. That is EasyATL saying “this pose is getting stale.” Your wheels should keep tracking. Quality is a gut score from 0 to 1, not a scientific probability.

### 2. Ignore tags you never told it about

You must register field tags (`addCurrentGameTags()` or `addTag(...)`). A tag on screen that is not on that list does nothing. DECODE’s spinning/obelisk tags that move each match should stay off the list.

### 3. Throw out ugly detections

Before any fancy averaging, EasyATL asks:

- Is the tag **too far**? (default: farther than 96 in)
- Is it **way off to the side** of the image? (bearing)
- Is the tag **twisted** a lot relative to the camera? (yaw)
- Are the numbers even real? (not NaN, range must be positive)

If any of those fail, that tag never becomes a pose. This is why glancing, distant, or sideways tags should not yank your robot.

### 4. Turn each good tag into “where the robot must be”

You already told EasyATL:

- where that tag sits on the field
- where the **lens** sits on the robot (forward, right, yaw, and pitch)

From “I see tag 20 this far in front and a bit to the right,” it backs out the robot’s field pose. **Wrong lens numbers or wrong tag map = wrong pose every time.** Filters cannot fix a tape-measure mistake. Measure the glass of the lens, not the plastic housing. If the camera looks down, set pitch (usually a small negative number).

### 5. Trust some tags more than others

A tag that is close and facing you is more believable than one far away at a steep angle. EasyATL gives close / head-on tags a **higher weight**. If the FTC SDK also reports a “how sure am I?” score (`decisionMargin`), a higher score counts more.

### 6. If several tags disagree, ignore the weirdo

Suppose two tags say you are at (36, 0) and one tag says you are at (80, 0). EasyATL first picks a “middle of the pack” guess, then **drops** the tag that is way off. One bad detection should not drag the whole answer.

If only one tag is in view, there is no pack — that one tag is the answer (if it passed step 3).

### 7. Blend the keepers

The tags that agree are averaged, with the more-trusted ones pulling harder. Heading is averaged the careful way so “almost 180° left” and “almost 180° right” do not accidentally become “facing 0°.”

### 8. Don’t jump the full distance in one frame (optional)

You can cap how far one camera frame is allowed to move the pose (`maxStepInches`). Useful if you correct odometry and do not want a single glitch to teleport the robot. Off by default.

### 9. Smooth so the pose does not twitch

Brand-new first pose: take it as-is. After that, mix “old pose” and “new measurement.”

Think of a slider from 0 to 1 called `smoothingAlpha`:

- **1.0** — always jump to the new camera answer (twitchy)
- **0.65** (default) — mostly new, a little old (typical)
- **lower** — calmer, but it lags when you drive

### 10. Quality and “how unsure are we?”

Quality goes **up** when:

- tags are close and head-on (good weights)
- several tags agree
- they are not fighting each other (small leftover disagreement)

Quality goes **down** when tags disappear (step 1). Sample TeleOps often wait for quality ≥ 0.20 before writing vision into odometry. That 0.20 is a **suggestion**, not a magic library constant.

There is also a rough “uncertainty” number for people who fuse with odometry (“only correct wheels when vision looks tighter”). It is a hint, not inches of guaranteed truth. Measure **your** robot with a tape.

## Pitch, in one picture

If the camera points at the floor a bit, “distance along the camera’s view” is longer than “distance along the floor.” If you leave pitch at 0, the robot looks farther from the tag than it really is, and that error gets **worse the farther you are**. Set pitch. It is the usual reason pose is fine up close and worse at 6 ft.

## What you will see on the Driver Station

| Telemetry | Meaning |
| --- | --- |
| **Visible** | Configured tags the camera saw this loop (including ones we later threw out) |
| **Accepted** | Tags that actually built the pose |
| **Quality** | How much to trust that pose right now |
| **REJECT range / bearing / yaw / outlier** | Why that tag did not count (tuner debug) |
| `localize()` returned **false** | Nothing usable this loop; last pose is **frozen** until tags come back |

A tag on the preview is not the same as an accepted pose.

## What EasyATL is not

- Not a full 3D SLAM system
- Not a replacement for dead wheels
- Not “the SDK was wrong so we invented a new camera”
- Not something you skip to save loop time — it is cheap

If tape and vision disagree by a **constant** amount everywhere, fix geometry (lens + tag map). If they jump only at the edge of the image or far away, then tune the filters. That order is the whole reliability story.

---

Want the actual equations? [Math.md](Math.md). Ready to use it? [README](../README.md).
