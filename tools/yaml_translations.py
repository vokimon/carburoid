#!/usr/bin/env python3

import typer
import yaml
from pathlib import Path

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


if __name__ == "__main__":
    app()
