#!/usr/bin/env python3
"""Generate the placeholder demo clips and list thumbnails shipped in :core:data.

Why procedurally generated clips?
---------------------------------
v1 needs a working video pipeline (player, asset resolution, looping, pause-on-scroll) and a
visual cue for every movement pattern. Licensed footage is a content-production task; this
script produces clean *schematic* animations instead, so the app is complete and demoable.
Replacing them later is a data-only change: drop an MP4 named after the exercise into
`core/data/src/main/assets/videos/` and set `videoAsset` in exercises.json.

Usage:
    python3 tools/generate_demo_videos.py [--only squat] [--no-thumbs]

Requires: Pillow and an ffmpeg binary (the script also accepts imageio-ffmpeg).
"""

from __future__ import annotations

import argparse
import json
import math
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "core" / "data" / "src" / "main" / "assets"
VIDEO_DIR = ASSETS / "videos"
THUMB_DIR = ASSETS / "thumbnails"
CATALOGUE = ASSETS / "exercises.json"

FPS = 24
SECONDS = 6
FRAMES = FPS * SECONDS
SIZE = 720
THUMB_SIZE = 360

BG_TOP = (18, 21, 27)
BG_BOTTOM = (10, 12, 16)
GRID = (30, 35, 44)
GROUND = (52, 60, 74)
BODY = (238, 242, 248)
BODY_DIM = (150, 160, 175)
ACCENT = (255, 106, 61)
ACCENT_SOFT = (255, 178, 150)
STEEL = (170, 182, 200)
PLATE = (90, 200, 190)


def find_ffmpeg() -> str:
    explicit = os.environ.get("FFMPEG")
    if explicit and Path(explicit).exists():
        return explicit
    found = shutil.which("ffmpeg")
    if found:
        return found
    try:
        import imageio_ffmpeg  # type: ignore

        return imageio_ffmpeg.get_ffmpeg_exe()
    except Exception:  # pragma: no cover - optional dependency
        raise SystemExit(
            "ffmpeg not found. Install ffmpeg or `pip install imageio-ffmpeg` and retry."
        )


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    candidates = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold
        else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf" if bold
        else "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
    ]
    for path in candidates:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def background() -> Image.Image:
    img = Image.new("RGB", (SIZE, SIZE), BG_TOP)
    draw = ImageDraw.Draw(img)
    for y in range(SIZE):
        t = y / SIZE
        color = tuple(int(BG_TOP[i] * (1 - t) + BG_BOTTOM[i] * t) for i in range(3))
        draw.line([(0, y), (SIZE, y)], fill=color)
    for x in range(0, SIZE, 60):
        draw.line([(x, 0), (x, SIZE)], fill=GRID, width=1)
    for y in range(0, SIZE, 60):
        draw.line([(0, y), (SIZE, y)], fill=GRID, width=1)
    draw.line([(60, SIZE - 190), (SIZE - 60, SIZE - 190)], fill=GROUND, width=4)
    # subtle vignette
    vignette = Image.new("L", (SIZE, SIZE), 0)
    vdraw = ImageDraw.Draw(vignette)
    vdraw.ellipse([-160, -160, SIZE + 160, SIZE + 160], fill=70)
    img.paste(Image.new("RGB", (SIZE, SIZE), (0, 0, 0)), (0, 0), Image.eval(vignette, lambda v: 90 - v // 2))
    return img


class Skeleton:
    """A tiny 2D articulated figure. All coordinates are in pixels."""

    def __init__(self, draw: ImageDraw.ImageDraw):
        self.draw = draw

    def limb(self, a, b, width=13, color=BODY):
        self.draw.line([a, b], fill=color, width=width)
        r = width // 2
        for point in (a, b):
            self.draw.ellipse([point[0] - r, point[1] - r, point[0] + r, point[1] + r], fill=color)

    def head(self, center, radius=26, color=BODY):
        x, y = center
        self.draw.ellipse([x - radius, y - radius, x + radius, y + radius], fill=color)

    def barbell(self, center, angle_deg=0.0, length=210, plate_r=34):
        x, y = center
        rad = math.radians(angle_deg)
        dx, dy = math.cos(rad) * length / 2, math.sin(rad) * length / 2
        a = (x - dx, y - dy)
        b = (x + dx, y + dy)
        self.draw.line([a, b], fill=STEEL, width=9)
        for end in (a, b):
            self.draw.ellipse(
                [end[0] - plate_r, end[1] - plate_r, end[0] + plate_r, end[1] + plate_r],
                fill=PLATE,
                outline=STEEL,
                width=3,
            )

    def dumbbell(self, center, color=PLATE, size=16):
        x, y = center
        self.draw.line([(x - size, y), (x + size, y)], fill=STEEL, width=8)
        for offset in (-size, size):
            self.draw.ellipse(
                [x + offset - 11, y - 13, x + offset + 11, y + 13], fill=color, outline=STEEL, width=2
            )

    def kettlebell(self, center):
        x, y = center
        self.draw.ellipse([x - 14, y - 22, x + 14, y + 8], fill=PLATE, outline=STEEL, width=2)
        self.draw.arc([x - 16, y - 36, x + 16, y - 4], 200, 340, fill=STEEL, width=6)


def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


def ease(t: float) -> float:
    """Smooth 0..1 ease in/out."""
    return t * t * (3 - 2 * t)


def wave(frame: int, period_frames: int = FRAMES // 2) -> tuple[float, bool]:
    """Returns (0..1 phase, going_up_flag) for a ping-pong animation."""
    phase = (frame % period_frames) / period_frames
    up = phase < 0.5
    triangle = phase * 2 if up else 2 - phase * 2
    return ease(triangle), up


# --------------------------------------------------------------------------------------------
# Pose rendering
# --------------------------------------------------------------------------------------------


def draw_side_figure(
    sk: Skeleton, *, hip, lean_deg, knee_flex, elbow_deg, arm_raise_deg, implement=None,
    hand_offset=None, bar_at_shoulders=False, scale=0.92,
):
    """Side-view figure standing on the ground line with simple joint trigonometry.

    Angles are in degrees: ``arm_raise_deg`` 0 = arm hanging straight down, 90 = horizontal
    forward, 180 = overhead. ``elbow_deg`` 0 = straight arm, 90 = 90-degree bend.
    """
    ground = SIZE - 190
    hip_x, hip_y = hip
    hip_y = ground - (ground - hip_y) * scale
    torso_len = 150 * scale
    lean = math.radians(lean_deg)
    shoulder = (hip_x + math.sin(lean) * torso_len, hip_y - math.cos(lean) * torso_len)
    neck = (shoulder[0] + math.sin(lean) * 12, shoulder[1] - math.cos(lean) * 12)
    head_c = (neck[0] + math.sin(lean) * 26, neck[1] - math.cos(lean) * 26)

    thigh = 120 * scale
    shin = 120 * scale
    knee_angle = math.radians(knee_flex)
    knee = (hip_x + math.sin(knee_angle) * thigh, hip_y + math.cos(knee_angle) * thigh)
    ankle_angle = math.radians(max(-25, knee_flex * 0.35))
    ankle_x = knee[0] - math.sin(knee_angle) * shin * 0.42
    ankle_y = ground
    foot = (ankle_x + 34, ground)

    # Arms
    upper = 74 * scale
    fore = 70 * scale
    raise_rad = math.radians(arm_raise_deg)
    elbow = (
        shoulder[0] + math.sin(raise_rad) * upper,
        shoulder[1] + math.cos(raise_rad) * upper,
    )
    elbow_rad = math.radians(elbow_deg)
    hand = (
        elbow[0] + math.sin(raise_rad + elbow_rad) * fore,
        elbow[1] + math.cos(raise_rad + elbow_rad) * fore,
    )
    if hand_offset:
        hand = (hand[0] + hand_offset[0], hand[1] + hand_offset[1])

    sk.limb((hip_x, hip_y), shoulder, 16)
    sk.limb((hip_x, hip_y), knee, 15)
    sk.limb(knee, (ankle_x, ankle_y), 14)
    sk.limb((ankle_x, ankle_y), foot, 11)
    sk.limb(shoulder, elbow, 12)
    sk.limb(elbow, hand, 11)
    sk.head(head_c, 27)
    # torso accent
    sk.draw.line([(hip_x, hip_y), shoulder], fill=ACCENT_SOFT, width=4)

    if implement == "barbell" and bar_at_shoulders:
        # Bar sits across the upper back, hands just outside the shoulders.
        bar_y = shoulder[1] - 4
        sk.barbell((hip_x + 18, bar_y), angle_deg=0, length=230)
    elif implement == "barbell":
        sk.barbell((hand[0], hand[1] - 4), angle_deg=0)
    elif implement == "dumbbell":
        sk.dumbbell((hand[0], hand[1]))
    elif implement == "kettlebell":
        sk.kettlebell((hand[0], hand[1] + 6))
    return shoulder, hip, hand, knee, (ankle_x, ankle_y)


def draw_lying_figure(sk: Skeleton, *, press: float, implement="barbell"):
    """Bench press (side view): torso horizontal, arms pressing up."""
    ground = SIZE - 190
    bench_y = ground - 110
    hip = (250, bench_y)
    shoulder = (410, bench_y - 6)
    head_c = (452, bench_y - 12)
    knee = (206, bench_y + 26)
    ankle = (250, ground)
    sk.draw.rounded_rectangle([300, bench_y + 14, 470, bench_y + 34], radius=8, fill=(46, 52, 64))
    sk.draw.rectangle([352, bench_y + 34, 368, ground], fill=(46, 52, 64))
    sk.draw.rectangle([404, bench_y + 34, 420, ground], fill=(46, 52, 64))
    sk.limb(hip, shoulder, 16)
    sk.limb((hip[0] - 6, hip[1] + 4), knee, 15)
    sk.limb(knee, ankle, 14)
    sk.limb((ankle[0] - 34, ground), ankle, 11)
    sk.head(head_c, 26)

    elbow_lift = lerp(28, 92, press)
    elbow = (shoulder[0] + 6, shoulder[1] - elbow_lift)
    hand = (shoulder[0] + 2, shoulder[1] - elbow_lift - 56)
    sk.limb(shoulder, elbow, 12)
    sk.limb(elbow, hand, 11)
    if implement == "barbell":
        sk.barbell((hand[0], hand[1] - 6))
    else:
        sk.dumbbell((hand[0], hand[1]))


def draw_hanging_figure(sk: Skeleton, *, pull: float):
    """Pull-up (side/front hybrid): hands fixed on the bar, body rises."""
    bar_y = 150
    sk.draw.line([(200, bar_y), (520, bar_y)], fill=STEEL, width=10)
    sk.draw.line([(200, bar_y - 40), (200, bar_y + 8)], fill=(70, 78, 92), width=8)
    sk.draw.line([(520, bar_y - 40), (520, bar_y + 8)], fill=(70, 78, 92), width=8)

    hand_y = bar_y + 6
    body_rise = lerp(0, 92, pull)
    shoulder = (360, hand_y + 74 - body_rise)
    head_c = (360, shoulder[1] - 34)
    hip = (360, shoulder[1] + 150)
    knee = (360, hip[1] + 108)
    ankle = (360, knee[1] + 104)

    sk.limb(shoulder, hip, 16)
    sk.limb(hip, knee, 15)
    sk.limb(knee, ankle, 14)
    sk.limb((296, hand_y), shoulder, 12)
    sk.limb((424, hand_y), shoulder, 12)
    sk.head(head_c, 26)
    sk.draw.line([shoulder, hip], fill=ACCENT_SOFT, width=5)


def draw_floor_figure(sk: Skeleton, *, lower: float):
    """Floor core work: legs lowering from a tabletop position."""
    ground = SIZE - 190
    hip = (330, ground - 40)
    shoulder = (200, ground - 34)
    head_c = (160, ground - 34)
    sk.draw.rounded_rectangle([100, ground - 10, 480, ground], radius=6, fill=(46, 52, 64))
    sk.limb(shoulder, hip, 16)
    sk.head(head_c, 26)
    thigh_angle = lerp(-70, -8, lower)
    thigh = 96
    knee = (hip[0] + math.cos(math.radians(thigh_angle)) * thigh, hip[1] + math.sin(math.radians(thigh_angle)) * thigh)
    shin = 92
    ankle = (knee[0] + math.cos(math.radians(thigh_angle + lerp(60, 6, lower))) * shin,
             knee[1] + math.sin(math.radians(thigh_angle + lerp(60, 6, lower))) * shin)
    sk.limb(hip, knee, 14)
    sk.limb(knee, ankle, 13)
    # arms tucked
    sk.limb(shoulder, (shoulder[0] + 40, shoulder[1] + 60), 11)


def draw_front_arms(sk: Skeleton, *, raise_amount: float):
    """Front view: lateral raise (arms out to the sides)."""
    ground = SIZE - 190
    hip = (360, ground - 260)
    shoulder = (360, ground - 400)
    head_c = (360, ground - 452)
    sk.limb(hip, shoulder, 18)
    sk.limb((340, hip[1]), (330, ground), 15)
    sk.limb((380, hip[1]), (390, ground), 15)
    sk.head(head_c, 30)
    for side in (-1, 1):
        angle = math.radians(lerp(6, 84, raise_amount))
        elbow = (shoulder[0] + side * math.sin(angle) * 70, shoulder[1] + math.cos(angle) * 70)
        hand = (shoulder[0] + side * math.sin(angle) * 146, shoulder[1] + math.cos(angle) * 146)
        sk.limb(shoulder, elbow, 12)
        sk.limb(elbow, hand, 11)
        sk.dumbbell((hand[0], hand[1] + 6))


# --------------------------------------------------------------------------------------------
# Pattern definitions
# --------------------------------------------------------------------------------------------

PATTERNS = {
    "HORIZONTAL_PUSH": ("Bench press", "PRESS", "dumbbell"),
    "VERTICAL_PUSH": ("Overhead press", "PRESS", "barbell"),
    "HORIZONTAL_PULL": ("Row", "PULL", "dumbbell"),
    "VERTICAL_PULL": ("Pull-up", "PULL", None),
    "SQUAT": ("Squat", "LOWER", "barbell"),
    "HINGE": ("Hinge", "LOWER", "barbell"),
    "LUNGE": ("Lunge", "LOWER", "dumbbell"),
    "LEG_ISOLATION": ("Leg isolation", "CURL", None),
    "BICEPS_ISOLATION": ("Biceps curl", "CURL", "dumbbell"),
    "TRICEPS_ISOLATION": ("Triceps extension", "EXTEND", "cable"),
    "SHOULDER_ISOLATION": ("Lateral raise", "RAISE", "dumbbell"),
    "CALVES": ("Calf raise", "RAISE", None),
    "CORE": ("Core", "HOLD", None),
    "CONDITIONING": ("Conditioning", "CARDIO", None),
}


def render_pattern_frame(pattern: str, frame: int, size: int = SIZE, for_thumb: bool = False) -> Image.Image:
    img = background() if not for_thumb else _thumb_background(size)
    draw = ImageDraw.Draw(img)
    sk = Skeleton(draw)

    if size != SIZE:
        # scale factor for thumbnail rendering
        factor = size / SIZE
        sk.draw = draw
    else:
        factor = 1.0

    t, up = wave(frame)
    title, phase, implement = PATTERNS[pattern]

    if pattern == "HORIZONTAL_PUSH":
        draw_lying_figure(sk, press=t, implement="barbell")
    elif pattern == "VERTICAL_PUSH":
        draw_side_figure(
            sk, hip=(360, SIZE - 96 - 300), lean_deg=lerp(1, -7, t), knee_flex=6,
            elbow_deg=lerp(95, 4, t), arm_raise_deg=lerp(84, 172, t), implement="barbell",
        )
    elif pattern == "HORIZONTAL_PULL":
        draw_side_figure(
            sk, hip=(330, SIZE - 96 - 262), lean_deg=lerp(52, 44, t), knee_flex=24,
            elbow_deg=lerp(4, 128, t), arm_raise_deg=lerp(2, -34, t), implement="dumbbell",
        )
    elif pattern == "VERTICAL_PULL":
        draw_hanging_figure(sk, pull=t)
    elif pattern == "SQUAT":
        draw_side_figure(
            sk, hip=(360, lerp(SIZE - 96 - 300, SIZE - 96 - 214, t)), lean_deg=lerp(3, 26, t),
            knee_flex=lerp(4, 56, t), elbow_deg=lerp(128, 118, t), arm_raise_deg=lerp(96, 88, t),
            implement="barbell", bar_at_shoulders=True,
        )
    elif pattern == "HINGE":
        draw_side_figure(
            sk, hip=(lerp(360, 296, t), lerp(SIZE - 96 - 300, SIZE - 96 - 274, t)),
            lean_deg=lerp(4, 66, t), knee_flex=lerp(6, 20, t), elbow_deg=4,
            arm_raise_deg=0, implement="barbell",
        )
    elif pattern == "LUNGE":
        draw_side_figure(
            sk, hip=(lerp(360, 338, t), lerp(SIZE - 96 - 292, SIZE - 96 - 232, t)),
            lean_deg=7, knee_flex=lerp(8, 46, t), elbow_deg=4, arm_raise_deg=0,
            implement="dumbbell",
        )
    elif pattern == "LEG_ISOLATION":
        draw_side_figure(
            sk, hip=(344, SIZE - 96 - 286), lean_deg=10, knee_flex=lerp(8, 64, t),
            elbow_deg=lerp(60, 116, t), arm_raise_deg=88,
        )
    elif pattern == "CALVES":
        draw_side_figure(
            sk, hip=(360, SIZE - 96 - 300 + (13 * t)), lean_deg=0, knee_flex=4,
            elbow_deg=4, arm_raise_deg=0,
        )
    elif pattern == "BICEPS_ISOLATION":
        draw_side_figure(
            sk, hip=(360, SIZE - 96 - 300), lean_deg=1, knee_flex=4,
            elbow_deg=lerp(4, 132, t), arm_raise_deg=2, implement="dumbbell",
        )
    elif pattern == "TRICEPS_ISOLATION":
        draw_side_figure(
            sk, hip=(360, SIZE - 96 - 300), lean_deg=4, knee_flex=6,
            elbow_deg=lerp(116, 6, t), arm_raise_deg=6,
        )
    elif pattern == "SHOULDER_ISOLATION":
        draw_front_arms(sk, raise_amount=t)
    elif pattern == "CORE":
        draw_floor_figure(sk, lower=t)
    elif pattern == "CONDITIONING":
        knee_alt = 18 + 42 * t
        draw_side_figure(
            sk, hip=(360, SIZE - 190 - 286 + 10 * t), lean_deg=12, knee_flex=knee_alt,
            elbow_deg=lerp(118, 92, t), arm_raise_deg=88,
        )

    if not for_thumb:
        overlay = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
        odraw = ImageDraw.Draw(overlay)
        bar_top = SIZE - 150
        odraw.rectangle([0, bar_top, SIZE, SIZE], fill=(10, 12, 16, 214))
        odraw.line([(0, bar_top), (SIZE, bar_top)], fill=(44, 50, 62, 255), width=2)
        img = Image.alpha_composite(img.convert("RGBA"), overlay).convert("RGB")
        draw = ImageDraw.Draw(img)

        draw.text((60, bar_top + 22), title.upper(), font=font(34, bold=True), fill=BODY)
        small = font(19)
        draw.text((60, bar_top + 70), "TrainWithMe · sample demo clip", font=small, fill=BODY_DIM)

        chip = phase
        bbox = draw.textbbox((0, 0), chip, font=small)
        chip_w = bbox[2] - bbox[0] + 34
        draw.rounded_rectangle(
            [SIZE - 60 - chip_w, bar_top + 24, SIZE - 60, bar_top + 62],
            radius=19, fill=(30, 35, 45), outline=(52, 60, 74),
        )
        draw.text((SIZE - 60 - chip_w + 17, bar_top + 33), chip, font=small, fill=ACCENT)
        for i in range(3):
            x = SIZE - 78 - i * 26
            active = abs(t - i / 2) < 0.34
            draw.ellipse(
                [x - 7, bar_top + 76, x + 7, bar_top + 90],
                fill=ACCENT if active else (44, 50, 62),
            )
        draw.text((60, bar_top + 108), "Placeholder art — swap in licensed footage", font=font(16), fill=(104, 113, 130))
    return img


def _thumb_background(size: int) -> Image.Image:
    img = Image.new("RGB", (size, size), (24, 28, 35))
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle([0, 0, size - 1, size - 1], radius=int(size * 0.09), fill=(24, 28, 35))
    draw.ellipse([size * 0.12, size * 0.12, size * 0.88, size * 0.88], outline=(34, 40, 50), width=2)
    return img


def write_video(pattern: str, ffmpeg: str, out_dir: Path) -> Path:
    out = out_dir / f"{pattern.lower()}.mp4"
    with tempfile.TemporaryDirectory() as tmp:
        tmp_path = Path(tmp)
        for i in range(FRAMES):
            render_pattern_frame(pattern, i).save(tmp_path / f"f{i:04d}.png")
        cmd = [
            ffmpeg, "-y", "-loglevel", "error",
            "-framerate", str(FPS),
            "-i", str(tmp_path / "f%04d.png"),
            "-c:v", "libx264", "-profile:v", "main", "-level", "3.1",
            "-pix_fmt", "yuv420p", "-movflags", "+faststart",
            "-crf", "30", "-preset", "veryfast",
            "-an", str(out),
        ]
        subprocess.run(cmd, check=True)
    return out


def write_thumbnails(ffmpeg: str) -> None:
    catalogue = json.loads(CATALOGUE.read_text())
    THUMB_DIR.mkdir(parents=True, exist_ok=True)
    for exercise in catalogue:
        img = render_pattern_frame(exercise["pattern"], FRAMES // 8, for_thumb=True)
        img = img.resize((THUMB_SIZE, THUMB_SIZE), Image.LANCZOS)
        draw = ImageDraw.Draw(img)
        draw.rounded_rectangle([0, 0, THUMB_SIZE - 1, THUMB_SIZE - 1], radius=28, outline=(40, 46, 58), width=2)
        img.save(THUMB_DIR / f"{exercise['id']}.jpg", quality=82, optimize=True)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--only", help="render a single pattern, e.g. SQUAT")
    parser.add_argument("--no-thumbs", action="store_true", help="skip list thumbnails")
    args = parser.parse_args()

    ffmpeg = find_ffmpeg()
    VIDEO_DIR.mkdir(parents=True, exist_ok=True)
    patterns = [args.only] if args.only else list(PATTERNS)
    for pattern in patterns:
        path = write_video(pattern, ffmpeg, VIDEO_DIR)
        print(f"{pattern:22s} -> {path.relative_to(ROOT)} ({path.stat().st_size // 1024} KB)")
    if not args.no_thumbs:
        write_thumbnails(ffmpeg)
        print(f"thumbnails -> {THUMB_DIR.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
