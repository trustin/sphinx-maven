# -*- coding: utf-8 -*-

import sys, os
from recommonmark.parser import CommonMarkParser

project = u'sphinx-maven-plugin'
copyright = u'2016, Trustin Lee et al'
version = '1.4'
release = '1.4.0.Final'

# General options
needs_sphinx = '1.0'
master_doc = 'index'
pygments_style = 'tango'
add_function_parentheses = True

extensions = ['sphinx.ext.autodoc', 'sphinxcontrib.plantuml', 'javasphinx']
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
html_use_smartypants = True
html_show_sourcelink = False
html_static_path = ['_static']

# PlantUML options
plantuml = os.getenv('plantuml')
