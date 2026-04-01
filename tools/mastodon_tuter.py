#!/usr/bin/env python

# pip install typer mastodon dotenv yaml

import os
import sys
import typer
import yaml
from mastodon import Mastodon
from dotenv import load_dotenv
from typing import List, Dict, Any, Optional, Set

app = typer.Typer(
    name = "mastodon stuter",
    help="CLI mastodon interaction",
    no_args_is_help=True,
)
load_dotenv()

MAX_CONTENT_LENGTH = 500  # máximo número de caracteres por post en Mastodon

def fail(reason: str):
    """Imprime un mensaje de error y sale del programa."""
    typer.echo(f"❌ {reason}", err=True)
    raise typer.Exit(code=1)

def get_available_accounts() -> Set[str]:
    """Devuelve los alias de cuentas configuradas en .env."""
    accounts = set()
    for key in os.environ.keys():
        if key.startswith("MASTODON_") and key.endswith("_ACCESS_TOKEN"):
            alias = key[len("MASTODON_"):-len("_ACCESS_TOKEN")]
            accounts.add(alias.lower())
    return accounts

def get_client(account: str) -> Mastodon:
    """Crea un cliente Mastodon para la cuenta indicada."""
    prefix = f"MASTODON_{account.upper()}"
    instance_url = os.getenv(f"{prefix}_INSTANCE_URL")
    access_token = os.getenv(f"{prefix}_ACCESS_TOKEN")

    available_accounts = get_available_accounts()
    if not available_accounts:
        fail("No Mastodon accounts configured in .env")

    if account.lower() not in available_accounts:
        fail(f"Unknown account alias: {account}\nAvailable accounts: {', '.join(sorted(available_accounts))}")

    if not instance_url or not access_token:
        fail(f"Missing env var for {account}")

    return Mastodon(access_token=access_token, api_base_url=instance_url)

def read_file(file: str) -> str:
    """Lee contenido de un archivo o de stdin si file == '-'."""
    if file == "-":
        if sys.stdin.isatty():
            fail("No content piped to stdin")
        return sys.stdin.read()
    try:
        with open(file, "r", encoding="utf-8") as f:
            return f.read()
    except FileNotFoundError:
        fail(f"File not found: {file}")

def parse_markdown_thread(text: str) -> List[Dict[str, Any]]:
    """Parsea un hilo en Markdown con metadatos YAML opcionales."""
    text = text.lstrip("\ufeff")
    if not text.startswith("---"):
        return [{"meta": {}, "content": text.strip()}]

    sections = [s.strip() for s in text.split("---") if s.strip()]
    posts = []
    i = 0
    while i < len(sections):
        meta_text = sections[i]
        i += 1
        try:
            meta = yaml.safe_load(meta_text) or {}
            if not isinstance(meta, dict):
                fail(f"Post {len(posts)+1}: YAML must be a mapping/dict")
        except yaml.YAMLError as e:
            fail(f"Error parsing YAML in post {len(posts)+1}: {e}")
        content = ""
        if i < len(sections):
            content = sections[i].strip()
            i += 1
        posts.append({"meta": meta, "content": content})
    return posts

def validate_posts(posts: List[Dict[str, Any]]):
    """Valida contenido y existencia de archivos de media para cada post."""
    for idx, post in enumerate(posts):
        if not post["content"]:
            fail(f"Post {idx+1} has empty content")
        if len(post["content"]) > MAX_CONTENT_LENGTH:
            fail(f"Post {idx+1} exceeds Mastodon length limit ({len(post['content'])} chars)")
        media_items = post["meta"].get("media", [])
        for media in media_items:
            file_path = media.get("file")
            if not file_path or not os.path.isfile(file_path):
                fail(f"Post {idx+1} media file not found: {file_path}")

def do_boost(post_url: str, boosters: List[str]):
    """Realiza boost y favorito de un post dado con varias cuentas."""
    for booster in boosters:
        client = get_client(booster)
        try:
            results = client.search(post_url, result_type="statuses")
            if not results["statuses"]:
                typer.echo(f"❌ {booster}: Post not found on this instance", err=True)
                continue
            status_local = results["statuses"][0]["id"]
            client.status_favourite(status_local)
            client.status_reblog(status_local)
            typer.echo(f" ✅{booster}: Boosted and favorited successfully")
        except Exception as e:
            fail(f"{booster}: Failed ({e})")

@app.command(help="Post a markdown thread to Mastodon with media, boosters, and metadata.")
def post(
    file: str = typer.Argument(..., help="Markdown file with thread or '-' for stdin"),
    poster: str = typer.Argument(..., help="Poster account alias"),
    visibility: Optional[str] = typer.Option(None, help="Override visibility"),
    cw: Optional[str] = typer.Option(None, help="Override content warning"),
    language: Optional[str] = typer.Option(None, help="Override language"),
    sensitive: Optional[bool] = typer.Option(None, help="Override sensitive flag"),
    boosters: Optional[List[str]] = typer.Option(None, help="Optional list of boosters to override YAML"),
):
    text = read_file(file)
    posts = parse_markdown_thread(text)
    if not posts:
        fail("No posts found in file")
    validate_posts(posts)

    client = get_client(poster)
    prev_id = None

    for idx, post in enumerate(posts):
        meta = post["meta"]
        content = post["content"]

        final_visibility = visibility or meta.get("visibility", "public")
        final_cw = cw or meta.get("cw")
        final_language = language or meta.get("language")
        final_sensitive = sensitive if sensitive is not None else meta.get("sensitive", False)
        final_boosters = boosters or meta.get("boosters", [])

        media_items = meta.get("media", [])
        media_ids = []
        for m in media_items:
            file_path = m.get("file")
            alt_text = m.get("alt")
            if not file_path:
                fail(f"Post {idx+1}: media entry missing 'file'")
            try:
                media = client.media_post(file_path, description=alt_text)
                media_ids.append(media["id"])
            except Exception as e:
                fail(f"Post {idx+1}: Failed to upload media {file_path}: {e})")

        try:
            status = client.status_post(
                content,
                visibility=final_visibility,
                spoiler_text=final_cw,
                sensitive=final_sensitive,
                language=final_language,
                media_ids=media_ids or None,
                in_reply_to_id=prev_id
            )
            prev_id = status["id"]
            typer.echo(f"Post {idx+1} posted successfully: {status['url']}")
        except Exception as e:
            fail(f"Post {idx+1} failed: {e}")

        if final_boosters:
            do_boost(status["url"], final_boosters)

@app.command(help="Boost and favorite a Mastodon post by URL using one or more accounts")
def boost(
    post_url: str = typer.Argument(..., help="Public URL of the post to boost/favorite"),
    boosters: List[str] = typer.Argument(..., help="One or more account aliases that will boost/favorite"),
):
    if not boosters:
        fail("At least one booster must be provided")
    do_boost(post_url, boosters)

if __name__ == "__main__":
    app()

