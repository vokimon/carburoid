from pathlib import Path
from yamlns import ns
import andaluh
import sys
import re

def protect_variables(text):
    """Extract {variable} patterns and replace with numeric placeholders (no letters)"""
    if not isinstance(text, str):
        return text, []
    
    variables = []
    def replace_var(match):
        variables.append(match.group(0))
        return f"%%%{len(variables) - 1}%%%"
    
    protected = re.sub(r'\{[^}]+\}', replace_var, text)
    return protected, variables

def restore_variables(text, variables):
    """Restore original {variable} patterns from numeric placeholders"""
    if not isinstance(text, str):
        return text
    
    for i, var in enumerate(variables):
        text = text.replace(f"%%%{i}%%%", var)
    return text

def translate_value(value):
    """Translate a value while protecting {variable} patterns"""
    if not isinstance(value, str):
        return value
    
    protected, variables = protect_variables(value)
    translated = andaluh.epa(protected)
    restored = restore_variables(translated, variables)
    return restored

spanish_file = Path(sys.argv[1])
andaluh_file = spanish_file.parent / 'and.yaml'

spanish = ns.load(spanish_file)
andaluh = ns((
    (key, translate_value(value))
    for key, value in spanish.items()
))
if 'language_code' in andaluh:
    andaluh.language_code = "and"
if 'language_name' in andaluh:
    andaluh.language_name = "Andalûh"
andaluh.dump(andaluh_file)

