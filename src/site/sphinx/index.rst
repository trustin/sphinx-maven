.. _`Maven site plugin`: http://maven.apache.org/plugins/maven-site-plugin/
.. _`Sphinx`: http://sphinx.pocoo.org/
.. _`reStructured Text`: http://docutils.sf.net/rst.html
.. _`Markdown`: http://daringfireball.net/projects/markdown/
.. _`PlantUML`: http://plantuml.sourceforge.net/
.. _`Thomas Dudziak`: https://github.com/tomdz/sphinx-maven
.. _`Bala Sridhar`: https://github.com/balasridhar/sphinx-maven
.. _`sphinxcontrib-plantuml`: https://pypi.python.org/pypi/sphinxcontrib-plantuml
.. _`sphinxcontrib-inlinesyntaxhighlight`: http://sphinxcontrib-inlinesyntaxhighlight.readthedocs.org/en/latest/
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
Besides the extensions distributed with `Sphinx`_, this plugin includes the following extensions:

- `sphinxcontrib-plantuml`_ - enables embedding `PlantUML`_ diagrams
- `sphinxcontrib-inlinesyntaxhighlight`_ - enables syntax-highlighting inline literals
- `recommonmark`_ - adds Markdown support
- `javasphinx`_ - adds the Java domain

Credits and changes
-------------------
This plugin was originally written by `Thomas Dudziak`_. `Bala Sridhar`_ since then upgraded Sphinx to 1.3.1
and added PlantUML and JavaSphinx support in his fork. I'd like to appreciate their effort that did all the
heavy lifting. This fork includes the following additional changes:

1.4.0.Final (unreleased)
^^^^^^^^^^^^^^^^^^^^^^^^
- Added the ``asReport`` option to keep the default Maven site
- Upgraded Sphinx to 1.4.6
- Upgraded alabaster to 0.7.9
- Upgraded Babel to 2.3.4
- Upgraded CommonMark to 0.7.2
- Upgraded imagesize to 0.7.1
- Upgraded Pygments to 2.1.3
- Upgraded pytz to 2016.6.1
- Upgraded sphinx_rtd_theme to 0.1.10a0

1.3.1.Final (25-Apr-2016)
^^^^^^^^^^^^^^^^^^^^^^^^^
- The line separators of the generated text files are now converted to the line separator of the current platform, so that the version control systems like Git do not complain about inconsistent line endings.

1.3.0.Final (01-Apr-2016)
^^^^^^^^^^^^^^^^^^^^^^^^^
- Upgraded Sphinx to 1.4.0

  - Added imagesize 0.7.0

- Upgraded Pygments to 2.1.3
- Upgraded pytz to 2016.3
- Upgraded CommonMark to the latest master (0.6.3+)

  - Added future 0.15.2 (``builtins`` only)

- Upgraded reCommonMark to the latest master (0.4.0+)

  - Fixed the issues with renames in CommonMark

- Upgraded PlantUML to 8037
- Updated sphinx_rtd_theme to the latest master
- Updated sphinxcontrib/plantuml to the latest master

1.2.1.Final (07-Jan-2016)
^^^^^^^^^^^^^^^^^^^^^^^^^
- Downgraded Sphinx to the latest stable (1.3.3+) for stability
- Downgraded Jython to 2.7.0 due to an ``mkdirs`` error on Linux
- Upgraded Pygments to the latest master (pre-2.1)
- Upgraded Alabaster to 0.7.7
- Upgraded reCommonMark to 0.4.0
- Upgraded Babel to 2.2.0

1.2.0.Final (05-Dec-2015)
^^^^^^^^^^^^^^^^^^^^^^^^^
- Added `sphinxcontrib-inlinesyntaxhighlight`_ extension
- Added `javasphinx`_ extension back again

  - ``javasphinx-apidoc`` support has not been added back, because I'm not convinced it's more useful than
    Javadoc

- Updated Sphinx to the latest master (1.4.0a+)
- Updated Pygments to the latest master (pre-2.1)

1.1.0.Final (25-Nov-2015)
^^^^^^^^^^^^^^^^^^^^^^^^^
- Updated Sphinx to the latest master
- Updated both Java and Python dependencies
- Reduced the time taken for launching Sphinx
- Removed JavaSphinx and PDF support
- Fixed Windows-related issues

Read more
---------
.. toctree::
   :maxdepth: 2

   basic-usage
   configuration
   faq
