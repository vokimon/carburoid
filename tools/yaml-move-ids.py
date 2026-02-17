#!/usr/bin/env python

import os
import shutil
from typing import List
import typer
from ruamel.yaml import YAML

app = typer.Typer(help="Move translation keys between Android component directories.")

yaml = YAML()
yaml.preserve_quotes = True
yaml.width = 4096  # prevent line wrapping to preserve format

def get_nested(d: dict, path: List[str]):
    """Navigate nested dict to get sub-dict at path[:-1]."""
    for key in path[:-1]:
        d = d.get(key, {})
        if not isinstance(d, dict):
            return {}
    return d

def ensure_nested(d: dict, path: List[str]):
    """Ensure nested dicts exist according to path[:-1] and return the final dict."""
    for key in path[:-1]:
        if key not in d or not isinstance(d[key], dict):
            d[key] = {}
        d = d[key]
    return d

@app.command()
def move_translations(
    source: str = typer.Argument(..., help="Source translations directory"),
    target: str = typer.Argument(..., help="Target translations directory"),
    keys: List[str] = typer.Argument(..., help="Keys to move (can include dot paths)"),
    backup: bool = typer.Option(True, help="Do not generate backup files"),
    keep_source: bool = typer.Option(False, help="Keep the keys in the source file"),
    verbose: bool = typer.Option(False, help="Print operations performed")
):
    """
    Copies or moves translation ids from every <language>.yaml in a source directory
    into the matching <language>.yaml in the target directory,
    creating when missing the target yaml and the sections of the id.

    Example: 
    """
    if not os.path.exists(source):
        typer.secho(f"[ERROR] Source directory does not exist: {source}", fg=typer.colors.RED)
        raise typer.Exit(1)
    if not os.path.exists(target):
        typer.secho(f"[ERROR] Target directory does not exist: {target}", fg=typer.colors.RED)
        raise typer.Exit(1)

    for filename in os.listdir(source):
        if not filename.endswith(".yaml"):
            continue

        source_file = os.path.join(source, filename)
        target_file = os.path.join(target, filename)

        with open(source_file, "r", encoding="utf-8") as f:
            source_data = yaml.load(f) or {}

        if os.path.exists(target_file):
            with open(target_file, "r", encoding="utf-8") as f:
                target_data = yaml.load(f) or {}
        else:
            target_data = {}

        any_moved = False

        for key_path in keys:
            parts = key_path.split(".")
            src_dict = get_nested(source_data, parts)
            last_part = parts[-1]

            if last_part not in src_dict:
                typer.secho(f"[WARNING] {key_path} not found in {filename}", fg=typer.colors.YELLOW)
                continue

            value = src_dict[last_part]
            tgt_dict = ensure_nested(target_data, parts)
            tgt_dict[last_part] = value
            any_moved = True
            if verbose:
                typer.secho(f"[MOVE] {key_path} in {filename}", fg=typer.colors.GREEN)
            if not keep_source:
                del src_dict[last_part]

        if any_moved:
            # Backup if needed
            if not no_backup:
                shutil.copy(source_file, source_file + ".bak")
                if os.path.exists(target_file):
                    shutil.copy(target_file, target_file + ".bak")
            # Write YAML preserving formatting
            with open(source_file, "w", encoding="utf-8") as f:
                yaml.dump(source_data, f)
            with open(target_file, "w", encoding="utf-8") as f:
                yaml.dump(target_data, f)

if __name__ == "__main__":
    app()

