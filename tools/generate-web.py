#!/usr/bin/env python

import subprocess
from pathlib import Path
import typer
import yaml
from jinja2 import Template
from dotenv import load_dotenv
import os
import http.server
import socketserver
import time
from threading import Timer
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler

app = typer.Typer(invoke_without_command=True)


def resolve_paths(data_file: Path):
    base_dir = data_file.resolve().parent
    return {
        "BASE_DIR": base_dir,
        "DATA_FILE": data_file,
        "TEMPLATE_FILE": base_dir / "web-template.html",
        "OUTPUT_DIR": base_dir / "web",
        "TRANSLATIONS_DIR": base_dir / "web-translations",
    }


class RebuildHandler(FileSystemEventHandler):
    def __init__(self, build_fn, watched_paths, debounce=1.5):
        self.build_fn = build_fn
        self.watched_paths = [str(p.resolve()) for p in watched_paths]
        self.debounce = debounce
        self.last_run = 0
        self.timer = None

    def _is_relevant(self, path: str) -> bool:
        path = str(Path(path).resolve())
        return any(path.startswith(w) for w in self.watched_paths)

    def on_modified(self, event):
        self.process_event(event)

    def on_deleted(self, event):
        self.process_event(event)

    def on_created(self, event):
        self.process_event(event)

    def process_event(self, event):
        if event.is_directory:
            return

        if not self._is_relevant(event.src_path):
            return

        if not event.src_path.endswith((".yaml", ".html")):
            return

        if self.timer:
            self.timer.cancel()

        typer.echo(f"📁 {event.event_type}: {event.src_path}")
        self.timer = Timer(self.debounce, self._trigger_build)
        self.timer.start()

    def _trigger_build(self):
        self.timer = None
        typer.echo("🔄 Change detected, rebuilding...")
        try:
            self.build_fn()
        except Exception as e:
            typer.echo(f"❌ Build failed: {e}")

def serve_directory(directory: Path, port: int):
    handler = http.server.SimpleHTTPRequestHandler

    os.chdir(directory)

    with socketserver.TCPServer(("", port), handler) as httpd:
        typer.echo(f"🌍 Serving at http://localhost:{port}")
        httpd.serve_forever()

@app.command()
def serve(
    port: int = 8000,
    data_file: Path = typer.Argument(Path("./web-data.yaml"))
):
    """Serve the site locally and rebuild on changes"""

    paths = resolve_paths(data_file)

    def _build():
        build(data_file=data_file)

    # Initial build
    _build()

    watched_paths = [
        paths["DATA_FILE"],
        paths["TEMPLATE_FILE"],
        paths["TRANSLATIONS_DIR"],
    ]

    # Start watcher
    event_handler = RebuildHandler(_build, watched_paths)
    observer = Observer()

    observer.schedule(event_handler, str(paths["DATA_FILE"]))
    observer.schedule(event_handler, str(paths["TEMPLATE_FILE"]))
    observer.schedule(event_handler, str(paths["TRANSLATIONS_DIR"]), recursive=True)
    observer.start()

    typer.echo("👀 Watching for changes...")

    try:
        serve_directory(paths["OUTPUT_DIR"], port)
    except KeyboardInterrupt:
        typer.echo("\n🛑 Stopping...")
        observer.stop()

    observer.join()


def load_data(data_file):
    if not data_file.exists():
        typer.echo(f"❌ Missing {data_file}")
        raise typer.Exit(1)

    with open(data_file) as f:
        return yaml.safe_load(f)


def load_template(template_file):
    if not template_file.exists():
        typer.echo(f"❌ Missing {template_file}")
        raise typer.Exit(1)

    with open(template_file) as t:
        return Template(t.read())

def load_translations(translations_dir):
    translations = {}
    for lang_file in translations_dir.glob("*.yaml"):
        typer.echo(f"🌐 Loading translation {lang_file}")
        lang = lang_file.stem
        with open(lang_file) as f:
            translations[lang] = yaml.safe_load(f)
    return translations


def apply_translations(data, translations, fallback_lang):
    """
    Recursively traverse the data.
    - If a value is a string in all uppercase and exists in external translations,
      replace it with a dict of all languages.
    - Otherwise, keep the value as-is.
    """
    # Recurse for dicts
    if isinstance(data, dict):
        return {
            k: apply_translations(v, translations, fallback_lang)
           for k, v in data.items()
        }

    # Recurse for lists
    if isinstance(data, list):
        return [
            apply_translations(item, translations, fallback_lang)
            for item in data
        ]

    if not isinstance(data, str): return data
    if not data.isupper(): return data
    if data not in translations.get('en', {}): return data

    return {
        lang: ext.get(data, data)
        for lang, ext in translations.items()
    }

def translate_data(data: dict, lang: str, fallback_lang: str) -> dict:
    """
    Recursively traverse the YAML data.
    - If a value is a dict with 'en', 'es', etc., pick the correct language
    - Otherwise, keep the value as-is
    """
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

def detect_languages(data: dict, fallback_lang: str) -> set:
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
def build(
    data_file: Path = typer.Argument(Path("./web-data.yaml"))
):
    """Generate the static site"""
    paths = resolve_paths(data_file)

    translations = load_translations(paths["TRANSLATIONS_DIR"])
    data = load_data(paths["DATA_FILE"])
    fallback_lang = data.get("fallback_language", "en")
    data = apply_translations(data, translations, fallback_lang)
    template = load_template(paths["TEMPLATE_FILE"])
    langs = {
        lang: translations[lang].get('LANGUAGE_NAME', lang)
        for lang in detect_languages(data, fallback_lang)
    }
    for lang in langs:
        translated_data = translate_data(data, lang, fallback_lang)
        translated_data['lang'] = lang
        html_output = template.render(langs=langs, **translated_data)
        lang_dir = paths["OUTPUT_DIR"] / lang

        lang_dir.mkdir(parents=True, exist_ok=True)
        output_file = lang_dir / "index.html"

        with open(output_file, "w") as out:
            out.write(html_output)

        typer.echo(f"✅ Generated: {output_file}")


@app.command()
def publish(
    data_file: Path = typer.Argument(Path("./web-data.yaml"))
):
    """Publish via rsync over SSH"""
    load_dotenv()
    paths = resolve_paths(data_file)

    remote = os.getenv("REMOTE_PATH")

    if not remote:
        typer.echo("❌ REMOTE_PATH not set in .env")
        raise typer.Exit(1)

    if not paths["OUTPUT_DIR"].exists():
        typer.echo("❌ Nothing to publish. Run build first.")
        raise typer.Exit(1)

    cmd = [
        "rsync",
        "-avz",
        "--delete",
        f"{paths['OUTPUT_DIR']}/",
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
