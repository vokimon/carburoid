from pathlib import Path
from yamlns import ns
import andaluh
import sys

spanish_file = Path(sys.argv[1])
andaluh_file = spanish_file.parent / 'es-an.yaml'

spanish = ns.load(spanish_file)
andaluh = ns((
    (key, andaluh.epa(value))
    for key, value in spanish.items()
))
if 'language_code' in andaluh:
    andaluh.language_code = "es-AN"
if 'language_name' in andaluh:
    andaluh.language_name = "Andalûh"
andaluh.dump(andaluh_file)




