.. _`Maven site plugin`: http://maven.apache.org/plugins/maven-site-plugin/
.. _`Sphinx`: http://www.sphinx-doc.org/
.. _`reStructured Text`: http://www.sphinx-doc.org/en/master/usage/restructuredtext/basics.html
.. _`Markdown`: http://daringfireball.net/projects/markdown/
.. _`PlantUML`: http://plantuml.sourceforge.net/
.. _`Thomas Dudziak`: https://github.com/tomdz/sphinx-maven
.. _`Bala Sridhar`: https://github.com/balasridhar/sphinx-maven
.. _`sphinxcontrib-httpdomain`: https://sphinxcontrib-httpdomain.readthedocs.io/en/stable/
.. _`sphinxcontrib-inlinesyntaxhighlight`: http://sphinxcontrib-inlinesyntaxhighlight.readthedocs.org/en/latest/
.. _`sphinxcontrib-plantuml`: https://pypi.python.org/pypi/sphinxcontrib-plantuml
.. _`recommonmark`: https://recommonmark.readthedocs.org/en/latest/
.. _`javasphinx`: http://bronto.github.io/javasphinx/

sphinx-maven-plugin
===================
The *sphinx-maven-plugin* is a `Maven site plugin`_ that uses `Sphinx`_ to generate the main documentation.
Sphinx itself was originally created by the Python community for the new Python documentation. It uses a
plain text format called `reStructured Text`_ which it then compiles into a variety of documentation formats
such as HTML, LaTeX (for PDF), epub. reStructured Text is similar to `Markdown`_ but - at least via Sphinx -
has better support for multi-page documentation.

The *sphinx-maven-plugin* is BSD licensed just as Sphinx itself is. The plugin was created for Java-based
applications. The idea was to introduce the benefits of reStructured Text format and Sphinx documentation
generator for generating documentation for custom applications.

.. Explain what comes along with this plugin. ..

This plugin contains the python packages and its dependencies that are needed to generate the documentation
using `Sphinx`_. The plugin only supports the default themes that come along with `Sphinx`_ python package.
It also incorporates other open source plugins that are helpful while explaining complex concepts within
documentation. These plugins are:

Extensions
----------
This plugin provides the following additional extensions out of the box:

- `sphinxcontrib-httpdomain`_ - Sphinx domain for documenting HTTP APIs
- `sphinxcontrib-plantuml`_ - enables embedding `PlantUML`_ diagrams
- `sphinxcontrib-inlinesyntaxhighlight`_ - enables syntax-highlighting inline literals
- `recommonmark`_ - adds Markdown support
- `javasphinx`_ - adds the Java domain

Themes
------
This plugin provides the following additional themes out of the box:

- `guzzle_sphinx_theme <https://github.com/guzzle/guzzle_sphinx_theme>`_
- `sphinx_bootstrap_theme <http://ryan-roemer.github.io/sphinx-bootstrap-theme/>`_
- `sphinx_rtd_theme <https://sphinx-rtd-theme.readthedocs.io/en/latest/>`_

Changelog
---------
See `the GitHub releases page <https://github.com/trustin/sphinx-maven-plugin/releases>`_.

Credits
-------
This plugin was originally written by `Thomas Dudziak`_. `Bala Sridhar`_ since then upgraded Sphinx to 1.3.1
and added PlantUML and JavaSphinx support in his fork. I'd like to appreciate their effort that did all the
heavy lifting.

Read more
---------
.. toctree::
   :maxdepth: 2

   basic-usage
   configuration
   faq
