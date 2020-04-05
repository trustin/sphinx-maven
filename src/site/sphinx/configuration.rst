.. _`Available builders`: http://www.sphinx-doc.org/en/master/builders.html
.. _`Including content based on tags`: http://www.sphinx-doc.org/en/master/usage/restructuredtext/directives.html#tags

Configuration
=============

The ``sphinx-maven`` plugin has these configuration options:

======================== ================================================================================================= ==================================================
Parameter                Description                                                                                       Default value
======================== ================================================================================================= ==================================================
``sourceDirectory``      The directory containing the documentation source.                                                ``${basedir}/src/site/sphinx``
``outputDirectory``      The directory where the generated output will be placed.                                          ``${project.reporting.outputDirectory}``
``binaryUrl``            The URL of the Sphinx executable binary. Must start with ``file:``, ``http:`` or ``https:``       <automatic>
``environments``         The environment variables to set when launching Sphinx. e.g. ``<VAR1>x</VAR1><VAR2>y</VAR2>``
``dotBinary``            The path of Graphviz ``dot`` binary. e.g. ``/opt/graphviz/bin/dot``
``outputName``           The base name used to create the report's output file(s).                                         ``Python-Sphinx``
``name``                 The name of the report.                                                                           ``Sphinx-Docs``
``description``          The description of the report.                                                                    ``Documentation via sphinx``
``builder``              The builder to use. See `Available builders`_ for a list of possible builders.                    ``html``
``verbose``              Whether Sphinx should generate verbose output.                                                    ``true``
``traceback``            Whether Sphinx should print full traceback on exception.                                          ``true``
``warningsAsErrors``     Whether warnings should be treated as errors.                                                     ``false``
``force``                Whether Sphinx should generate output for all files instead of only the changed ones.             ``false``
``tags``                 Additional tags to pass to Sphinx. See `Including content based on tags`_ for more information.
``asReport``             Whether documentation should be generated as a project report (keep default Maven site).          ``false``
``skip``                 Whether Sphinx execution should be skipped.                                                       ``false``
``useDoctreeCache``      Whether doctree cache should be used.                                                             ``false``
``doctreeCacheDir``      The directory containing Sphinx doctree cache. Used only when ``useDoctreeCache`` is ``true``     ``${project.reporting.outputDirectory}/.doctrees``
``useMakeMode``          Whether Sphinx should use 'make mode' (``-M`` option) instead of 'build mode' (``-b`` option).    ``false``
======================== ================================================================================================= ==================================================

Sample Documentation Config
===========================
Sphinx looks at `conf.py` in the documentation source directory for building the final HTML file. This file
contains some basic settings for getting the desired output. The configuration used for generating the plugin
documentation is given below:

.. code-block:: python

  # -*- coding: utf-8 -*-
  import sys, os
  from recommonmark.parser import CommonMarkParser

  project = u'My Project'
  copyright = u'YYYY, John Doe'
  version = '1.0'
  release = '1.0.0'

  # General options
  needs_sphinx = '1.0'
  master_doc = 'index'
  pygments_style = 'tango'
  add_function_parentheses = True

  extensions = ['sphinx.ext.autodoc', 'sphinxcontrib.plantuml']

  templates_path = ['_templates']
  exclude_trees = ['.build']
  source_suffix = ['.rst', '.md']
  source_encoding = 'utf-8-sig'
  source_parsers = {
    '.md': CommonMarkParser
  }

  # HTML options
  html_theme = 'sphinx_rtd_theme'
  html_short_title = "my-project"
  htmlhelp_basename = 'my-project-doc'
  html_use_index = True
  html_show_sourcelink = False
  html_static_path = ['_static']

  # PlantUML options
  plantuml = os.getenv('plantuml')
