#!/usr/bin/env python

# generates metadata for packaging and distributing in different
# platforms from README.md, CHANGES.md and screenshots

import os
import operator
import contextlib
from pathlib import Path
from lxml import etree
import re
from godot_asset_library_client import git
from dataclasses import dataclass, field, asdict
from consolemsg import fail, warn
import yaml
from yamlns import ns

spdx2assetlib_license = {
    'MIT': 'MIT',
    'MPL-2.0': 'MPL-2.0',
    'GPL-3.0-only': 'GPLv3',
    'GPL-2.0-only': 'GPLv2',
    'LGPL-3.0-only': 'LGPLv3',
    'LGPL-2.1-only': 'LGPLv2.1',
    'LGPL-2.0-only': 'LGPLv2',
    'AGPL-3.0-only': 'AGPLv3',
    'GPL-3.0-or-later': 'GPLv3',
    'GPL-2.0-or-later': 'GPLv2',
    'LGPL-3.0-or-later': 'LGPLv3',
    'LGPL-2.1-or-later': 'LGPLv2.1',
    'LGPL-2.0-or-later': 'LGPLv2',
    'AGPL-3.0-or-later': 'AGPLv3',
    'EUPL-1.2': 'EUPL-1.2',
    'Apache-2.0': 'Apache-2.0',
    'CC0-1.0': 'CC0',
    'CC-BY-4.0': 'CC-BY-4.0',
    'CC-BY-3.0': 'CC-BY-3.0',
    'CC-BY-SA-4.0': 'CC-BY-SA-4.0',
    'CC-BY-SA-3.0': 'CC-BY-SA-3.0',
    'BSD-2-Clause': 'BSD-2-Clause',
    'BSD-3-Clause': 'BSD-3-Clause',
    'BSL-1.0':  'BSL-1.0',
    'ISC': 'ISC',
    'Unlicense': 'Unlicense',
    '': 'Proprietary',
}

def deduce_license():
    license_file = Path("LICENSE")
    if not license_file.exists():
        fail("LICENSE file not found. Setting it to Privative.")
    from spdx_lookup import match
    license_match = match(license_file.read_text())
    if not license_match:
        fail("LICENSE content couldn't be identified, correct or explicitly set in config the SPDX id")
    return license_match.license.id

# Unique name

def unique_name_from_manifest():
    manifest_path = Path("app/src/main/AndroidManifest.xml")

    if not manifest_path.exists():
        return warn(f"AndroidManifest.xml not found at {manifest_path}.")

    try:
        tree = etree.parse(manifest_path)
    except etree.ParseError as e:
        return warn(f"Failed to parse {manifest_path} as XML: {e}")

    root = tree.getroot()

    if not root.tag.endswith('manifest'):
        return warn(f"Root element of {manifest_path} is not '<manifest>', it's '{root.tag}'.")

    package_name = root.attrib.get('package')
    if not package_name:
        return warn(f"'package' attribute not found in <manifest> tag in {manifest_path}.")
    return package_name

def unique_name_from_build_gradle() -> str | None:
    possible_paths = [
        Path(p) for p in [
        "app/build.gradle.kts",
        "app/build.gradle",
        "build.gradle.kts", # Root build file (less common for appId)
        "build.gradle",   # Root build file (less common for appId)
        ]
    ]

    build_file_path = None
    for path in possible_paths:
        if path.exists():
            build_file_path = path
            break

    if not build_file_path:
        return warn(f"Could not find build.gradle(.kts) in standard locations")

    try:
        content = build_file_path.read_text()
    except Exception as e:
        return warn(f"Could not read {build_file_path}: {e}")
    
    default_config_pattern = re.compile(r'defaultConfig\s*\{([^}]*)\}', re.DOTALL)
    default_config_match = default_config_pattern.search(content)

    if not default_config_match:
        return warn("Could not find 'defaultConfig' block within 'android' block.")

    default_config_content = default_config_match.group(1)

    app_id_match = re.search(r"""applicationId\s*=?\s*["']([^"']+)["']""", default_config_content)

    if not app_id_match:
        return warn("'applicationId' not found within 'defaultConfig' block.")

    return app_id_match.group(1)

def deduce_unique_name():
    return unique_name_from_manifest() or unique_name_from_build_gradle()

# README

def cutoff_on_mark(content, cutoff_marker="end-of-description"):
    pattern = re.compile(rf"<!--\s*{re.escape(cutoff_marker)}\s*-->", re.IGNORECASE)
    match = pattern.search(content)
    if match:
        return content[:match.start()]
    return content  # fallback to full content if no marker

# Icons

def icon_from_android_resources() -> Path | None:
    # 1. Check for Adaptive Icon Definition (common modern approach)
    resource_base = Path() / "app" / "src" / "main" / "res"
    adaptive_icon_path = resource_base / "mipmap-anydpi-v26" / "ic_launcher.xml"
    if adaptive_icon_path.exists():
        tree = etree.parse(adaptive_icon_path)
        root = tree.getroot()

        foreground_element = None
        for elem in root.iter():
            if elem.tag.endswith('foreground'):
                foreground_element = elem
                break

        if foreground_element is not None:
            # Get the drawable reference, e.g., "@mipmap/ic_launcher_foreground"
            drawable_ref = foreground_element.get('{http://schemas.android.com/apk/res/android}drawable') or \
                        foreground_element.get('android:drawable') or \
                        foreground_element.get('drawable')
            if drawable_ref and drawable_ref.startswith('@mipmap/'):
                # Extract the drawable name, e.g., "ic_launcher_foreground"
                drawable_name = drawable_ref.split('/')[-1] # Get part after '/'

                # Now look for the highest res version of this drawable
                return find_highest_res_mipmap(project_root_path, drawable_name)

    return find_highest_res_mipmap("ic_launcher")

def find_highest_res_mipmap(drawable_name: str) -> Path | None:
    """
    Finds the highest resolution mipmap for a given drawable name.
    """
    densities = ["mipmap-xxxhdpi", "mipmap-xxhdpi", "mipmap-xhdpi", "mipmap-hdpi", "mipmap-mdpi"]
    resource_base = Path() / "app" / "src" / "main" / "res"
    for density_folder in densities:
        # Construct potential paths (PNG or WebP)
        potential_paths = [
            resource_base / density_folder / f"{drawable_name}.png",
            resource_base / density_folder / f"{drawable_name}.webp",
        ]
        for path in potential_paths:
            if path.exists():
                return path
    return None

def generate_fastlane_icon(metadata_path):
    default_icon = Path('icon.png')
    fastlane_icon = metadata_path/'en-US'/'images'/'icon.png'
    cp(default_icon, fastlane_icon)
    return
    if default_icon.exists():
        padIconMargins('icon.png', fastlane_icon)
        return
    resource_icon = icon_from_android_resources()
    cp(resource_icon, fastlane_icon)

def motto_from_splash(config):
    from svg_template import SvgTemplate
    return SvgTemplate(config.splash_svg).extract('motto').strip()

def generate_fdroid_metadata_file(metadata_path):
    meta = ns()
    # https://f-droid.org/docs/Build_Metadata_Reference/#Categories
    meta.Categories = config.fdroid_categories
    meta.License = config.license
    meta.AuthorName = "David Garcia Garzón" # TODO: obtain it from somewhere
    meta.AuthorEmail = "fdroid@canvoki.net" # TODO: obtain it from somewhere
    meta.AuthorWebSite = "https://canvoki.net/coder" # TODO: obtain it from somewhere
    meta.WebSite = config.repo_url  # TODO: get it from config so we can override it
    meta.SourceCode = config.repo_url
    meta.IssueTracker = config.issues_url
    if config.translate_url:
        meta.Translation = config.translate_url
    meta.Changelog = config.repo_url+"/blob/HEAD/CHANGES.md" # TODO: refactor into git.browse_url("CHANGES.md")
    meta.Name = config.project_name
    meta.Summary = config.short_description
    meta.Description = config.full_description
    meta.RepoType = "git" # TODO obtain it from config
    meta.Repo = config.repo_url # TODO: It works for github but others may differ checkout and browse url
    meta.AutoUpdateMode = "Version"
    meta.UpdateCheckMode = "Tags"
    if config.donate_url:
        meta.Donate = config.donate_url
    if config.liberapay_id:
        meta.Liberapay = config.liberapay_id
    if config.fdroid_antifeatures:
        meta.AntiFeatures = config.fdroid_antifeatures
    meta.CurrentVersion = config.last_version
    meta.CurrentVersionCode = int(version_to_code(config.last_version))
    meta.update(config.fdroid_fields)
    dump(Path('tools')/(config.unique_name+".yml"), meta.dump())


@dataclass
class Change:
    version_name: str
    version_date: str
    notes_md: str

    @property
    def version_tuple(self):
        return [int(v) for v in self.version_name.split('.')]

def version_to_code(version, epoch=0) -> str:
    return ''.join(
        f"{int(v):02d}"
        for v in version.split('.')
    )+'00'

@dataclass
class Config():
    repo: str = field(default_factory=git.repo_name)
    branch: str = field(default_factory=git.current_branch)
    git_hash: str = field(default_factory=git.revision_hash)
    repo_hosting: str = field(default_factory=git.repo_host)

    unique_name: str = field(default_factory=deduce_unique_name)
    categories: list[str] = field(default_factory=list)
    keywords: list[str] = field(default_factory=list)
    license: str = field(default_factory=deduce_license)
    #homepage: str = field(default_factory=)
    changes: list[Change] = field(default_factory=list)
    project_name: str = ""
    short_description: str = ""
    full_description: str = ""

    splash_svg: str = 'media/promo/splash.svg'
    motto: str = None
    fdroid_antifeatures: list[str] = field(default_factory=list)
    fdroid_categories: list[str] = field(default_factory=list)
    fdroid_fields: dict = field(default_factory=dict)
    translate_url: str = ""
    donate_url: str = ""
    liberapay_id: str = ""


    @property
    def repo_url(self):
        return git.browse_url_base(self)

    @property
    def repo_raw(self):
        return git.raw_url_base(self)

    @property
    def issues_url(self):
        return git.issues_url(self)

    @property
    def last_version(self):
        if not self.changes:
            return '0.0.0'
        return max(
            self.changes,
            key=operator.attrgetter('version_tuple'),
        ).version_name

    @classmethod
    def from_file(cls, filename):
        config_yaml = yaml.safe_load(Path(filename).read_text())
        return cls(**config_yaml)

    def __post_init__(self):
        if not self.changes:
            self._collect_changelogs()
        if not self.project_name:
            self._collect_descriptions()
        if not self.motto:
            self.motto = motto_from_splash(self)

    def _collect_changelogs(self):
        def _parse_changelog_file(text):
            """
            Extract version, date and notes (markdown string) from a changelog file content.
            Expects the first line to be like: '## 1.5.3 (2025-07-09)'
            """
            import re
            lines = text.strip().splitlines()
            heading = lines[0]
            match = re.match(r'\s*([\d\.]+)\s+\((\d{4}-\d{2}-\d{2})\)', heading)
            if not match:
                warn(f"Ignoring change versión: \"{heading}\"")
                return None, None, None
            version = match.group(1)
            date = match.group(2)
            notes_md = '\n'.join(lines[1:]).strip()
            return version, date, notes_md

        changelog=Path("CHANGES.md").read_text()

        changelog_chapters = changelog.split("##")[1:]
        self.changes = []
        for chapter in  changelog.split("##")[1:]:
            version, date, notes = _parse_changelog_file(chapter)
            if not version:
                continue # Unreleased
            self.changes.append(Change(
                version_name = version,
                version_date = date,
                notes_md = notes,
            ))

    def _collect_descriptions(self):
        """
        Extract descriptions.
        Anything before a tittle (usually badges is ignored.
        The first title is used as title/appname.
        The first line after the title is used as short descriptions.
        The rest of the document is the full description.
        You can place an `<!-- end-of-description -->` to limit what is included.
        Emoji will be filter out.
        Anything besides 
        """
        readme = Path("README.md").read_text()
        readme = readme.split('#',1)[1]
        readme_lines = readme.splitlines()
        self.project_name = self.project_name or readme_lines.pop(0).replace('#', '').strip().replace('-',' ').title()
        readme_lines = [ line for line in readme_lines if not line.strip().startswith("![") ]
        while not readme_lines[0].strip():
            readme_lines.pop(0)
        self.short_description = self.short_description or readme_lines.pop(0)
        self.full_description = self.full_description or cutoff_on_mark('\n'.join(readme_lines).strip())


yaml_metadata = 'meta/overrides.yaml'
config = Config.from_file(yaml_metadata)

emoji_pattern = re.compile(
    "["
    "\U0001F600-\U0001F64F"
    "\U0001F300-\U0001F5FF"
    "\U0001F680-\U0001F6FF"
    "\U0001F700-\U0001F77F"
    "\U0001F780-\U0001F7FF"
    "\U0001F800-\U0001F8FF"
    "\U0001F900-\U0001F9FF"
    "\U0001FA00-\U0001FA6F"
    "\U0001FA70-\U0001FAFF"
    "\u2600-\u26FF"
    "\u2700-\u27BF"
    "]+", flags=re.UNICODE)

def remove_emojis(text):
    return emoji_pattern.sub(r'', text)

def program_exists(program):
    import shutil
    return shutil.which(program) is not None

def mkdir(path):
    path.mkdir(exist_ok=True, parents=True)

def cp(origin, target):
    origin=Path(origin)
    target=Path(target)
    print(f":: \033[34;1m{origin} -> {target}\033[0m")
    mkdir(target.parent)
    target.write_bytes(origin.read_bytes())

def dump(file, content):
    print(f":: \033[34;1m{file}\033[0m\n{content}")
    file = Path(file)
    mkdir(file.parent)
    file.write_text(content)

def generate_metadata_translation_master():
    translation = ns()
    translation.project_name = config.project_name
    translation.splash_motto = config.motto
    translation.short_description = config.short_description
    translation.full_description_md = config.full_description
    translation.keywords = ', '.join(config.keywords or [])
    for release in config.changes:
        translation[f'changes_{release.version_name.replace('.','_')}_md'] = release.notes_md
    do_not_edit_warning = (
        "# DO NOT EDIT THIS FILE: This file is generated.\n"
        "# Scripts gather information around the project to build this\n"
        "# Edit other language translations, not this one.\n"
    )
    dump('meta/translations/en.yaml', do_not_edit_warning + translation.dump())

def generate_fastlane_descriptions(metadata_path, translations):
    for lang, trans in translations.items():
        for key, file in [
            ('project_name', 'title.txt'),
            ('short_description', 'short_description.txt'),
            ('full_description_md', 'full_description.txt'),
        ]:
            text = tr(translations, lang, key)
            if not text: continue

            # TODO: Process md if key ends in md
            dump(metadata_path/lang/file, text)

def tr(translations, lang, key):
    trans = translations[lang]
    result = trans.get(key)
    if result: return result
    warn(f"'Translation missing for '{key}' in language {lang}. Using english as fallback.")
    return translations['en-US'].get(key)

def generate_fastlane_changelogs(metadata_path: Path, translations):
    for lang, trans in translations.items():
        changelog_path = metadata_path/lang/'changelogs'
        for change in config.changes:
            version_name = change.version_name
            version_code = version_to_code(version_name)
            version_key = f'changes_{change.version_name.replace('.','_')}_md'
            notes_md = tr(translations, lang, version_key)
            if not notes_md: continue
            dump((changelog_path/version_code).with_suffix('.txt'),
                f"## {version_name} ({change.version_date})\n\n"
                f"{change.notes_md}\n\n"
            )

def generate_fastlane_featuredImages(metadata_path: Path, translations):
    from svg_template import SvgTemplate
    template = SvgTemplate(config.splash_svg)
    for lang, trans in translations.items():
        featured_path = metadata_path/lang/'images'/'featureGraphic.png'
        motto = tr(translations, lang, 'splash_motto')
        if not motto: continue
        template.generate(
            png=featured_path,
            replacements={
                "version": config.last_version,
                "motto": motto,
            },
        )

def generate_fastlane_screenshots(metadata_path):
    # TODO: Consider translated screenshots
    images_path = metadata_path/'en-US'/'images'/'phoneScreenshots'

    for screenshot in Path().glob("media/*.png"):
        target = images_path/screenshot.name
        cp(screenshot, target)

    for screenshot in Path().glob("media/*.webp"):
        target = images_path/screenshot.name
        cp(screenshot, target)


def padIconMargins(input_png, output_png):
    import subprocess
    pad=300
    subprocess.run([
        'convert',
        f'{input_png}',
        '-set', 'option:distort:viewport',
        f'%[fx:w+{2*pad}]x%[fx:h+{2*pad}]-{pad}-{pad}',
        '-virtual-pixel',
        'Edge',
        '-distort',
        'SRT',
        '0',
        '+repage',
        f'{output_png}',
    ])



def adapt_android_preset(metadata_path):
    appname = config.project_name
    version = config.last_version
    code = version_to_code(version)
    export_path = (Path('build/android')/appname.replace(" ", "-").lower()).with_suffix('.apk')
    icon_main = str(metadata_path/'images'/'icon.png')

    import configparser
    import json

    def get(section, name):
        return json.loads(section.get(name, 'null'))
    def set(section, name, value):
        section[name] = json.dumps(value)

    def get_named_preset(config, name):
        for section in config.sections():
            section_name = get(config[section], 'name')
            print(f"Found {section} {section_name} {name}")
            if section_name != name: continue
            return config[section], config[section+".options"]
        return None, None

    export_presets = configparser.ConfigParser()
    export_presets.read('tools/export_presets_template.cfg')
    preset, options = get_named_preset(export_presets, "Android")
    set(preset, 'export_path', str(export_path))
    set(options, 'version/name', version)
    set(options, 'version/code', code)
    set(options, 'package/name', appname)
    set(options, 'package/unique_name', config.unique_name)
    set(options, 'launcher_icons/main_192x192', icon_main)
    presets_file = Path("export_presets.cfg")
    with presets_file.open('w') as output:
        export_presets.write(output)
    modified = presets_file.read_text().replace(" = ", "=")
    dump(presets_file, modified)

def update_promo_images():
    from svg_template import SvgTemplate
    for promo_image in Path(config.splash_svg).parent.glob('*.svg'):
        svg_template = SvgTemplate(promo_image)
        svg_template.generate(
            svg=promo_image,
            png=promo_image.with_suffix('.png'),
            replacements=dict(
                version=config.last_version,
            )
        )

def insert_markdown_as_xhtml(parent, markdown_text):
    from markdown import markdown

    # Convert markdown to HTML (XHTML-compliant)
    html = markdown(cutoff_on_mark(markdown_text), extensions=["extra"])

    # Wrap in a dummy root so we can parse multiple elements
    wrapped_html = f"<wrapper>{html}</wrapper>"

    # Parse with XML parser (NOT HTML parser!)
    parser = etree.XMLParser()
    wrapper = etree.fromstring(wrapped_html, parser=parser)

    replace_headings(wrapper)
    flatten_nested_uls(wrapper)

    # Append children to the target XML node
    for child in wrapper:
        parent.append(child)

def flatten_nested_uls(root):
    for nested_ul in root.xpath('.//ul//ul'):
        parent_li = nested_ul.getparent()
        parent_ul = parent_li.getparent()
        insertion_index = parent_ul.index(parent_li) + 1

        for li in reversed(nested_ul.findall('li')):
            parent_ul.insert(insertion_index, li)

        parent_li.remove(nested_ul)
        parent_ul.remove(parent_li)

def replace_headings(root):
    for heading in root.xpath(".//h1 | .//h2 | .//h3 | .//h4 | .//h5 | .//h6"):
        print("detectado uno")
        # Create <p><strong>...</strong></p>
        strong = etree.Element("strong")
        strong.text = heading.text
        p = etree.Element("p")
        p.append(strong)

        # Copy over tail text (if any)
        if heading.tail:
            p.tail = heading.tail

        # Replace heading with <p><strong>
        parent = heading.getparent()
        parent.replace(heading, p)


def update_flatpak_metainfo():
    metainfo_path = Path(f"tools/flatpak/{config.unique_name}.metainfo.xml")

    #cp('icon.svg', Path('tools/flatpak')/f"{config.unique_name}.svg")

    tree = etree.parse(metainfo_path)
    root = tree.getroot()

    def get_and_clear(root, tag):
        node = root.find(tag)
        if node is None:
            return
        node.clear()
        return node

    # -- existing fields

    # Title → <name>
    name_node = get_and_clear(root, "name")
    name_node.text = config.project_name

    # Summary → <summary>
    summary_node = get_and_clear(root, "summary")
    summary_node.text = config.short_description

    # Full description (markdown) → <description><p>...</p></description>
    description_node = get_and_clear(root, "description")
    insert_markdown_as_xhtml(description_node, config.full_description)

    # id
    id_node = get_and_clear(root, "id")
    id_node.text = config.unique_name

    # icon
    icon_node = get_and_clear(root, "icon")
    icon_node.text = config.unique_name
    icon_node.set('type', 'stock')

    # launch method
    launchable = get_and_clear(root, "launchable")
    launchable.text = config.unique_name + ".desktop"
    launchable.set('type', 'desktop-id')

    # Screenshots
    version_name = config.last_version
    tag_name = f"{config.repo_name}-{version_name}"
    raw_repo_url_prefix = config.repo_raw.replace('refs/heads/main', tag_name)
    image_url_prefix = f"{raw_repo_url_prefix}/"

    def preview_caption(preview):

        # If explicit in metadata, take it
        if 'caption' in preview:
            return preview['caption']

        # If there is a side md file, take it
        image_file = Path(preview['repoimage'])
        caption_file = image_file.with_suffix('.md')
        if caption_file.exists():
            return caption_file.read_text().strip()

        # Else, use the file name
        caption = image_path.stem
        caption = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', caption)
        caption = re.sub('([a-z0-9])([A-Z])', r'\1_\2', caption)
        caption = caption.replace('-', ' ')
        caption = caption.replace('_', ' ')
        return caption.title()

    # TODO: split collect and generate logic here
    screenshots_node = get_and_clear(root, "screenshots")
    default = True
    for preview in config.previews:
        if 'repoimage' not in preview:
            continue

        image_path = preview['repoimage']
        preview['caption'] = preview_caption(preview)
        image_url = image_url_prefix + image_path

        screenshot_el = etree.SubElement(screenshots_node, "screenshot")
        if default:
            screenshot_el.set("type", "default")
            default = False
        image_el = etree.SubElement(screenshot_el, "image")
        image_el.text = image_url
        caption_el = etree.SubElement(screenshot_el, "caption")
        insert_markdown_as_xhtml(caption_el, preview['caption'])

    # project_license
    license_node = get_and_clear(root, "project_license")
    license_node.text = config.license

    # Strip scheme if present
    repo_host = 'https://github.com'
    url_fields = {
        'homepage': config.repo_url,
        'vcs-browser': config.repo_url,
        'bugtracker': config.issues_url,
    }

    releases_node = get_and_clear(root, "releases")
    for release in config.changes:
        release_node = etree.SubElement(releases_node, 'release')
        release_node.set('version', release.version_name)
        release_node.set('date', release.version_date)
        release_description_node = etree.SubElement(release_node, 'description')
        insert_markdown_as_xhtml(release_description_node, release.notes_md)
        

    content_rating_node = root.find("content_rating")

    for url_type, url_value in url_fields.items():
        xpath_expr = f"url[@type='{url_type}']"
        url_node = get_and_clear(root, xpath_expr)
        if url_node is None:
            url_node = etree.Element("url", type=url_type)
            content_rating_node.addnext(url_node)
        url_node.text = url_value
        url_node.set("type", url_type)

    categories_node = get_and_clear(root, "categories")
    for category in config.categories:
        etree.SubElement(categories_node, "category").text = category

    keywords_node = get_and_clear(root, "keywords")
    for word in config.keywords:
        etree.SubElement(keywords_node, "keyword").text = word

    print(f":: \033[34;1m-> {metainfo_path}\033[0m")
    etree.indent(tree, space="  ")
    tree.write(metainfo_path, encoding='utf-8', xml_declaration=True, pretty_print=True)

def update_flatpak_desktop_file():
    app_name = config.project_name
    summary = config.short_description
    desktop_file = Path("tools/flatpak")/f"{config.unique_name}.desktop"

    dump(desktop_file, f"""\
[Desktop Entry]
Name={app_name}
Comment={summary}
Categories={";".join(config.categories)}
Icon={config.unique_name}
Exec=godot-runner %U
Type=Application
Terminal=false
StartupNotify=true
""")


def generate_gradle_version_properties():
    """
    Generates a version.properties file that can be
    read by Gradle like this:

        def versionPropsFile = rootProject.file("version.properties")
        def versionProps = new Properties()
        versionProps.load(new FileInputStream(versionPropsFile))
        android {
            ...
            defaultConfig {
                ...
                versionCode = Integer.parseInt(versionProps['versionCode'])
                versionName = versionProps['versionName']
                ...
            }
            ...
        }
    """
    version = config.last_version
    code = version_to_code(version)
    dump("version.properties",
        f"versionName=\"{version}\"\n"
        f"versionCode={code}\n"
    )

def load_metadata_translations():
    return ns((
        (yaml.stem if yaml.stem != 'en' else 'en-US', ns.load(yaml))
        for yaml in Path('meta/translations').glob("*.yaml")
    ))

def generate_fastlane():
    generate_metadata_translation_master()
    translations = load_metadata_translations()
    metadata_path = Path("fastlane/metadata/android")
    mkdir(metadata_path)
    dump(Path('fastlane/.gdignore'), '')
    generate_fastlane_descriptions(metadata_path, translations)
    generate_fastlane_changelogs(metadata_path, translations)
    generate_fastlane_screenshots(metadata_path)
    generate_fastlane_icon(metadata_path)
    generate_fastlane_featuredImages(metadata_path, translations)
    generate_gradle_version_properties()
    generate_fdroid_metadata_file(metadata_path)
    #adapt_android_preset(metadata_path)

def generateMetadata():
    update_promo_images()
    generate_fastlane()
    #update_flatpak_metainfo()
    #update_flatpak_desktop_file()


if __name__ == '__main__':
    generateMetadata()
    #print(yaml.dump(asdict(config)))


