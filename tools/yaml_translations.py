#!/usr/bin/env python3

import typer
import yaml
from pathlib import Path
from ruamel.yaml import YAML
from collections import OrderedDict

app = typer.Typer(invoke_without_command=True)

@app.callback()
def main(ctx: typer.Context):
    if ctx.invoked_subcommand is None:
        typer.echo(ctx.get_help())
        raise typer.Exit()


@app.command()
def distribute(
    input_file: Path = typer.Argument(..., help="YAML with id: lang: translation"),
    output_dir: Path = typer.Argument(..., help="Directory with per language YAML translation files"),
):
    """
    Append translations to per-language YAML files without modifying existing content.
    """

    with open(input_file, "r", encoding="utf-8") as f:
        data = yaml.safe_load(f)

    if not isinstance(data, dict):
        typer.echo("❌ Input YAML must be a dictionary")
        raise typer.Exit(1)

    for key, translations in data.items():
        if not isinstance(translations, dict):
            typer.echo(f"⚠️ Skipping {key}: not a dict")
            continue

        for lang, text in translations.items():
            lang_file = output_dir / f"{lang}.yaml"

            # Ensure directory exists
            lang_file.parent.mkdir(parents=True, exist_ok=True)

            # Prepare YAML line (safe dump of single key)
            snippet = yaml.dump(
                {key: text},
                allow_unicode=True,
                sort_keys=False,
            )

            # Append to file
            with open(lang_file, "a", encoding="utf-8") as f:
                # Ensure separation
                f.write("\n")
                f.write(snippet)

            typer.echo(f"✅ {lang}: appended '{key}'")

    typer.echo("🎉 Done!")


yaml = YAML(typ='rt')
yaml.indent(mapping=2, sequence=4, offset=2)
yaml.width = 10**9

def flatten(d, parent_key="", sep="."):
    items = {}
    for k, v in (d or {}).items():
        new_key = f"{parent_key}{sep}{k}" if parent_key else k
        if isinstance(v, dict):
            items.update(flatten(v, new_key, sep))
        else:
            items[new_key] = v
    return items


def unflatten(d, sep="."):
    result = {}
    for key, value in d.items():
        parts = key.split(sep)
        target = result
        for part in parts[:-1]:
            target = target.setdefault(part, {})
        target[parts[-1]] = value
    return result


def collect_files(paths):
    files = set()

    if not paths:
        paths = ["."]

    for p in paths:
        p = Path(p)
        if p.is_dir():
            files.update(p.glob("*.yml"))
            files.update(p.glob("*.yaml"))
        elif p.suffix in [".yml", ".yaml"]:
            files.add(p)

    return sorted(files)


def detect_reference(files, ref_lang):
    for f in files:
        if f.stem == ref_lang:
            return f
    raise typer.Exit(f"Reference file '{ref_lang}.yaml' not found")

def apply_format(target_value, ref_value):
    return type(ref_value)(target_value)


def reorder(ref_flat, target_flat, file_name, add_missing, remove_extra):
    result = OrderedDict()

    # claves de referencia
    for key in ref_flat:
        if key in target_flat:
            result[key] = apply_format(target_flat[key], ref_flat[key])
        else:
            typer.echo(f"[MISSING] {file_name}: {key}")
            if add_missing:
                result[key] = ""

    # extras
    extras = [k for k in target_flat if k not in ref_flat]
    if extras:
        typer.echo(f"[EXTRA] {file_name}: {extras}")

        if not remove_extra:
            for k in sorted(extras):
                result[k] = target_flat[k]

    return result


@app.command()
def sync(
    paths: list[str] = typer.Argument(..., min=1),
    ref: str = typer.Option("en", "--ref", help="Reference language"),
    add_missing: bool = typer.Option(False, "--add-missing",
        help="Adds missing ids from reference language with an empty string ready to fill",
    ),
    remove_not_in_ref: bool = typer.Option(False, "--remove-not-in-ref",
        help="Removes ids not in the reference language",
    ),
):
    """
    Syncs string order and yaml format with the one in the reference language.

    Provide a translation directory or a set of yaml files.
    """

    files = collect_files(paths)

    if not files:
        typer.echo("No YAML files found")
        raise typer.Exit()

    ref_file = detect_reference(files, ref)

    typer.echo(f"Reference: {ref_file}")

    # cargar referencia
    with ref_file.open() as f:
        ref_data = yaml.load(f) or {}

    ref_flat = flatten(ref_data)
    ref_keys = list(ref_flat.keys())

    # procesar resto
    for file in files:
        if file == ref_file:
            continue

        typer.echo(f"\nProcessing {file}...")

        with file.open() as f:
            data = yaml.load(f) or {}

        flat = flatten(data)

        new_flat = reorder(
            ref_flat,
            flat,
            file.name,
            add_missing,
            remove_not_in_ref,
        )

        new_data = unflatten(new_flat)

        with file.open("w") as f:
            yaml.dump(new_data, f)


if __name__ == "__main__":
    app()
