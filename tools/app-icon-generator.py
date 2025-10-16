import typer
from pathlib import Path
from PIL import Image
import cairosvg
import xml.etree.ElementTree as ET
from typing import Tuple

app = typer.Typer()

DENSITY_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

def write_file(file, content):
    typer.echo(f"📝 Generating {file}...")
    Path(file).write_text(content)


def extract_svg_data(svg_path: Path) -> tuple[str, float, float]:
    ns = {"svg": "http://www.w3.org/2000/svg"}
    ET.register_namespace("", ns["svg"])
    tree = ET.parse(svg_path)
    root = tree.getroot()

    viewBox = root.attrib.get("viewBox", "0 0 24 24")
    try:
        _, _, w, h = map(float, viewBox.strip().split())
    except Exception:
        typer.echo("⚠️  viewBox inválido. Usando 24x24 por defecto.")
        w, h = 24, 24

    paths = root.findall(".//svg:path", ns)
    if not paths:
        raise RuntimeError("No se encontró ningún <path> en el SVG.")

    path_data = " ".join([p.attrib["d"] for p in paths if "d" in p.attrib])
    return path_data, w, h

def material_icon_to_launcher_svg(
    path_d: str,
    original_viewbox_size: Tuple[float, float],
    foreground_color: str,
    background_color: str,
    round: bool=False,
) -> str:
    original_width, original_height = original_viewbox_size
    assert original_width == original_height, "Unexpected non-squared icon, requires considering the max direction and extra-pad the shorter direction, but not implemented."

    material_size = 24
    material_safe_zone = 20
    material_pad = 2 # (material_size - material_safe_zone) / 2
    launcher_size = 108
    launcher_safe_zone = 66
    launcher_pad = 21 # (launcher_size - launcher_safe_zone) / 2
    mapped_material_pad = 9 # material_pad * launcher_size / material_size
    translate = 12 # launcher_pad - mapped_material_pad

    actual_safe_zone_size = material_safe_zone * original_width / material_size

    scale = launcher_safe_zone / actual_safe_zone_size
    bg_shape = (
        f"""<circle cx="{launcher_size/2}" cy="{launcher_size/2}" r="{launcher_size/2}" fill="{background_color}" />"""
        if round else
        f"""<rect width="{launcher_size}" height="{launcher_size}" fill="{background_color}" rx="12" />"""
    )

    return f"""<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg"
     width="{launcher_size}" height="{launcher_size}" viewBox="0 0 {launcher_size} {launcher_size}">
  {bg_shape}
  <g transform="translate({translate:.3f}, {translate:.3f}) scale({scale:.6f})"
     fill="{foreground_color}">
    <path d="{path_d}" />
  </g>
</svg>
"""



def rasterize_svg_to_png(svg_content: str, size: int, output: Path):
    return cairosvg.svg2png(
        bytestring=svg_content.encode('utf-8'),
        write_to=str(output),
        output_width=size,
        output_height=size
    )

@app.command()
def launcher_icon(
    svg_path: Path = typer.Argument(..., help="Ruta al archivo SVG del ícono (foreground)"),
    background_color: str = typer.Option("#d69999", "--bg", help="Color de fondo (ej: '#d69999')"),
    foreground_color: str = typer.Option("#712b5e", "--fg", help="Color del icono (ej: '#712b5e')"),
    output_dir: Path = typer.Option(Path("res"), "--out", help="Directorio de salida para recursos"),
    padding: int = typer.Option(0, help="Padding interno en píxeles"),
    themed: bool = typer.Option(True, help="¿Incluir ic_launcher_themed.xml para Android 12+?"),
    minsdk: int = typer.Option(24, help="minSdkVersion de la app"),
):
    """
    Generates adaptative icons for Android from a material icon svg.
    """
    if not svg_path.exists():
        typer.echo(f"❌ No se encontró el archivo: {svg_path}")
        raise typer.Exit()

    output_dir.mkdir(parents=True, exist_ok=True)

    path_data, viewport_w, viewport_h = extract_svg_data(svg_path)
    svg = material_icon_to_launcher_svg(
        path_d=path_data,
        original_viewbox_size=(viewport_w, viewport_h),
        foreground_color=foreground_color,
        background_color=background_color,
    )
    svg_round = material_icon_to_launcher_svg(
        path_d=path_data,
        original_viewbox_size=(viewport_w, viewport_h),
        foreground_color=foreground_color,
        background_color=background_color,
        round=True,
    )

    for density, size in DENSITY_SIZES.items():
        mipmap_dir = output_dir / f"mipmap-{density}"

        squared = mipmap_dir / "ic_launcher.png"
        typer.echo(f"📝 Generating {squared}...")
        mipmap_dir.mkdir(parents=True, exist_ok=True)
        rasterize_svg_to_png(
            svg_content=svg,
            size=size,
            output=squared,
        )

        round = mipmap_dir / "ic_launcher_round.png"
        typer.echo(f"📝 Generating {round}...")
        rasterize_svg_to_png(
            svg_content=svg_round,
            size=size,
            output=round,
        )


    # 2. Exportar foreground como VectorDrawable
    typer.echo("📝 Generando ic_launcher_foreground.xml desde SVG...")

    v24suffix = '-v24' if minsdk < 26 else ''
    drawable_dir = output_dir / ("drawable-anydpi" + v24suffix)
    drawable_dir.mkdir(parents=True, exist_ok=True)

    def write_drawable(name, foreground_color):
        write_file(drawable_dir / f"ic_launcher_{name}.xml", f"""\
        <?xml version="1.0" encoding="utf-8"?>
        <vector xmlns:android="http://schemas.android.com/apk/res/android"
        android:width="108dp"
        android:height="108dp"
        android:viewportWidth="48"
        android:viewportHeight="48">

        <group
            android:translateX="12"
            android:translateY="12">
            <path
                android:fillColor="{foreground_color}"
                android:pathData="{path_data}" />
        </group>
        </vector>""".strip())

    write_drawable("foreground", foreground_color)
    write_drawable("monochrome", "#000000")

    v26suffix = '-v26' if minsdk < 26 else ''
    adaptive_dir = output_dir / ("mipmap-anydpi" + v26suffix)
    adaptive_dir.mkdir(parents=True, exist_ok=True)

    write_file(adaptive_dir / "ic_launcher_background.xml", f"""\
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <corners android:radius="12dp"/>
    <solid android:color="{background_color}" />
</shape>""".strip())

    # 4. Adaptive icon XMLs
    def write_adaptive_icon(name: str, bg: str, fg: str):
        write_file(adaptive_dir / f"{name}.xml", f"""
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/{bg}"/>
    <foreground android:drawable="@drawable/{fg}"/>
    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>
</adaptive-icon>""".strip())

    write_adaptive_icon("ic_launcher", "ic_launcher_background", "ic_launcher_foreground")
    write_adaptive_icon("ic_launcher_round", "ic_launcher_background", "ic_launcher_foreground")

    if themed:
        typer.echo("🎨 Generando ic_launcher_themed.xml...")
        write_file(adaptive_dir / "ic_launcher_themed.xml", f"""
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="?android:attr/colorBackground"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>
</adaptive-icon>""".strip())

    typer.echo("✅ Generación completa.")

@app.command()
def splash(
    app_name: str = typer.Argument(..., help="Application name"),
    svg_path: Path = typer.Argument(..., help="Material icon svg file (foreground)"),
    background_color: str = typer.Option("#d69999", "--bg", help="Background color"),
    foreground_color: str = typer.Option("#712b5e", "--fg", help="Foreground color"),
    motto: str = typer.Option("Fill up cheaper, closer", "--motto", help="Motto under the name"),
    output: Path = typer.Option("featureGraphic.svg", "--out", help="Output svg"),
):
    """
    Generate a simple featureGraphic.svg for Google Play / F-Droid (1024x500)
    """
    if not svg_path.exists():
        typer.echo(f"❌ No se encontró el archivo: {svg_path}")
        raise typer.Exit()

    # Reuse your existing SVG parser
    path_data, viewport_w, viewport_h = extract_svg_data(svg_path)

    # Build SVG content
    svg_content = f"""<svg width="1024" height="500" viewBox="0 0 1024 500"
     xmlns="http://www.w3.org/2000/svg">
  <!-- Background -->
  <rect width="1024" height="500" fill="{background_color}" />

  <!-- Icon: large, on the left -->
  <path
    d="{path_data}"
    fill="{foreground_color}"
    transform="translate(70, 140) scale(8)"
  />

  <!-- App Name -->
  <text x="310" y="220"
        font-family="Google Sans, Helvetica Neue, sans-serif"
        font-size="64"
        font-weight="700"
        fill="{foreground_color}"
        text-anchor="start">
    {app_name}
  </text>

  <!-- Version -->
  <text x="660" y="220"
        id="version"
        font-family="Google Sans, Helvetica Neue, sans-serif"
        font-size="52"
        font-weight="500"
        fill="#f0e0dd"
        text-anchor="start">
    1.0.1
  </text>

  <!-- Motto -->
  <text x="310" y="290"
        id="motto"
        font-family="Google Sans, Helvetica Neue, sans-serif"
        font-size="36"
        fill="{foreground_color}"
        fill-opacity="0.7"
        text-anchor="start">
    {motto}
  </text>
</svg>"""


    write_file(output, svg_content)
    typer.echo(f"✅ Generated: {output}")


if __name__ == "__main__":
    app()
