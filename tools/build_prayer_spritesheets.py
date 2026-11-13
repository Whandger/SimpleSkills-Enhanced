from pathlib import Path
import json
from PIL import Image

root = Path("src/main/resources/assets/simpleskills/textures")
for name in ("divine_healing", "lightning_bolt", "meteor"):
    source = Image.open(root / "effects/prayer" / f"{name}_sheet.png").convert("RGBA")
    cell_w, cell_h = source.width // 4, source.height // 4
    size = min(cell_w, cell_h)
    frames = []
    for row in range(4):
        for col in range(4):
            x = col * cell_w + (cell_w - size) // 2
            y = row * cell_h + (cell_h - size) // 2
            frames.append(source.crop((x, y, x + size, y + size)).resize((256, 256), Image.Resampling.LANCZOS))
    strip = Image.new("RGBA", (256, 256 * len(frames)))
    for index, frame in enumerate(frames):
        strip.alpha_composite(frame, (0, index * 256))
    target = root / "particle" / f"prayer_{name}.png"
    strip.save(target)
    (target.with_suffix(".png.mcmeta")).write_text(json.dumps({
        "animation": {"width": 256, "height": 256, "frametime": 2, "interpolate": True,
                      "frames": list(range(len(frames)))}
    }), encoding="utf-8")
    print(target, source.size, "->", strip.size)
