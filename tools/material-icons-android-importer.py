#!/usr/bin/env python3


# sudo apt install librsvg2-bin libxml2-utils build-essential
# pip install typer[all] requests lxml svgpathtools rich yamlns

import requests
from pathlib import Path
from xml.etree import ElementTree as ET
from typing import Optional
from yamlns import ns
from rich import print
from rich.columns import Columns
import typer

app = typer.Typer()

DATA_JSON_URL = "https://material-icons.github.io/material-icons/data.json"
VALID_VARIANTS = ["baseline", "outlined", "sharp", "round"]


def download_raw(url: str) -> Optional[bytes]:
    print(url)
    response = requests.get(url)
    if response.status_code == 404:
        return None
    response.raise_for_status()
    return response.content

def load_collection_metadata():
    response = requests.get(DATA_JSON_URL)
    response.raise_for_status()
    return ns(response.json())


def find_icon_metadata(metadata, icon_name: str):
    icon_metadata = [
        icon for icon in metadata.icons
        if icon["name"] == icon_name
    ]
    if not icon_metadata:
        print(Columns([o["name"] for o in metadata.icons]))
        print(f"Icon '{icon_name}' not found in metadata")
        raise typer.Exit(code=-1)
    return ns(icon_metadata[0])


def svg_to_vector_drawable(svg_data: bytes) -> str:
    tree = ET.ElementTree(ET.fromstring(svg_data))
    root = tree.getroot()

    vd_root = ET.Element("vector", {
        "xmlns:android": "http://schemas.android.com/apk/res/android",
        "android:width": "24dp",
        "android:height": "24dp",
        "android:viewportWidth": root.attrib.get("width", "24"),
        "android:viewportHeight": root.attrib.get("height", "24")
    })

    for path in root.findall("{http://www.w3.org/2000/svg}path"):
        d = path.attrib.get("d")
        if d:
            ET.SubElement(vd_root, "path", {
                "android:fillColor": "#000000",
                "android:pathData": d,
            })

    return ET.tostring(vd_root, encoding="unicode")


def target_path(icon_name: str, flavor: str = 'main', project_root: Path = Path()) -> Path:
    return project_root / "app/src" / flavor / "res/drawable" / f"ic_{icon_name}.xml"


@app.command()
def fetch_icon(
    icon_name: str = typer.Argument(
        ...,
        help="Material icon name (e.g., 'settings')"
    ),
    variant: str = typer.Argument(
        "baseline",
        help="Icon style variant",
        show_default=True,
        case_sensitive=False,
        autocompletion=lambda: VALID_VARIANTS,
    ),
    flavor: str = typer.Option(
        "main",
        help="Android source set flavor",
    ),
    project_root: Path = typer.Option(
        Path("."),
        file_okay=False,
        exists=True,
        help="Android project root directory"
    ),
):
    """
    Fecth a material icon,
    converts to Vector Drawable (Android's format),
    and place it into the proper path in the project,
    so that they are available as "@drawable/ic_{name}"
    """
    if variant not in VALID_VARIANTS:
        raise typer.BadParameter(
            f"Invalid variant '{variant}'. Must be one of: {', '.join(VALID_VARIANTS)}"
        )

    metadata = load_collection_metadata()
    icon_metadata = find_icon_metadata(metadata, icon_name)

    icon_url = metadata.root + metadata.asset_url_pattern.format(
        icon=icon_name,
        family=variant
    )
    svg_content = download_raw(icon_url)

    if not svg_content:
        print(f"[yellow]Variant '{variant}' not found for icon '{icon_name}'. Trying default 'baseline'...[/yellow]")
        fallback_url = metadata.root + metadata.asset_url_pattern.format(icon=icon_name, family="baseline")
        svg_content = download_raw(fallback_url)
        if not svg_content:
            available = ", ".join(icon_metadata.get("unsupported_families", []))
            raise typer.Exit(
                code=1,
                message=f"[red]Icon '{icon_name}' not available in any variant. Supported variants might be: {available}[/red]"
            )

    print(f"[green]✔ Downloaded SVG for {icon_name} ({variant})[/green]")

    vector_drawable = svg_to_vector_drawable(svg_content)

    target = target_path(icon_name, flavor, project_root)
    if not (project_root / 'app/src').is_dir():
        print(f"[yellow]Project not detected in {project_root}. Should contain at least app/src. Saving it in current directory.[/yellow]")
        target = Path(target.name)


    print(f"[blue]→ Writing to {target}[/blue]")
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(vector_drawable)

    print("[bold green]✓ Done![/bold green]")


if __name__ == "__main__":
    app()
