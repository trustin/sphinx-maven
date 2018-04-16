# -*- coding: utf-8 -*-

import sys, os
from recommonmark.parser import CommonMarkParser
import yaml

# Ensure tags are passed.
assert tags.has('tagFoo')
assert tags.has('tagBar')

# Ensure we can load a YAML file.
with open('test.yaml', 'r') as stream:
    test_yaml = yaml.load(stream)
    assert test_yaml[0] == 'a'
    assert test_yaml[1] == 'b'
    assert test_yaml[2] == 'c'

project = u'sphinx-maven-plugin'
copyright = u'2016, Trustin Lee et al'
version = '2.0'
release = '2.0.1'

# General options
needs_sphinx = '1.0'
master_doc = 'index'
pygments_style = 'tango'
add_function_parentheses = True

extensions = ['sphinx.ext.autodoc', 'javasphinx', 'sphinxcontrib.httpdomain',
              'sphinxcontrib.inlinesyntaxhighlight', 'sphinxcontrib.plantuml']

templates_path = ['_templates']
exclude_trees = ['.build']
source_suffix = ['.rst', '.md']
source_encoding = 'utf-8-sig'
source_parsers = {
  '.md': CommonMarkParser
}

# HTML options
html_theme = 'sphinx_rtd_theme'
html_short_title = "sphinx-maven-plugin"
htmlhelp_basename = 'sphinx-maven-plugin-doc'
html_use_index = True
html_show_sourcelink = False
html_static_path = ['_static']

# PlantUML options
plantuml = os.getenv('plantuml')
