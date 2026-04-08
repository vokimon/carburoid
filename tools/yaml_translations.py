#!/usr/bin/env python3

import typer
from pathlib import Path
from ruamel.yaml import YAML
from collections import OrderedDict
from typer import echo, style

app = typer.Typer(invoke_without_command=True)

# ------------------ message helpers ------------------

def step(text: str):
    """Informational step message."""
    echo(style(f"🔹 {text}", fg="blue"))

def success(text: str):
    """Success message."""
    echo(style(f"✅ {text}", fg="green"))

def warn(text: str):
    """Warning message."""
    echo(style(f"⚠️ {text}", fg="yellow"))

def error(text: str):
    """Error message (non-fatal)."""
    echo(style(f"❌ {text}", fg="red"))

def fail(text: str):
    """Fatal error: show message and exit."""
    error(text)
    raise typer.Exit(1)


@app.callback()
def main(ctx: typer.Context):
    if ctx.invoked_subcommand is None:
        typer.echo(ctx.get_help())
        raise typer.Exit()


# ------------------ YAML machinery ------------------

yaml = YAML(typ="rt")
yaml.indent(mapping=2, sequence=4, offset=2)
yaml.width = 10**9
yaml.preserve_quotes = True


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


def load_flat(path: Path) -> dict:
    """Load YAML from path and return as flattened dict. Returns empty dict if file does not exist."""
    if not path.exists():
        return {}
    with path.open("r", encoding="utf-8") as f:
        data = yaml.load(f) or {}
    return flatten(data)


def dump_flat(path: Path, flat_dict: dict):
    """Dump flattened dict into YAML at path, preserving hierarchy."""
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as f:
        yaml.dump(unflatten(flat_dict), f)


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
    fail(f"Reference file '{ref_lang}.yaml' not found")


def apply_format(target_value, ref_value):
    return type(ref_value)(target_value)


def reorder(ref_flat, target_flat, file_name, add_missing, remove_extra):
    result = OrderedDict()

    # Ids in the reference language
    for key in ref_flat:
        if key in target_flat:
            result[key] = apply_format(target_flat[key], ref_flat[key])
        else:
            warn(f"{file_name}: missing {key}")
            if add_missing:
                result[key] = ""

    # Ids not in reference language
    extras = [k for k in target_flat if k not in ref_flat]
    if extras:
        warn(f"{file_name}: extra keys {extras}")
        if not remove_extra:
            for k in sorted(extras):
                result[k] = target_flat[k]

    return result


@app.command()
def distribute(
    input_yaml: Path = typer.Argument(..., help="YAML with id -> lang -> translation"),
    output_dir: Path = typer.Argument(..., help="Directory with per language YAML translation files"),
):
    """
    Merge translations from input YAML into per-language YAML files, preserving the format in the source.
    """
    with open(input_yaml, "r", encoding="utf-8") as f:
        data = yaml.load(f) or {}

    if not isinstance(data, dict):
        fail("Input YAML must be a dictionary")

    # Gather all languages present in the input
    languages = sorted({lang for translations in data.values() for lang in translations.keys()})
    if not languages:
        fail("No languages found in input YAML")

    # Load existing YAMLs for all languages into flattened dictionaries
    lang_flats = {lang: load_flat(output_dir / f"{lang}.yaml") for lang in languages}

    # Merge input data into each language's flattened dictionary
    for key, translations in data.items():
        for lang, text in translations.items():
            flat = lang_flats[lang]
            if key in flat:
                warn(f"{lang}: '{key}' already exists, overwriting")
            flat[key] = text

    # Write updated flattened dictionaries back to YAML files
    for lang, flat in lang_flats.items():
        dump_flat(output_dir / f"{lang}.yaml", flat)
        success(f"{lang}: updated {len(data)} keys")

    success("All translations distributed successfully!")


@app.command()
def sync(
    yamls: list[str] = typer.Argument(..., min=1,
        help="Directory containing translation yamls or specific yamls to process",
    ),
    ref: str = typer.Option("en", "--ref", help="Reference language"),
    add_missing: bool = typer.Option(False, "--add-missing",
        help="Adds missing ids from reference language with an empty string ready to fill",
    ),
    remove_not_in_ref: bool = typer.Option(False, "--remove-not-in-ref",
        help="Removes ids not in the reference language",
    ),
):
    """
    Applies the order and format of the reference language to the specified yaml files
    """
    files = collect_files(yamls)
    if not files:
        fail("No YAML files found")

    ref_file = detect_reference(files, ref)
    step(f"Reference: {ref_file}")

    ref_flat = load_flat(ref_file)

    for file in files:
        if file == ref_file:
            continue

        step(f"Processing {file}...")
        flat = load_flat(file)
        new_flat = reorder(
            ref_flat,
            flat,
            file.name,
            add_missing,
            remove_not_in_ref,
        )
        dump_flat(file, new_flat)
        success(f"{file.name} synced successfully")


@app.command()
def rename(
    path: Path = typer.Argument(..., help="Directory with YAML files"),
    old_id: str = typer.Argument(..., help="Old translation key"),
    new_id: str = typer.Argument(..., help="New translation key"),
):
    """
    Renames a text id in all the translation files
    """
    if not path.is_dir():
        fail("Path must be a directory")

    files = list(path.glob("*.yml")) + list(path.glob("*.yaml"))
    if not files:
        fail("No YAML files found")

    for file in sorted(files):
        step(f"Processing {file}...")
        flat = load_flat(file)

        if old_id not in flat:
            warn(f"{file.name}: '{old_id}' not found")
            continue

        if new_id in flat:
            warn(f"{file.name}: '{new_id}' already exists (overwriting)")

        flat[new_id] = flat[old_id]
        del flat[old_id]
        dump_flat(file, flat)
        success(f"{file.name}: '{old_id}' renamed to '{new_id}'")


def choose_destination_file(dst_dir: Path, lang: str, src_file: Path) -> Path:
    for ext in (".yaml", ".yml"):
        candidate = dst_dir / f"{lang}{ext}"
        if candidate.exists():
            return candidate

    other_files = list(dst_dir.glob("*.yaml")) + list(dst_dir.glob("*.yml"))
    if other_files:
        return dst_dir / f"{lang}{other_files[0].suffix}"

    return dst_dir / src_file.name


@app.command()
def move(
    src_dir: Path = typer.Argument(..., help="Source translation directory"),
    dst_dir: Path = typer.Argument(..., help="Destination translation directory"),
    ids: list[str] = typer.Argument(..., help="IDs or prefixes to move"),
):
    """Move translation keys (possibly hierarchical) from source to destination module."""
    src_files = list(src_dir.glob("*.yml")) + list(src_dir.glob("*.yaml"))
    if not src_files:
        fail("No YAML files found in source")

    dst_dir.mkdir(parents=True, exist_ok=True)

    for src_file in sorted(src_files):
        lang = src_file.stem
        dst_file = choose_destination_file(dst_dir, lang, src_file)

        src_flat = load_flat(src_file)
        dst_flat = load_flat(dst_file)

        moved_any = False
        for move_id in ids:
            to_move = {k: v for k, v in src_flat.items()
                       if k == move_id or k.startswith(move_id + ".")}

            if not to_move:
                warn(f"{lang}: '{move_id}' not found in source")
                continue

            for k, v in to_move.items():
                if k in dst_flat:
                    warn(f"{lang}: '{k}' already in destination (overwriting)")
                dst_flat[k] = v
                del src_flat[k]
                moved_any = True

        if not moved_any:
            warn(f"{lang}: no keys moved")
            continue

        dump_flat(dst_file, dst_flat)
        dump_flat(src_file, src_flat)
        success(f"{lang}: moved {ids} from {src_dir} to {dst_dir}")


if __name__ == "__main__":
    app()
