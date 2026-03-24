#!/usr/bin/env python

import subprocess
from pathlib import Path

import typer
import yaml
from jinja2 import Template
from dotenv import load_dotenv
import os

app = typer.Typer(invoke_without_command=True)

# Base paths (independent of cwd)
BASE_DIR = Path(__file__).resolve().parent
DATA_FILE = BASE_DIR / "web-data.yaml"
TEMPLATE_FILE = BASE_DIR / "web-template.html"
OUTPUT_DIR = BASE_DIR / "web"


def load_data():
    if not DATA_FILE.exists():
        typer.echo(f"❌ Missing {DATA_FILE}")
        raise typer.Exit(1)

    with open(DATA_FILE) as f:
        return yaml.safe_load(f)


def load_template():
    if not TEMPLATE_FILE.exists():
        typer.echo(f"❌ Missing {TEMPLATE_FILE}")
        raise typer.Exit(1)

    with open(TEMPLATE_FILE) as t:
        return Template(t.read())

def translate_data(data: dict, lang: str = "en", fallback_lang: str = "en") -> dict:
    """
    Recursively traverse the YAML data.
    - If a value is a dict with 'en', 'es', etc., pick the correct language
    - Otherwise, keep the value as-is
    """
    FALLBACK_LANG = "en"
    if isinstance(data, dict):
        # Translation
        if fallback_lang in data:
            return data.get(lang, data[fallback_lang])
        # Not a translation, recurse into dict
        return {
            key: translate_data(value, lang, fallback_lang)
            for key, value in data.items()
        }
    if isinstance(data, list):
        # recurse into lists
        return [translate_data(item, lang, fallback_lang) for item in data]
    # base case: primitive value
    return data

def detect_languages(data, fallback_lang="en") -> set:
    """
    Recursively detect all language keys in a YAML-like structure.
    - If a dict has fallback_lang, treat it as a language dict and collect all keys.
    - Works recursively with lists and nested dicts.
    """
    # Early exit: primitive
    if not isinstance(data, (dict, list)):
        return set()

    # List: recurse into each item
    if isinstance(data, list):
        langs = set()
        for item in data:
            langs |= detect_languages(item, fallback_lang)
        return langs

    # Dict: check if it's a language dict
    if fallback_lang in data:
        return set(data.keys())

    # Normal dict: recurse on values
    langs = set()
    for value in data.values():
        langs |= detect_languages(value, fallback_lang)
    return langs

@app.callback()
def main(ctx: typer.Context):
    if ctx.invoked_subcommand is None:
        typer.echo(ctx.get_help())
        raise typer.Exit()

@app.command()
def build():
    """Generate the static site"""
    data = load_data()
    template = load_template()

    langs = detect_languages(data, fallback_lang='en')
    for lang in langs:
        translated_data = translate_data(data, lang)
        html_output = template.render(**translated_data)
        lang_dir = OUTPUT_DIR / lang

        lang_dir.mkdir(parents=True, exist_ok=True)
        output_file = lang_dir / "index.html"

        with open(output_file, "w") as out:
            out.write(html_output)

        typer.echo(f"✅ Generated: {output_file}")


@app.command()
def publish():
    """Publish via rsync over SSH"""
    load_dotenv()

    remote = os.getenv("REMOTE_PATH")

    if not remote:
        typer.echo("❌ REMOTE_PATH not set in .env")
        raise typer.Exit(1)

    if not OUTPUT_DIR.exists():
        typer.echo("❌ Nothing to publish. Run build first.")
        raise typer.Exit(1)

    cmd = [
        "rsync",
        "-avz",
        "--delete",
        f"{OUTPUT_DIR}/",
        remote,
    ]

    typer.echo(f"🚀 Publishing to {remote}...")

    try:
        subprocess.run(cmd, check=True)
    except subprocess.CalledProcessError:
        typer.echo("❌ Publish failed")
        raise typer.Exit(1)

    typer.echo("✅ Publish complete")


if __name__ == "__main__":
    app()
