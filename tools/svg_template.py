from lxml import etree
from pathlib import Path
import cairosvg
import tempfile

class SvgTemplate:
    def __init__(self, template_path: Path):
        self.template_path = Path(template_path)
        self.parser = etree.XMLParser(remove_blank_text=True)
        # Parse once to extract namespaces and enable extraction
        self._original_tree = etree.parse(self.template_path, self.parser)
        self._original_namespaces = self._extract_namespaces(self._original_tree.getroot())
        # Create XPath namespace map (default namespace -> "svg")
        self._xpath_nsmap = {"svg": "http://www.w3.org/2000/svg"}
        for prefix, uri in self._original_namespaces.items():
            if prefix is not None:  # Non-default namespaces
                self._xpath_nsmap[prefix] = uri

    def _extract_namespaces(self, element):
        """Extract all namespaces from element and its ancestors."""
        ns = {}
        if element.nsmap:
            ns.update(element.nsmap)
        parent = element.getparent()
        if parent is not None:
            ns.update(self._extract_namespaces(parent))
        return ns

    def _get_text_content(self, text_el):
        """Extract text content from text element (plain or tspan)."""
        if text_el.text and text_el.text.strip():
            return text_el.text
        tspans = text_el.xpath("./svg:tspan", namespaces=self._xpath_nsmap)
        if tspans and tspans[0].text:
            return tspans[0].text
        return ""

    def extract(self, element_id: str) -> str:
        """Extract current text value from element with given id."""
        text_els = self._original_tree.getroot().xpath(
            f"//svg:text[@id='{element_id}']",
            namespaces=self._xpath_nsmap
        )
        if not text_els:
            raise ValueError(f"No text element found with id='{element_id}'")
        return self._get_text_content(text_els[0]).strip()

    def _update_text_in_tree(self, tree, key: str, value: str) -> bool:
        """Update text element by id in the given tree."""
        root = tree.getroot()
        text_els = root.xpath(f"//svg:text[@id='{key}']", namespaces=self._xpath_nsmap)
        if not text_els:
            print(f"⚠️ Warning: No <text> element with id='{key}' found")
            return False

        text_el = text_els[0]
        current = self._get_text_content(text_el).strip()

        if current == value:
            print(f"⏭️ Skipping '{key}': already '{value}'")
            return False

        print(f"✏️ Updating '{key}' from '{current}' to '{value}'")

        # Update text
        if text_el.text and text_el.text.strip():
            text_el.text = value
        else:
            tspans = text_el.xpath("./svg:tspan", namespaces=self._xpath_nsmap)
            if tspans:
                tspans[0].text = value
                for tspan in tspans[1:]:
                    tspan.getparent().remove(tspan)
            else:
                text_el.text = value
        return True

    def generate(self, png: Path | None = None, svg: Path | None = None, replacements: dict = {}):
        """
        Generate SVG and/or PNG with replacements.

        Args:
            png: Output PNG path (optional)
            svg: Output SVG path (optional)
            replacements: Dict of {id: new_text} replacements

        Behavior:
            - If both png and svg provided: export both
            - If only svg provided: export SVG only
            - If only png provided: export PNG (using temp SVG)
            - If neither provided: overwrite original template
        """
        # Determine output paths
        if svg is None and png is None:
            # Overwrite original
            svg_out = self.template_path
            png_out = None
        elif svg is None:
            # Only PNG requested - use temp SVG
            svg_out = Path(tempfile.mktemp(suffix='.svg'))
            png_out = png
        else:
            # SVG provided
            svg_out = svg
            png_out = png

        # Work on a fresh copy
        tree = etree.parse(self.template_path, self.parser)
        updated = any(
            self._update_text_in_tree(tree, key, value)
            for key, value in replacements.items()
        )

        # Save SVG if needed
        if svg_out != self.template_path or updated:
            svg_out.parent.mkdir(parents=True, exist_ok=True)
            # Register all original namespaces
            for prefix, uri in self._original_namespaces.items():
                if prefix is None: continue
                etree.register_namespace(prefix, uri)
            tree.write(svg_out, encoding='utf-8', xml_declaration=True, pretty_print=True)

        # Export PNG if needed
        if png_out:
            png_out.parent.mkdir(parents=True, exist_ok=True)
            print(f"🖼️ Exporting {png_out}...")
            cairosvg.svg2png(
                url=str(svg_out),
                write_to=str(png_out),
                output_width=1024,
                output_height=500
            )
            # Clean up temp SVG if we created one
            if svg is None and png is not None:
                svg_out.unlink()

        return svg_out if svg_out != self.template_path else None


