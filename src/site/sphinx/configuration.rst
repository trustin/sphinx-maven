.. _`Sphinx commandline documentation`: http://sphinx.pocoo.org/man/sphinx-build.html?highlight=command%20line
.. _`Sphinx tag documentation`: http://sphinx.pocoo.org/markup/misc.html#tags
.. _`Jython`: http://www.jython.org/
.. _`GraphViz`: http://www.graphviz.org

Configuration
=============

The ``sphinx-maven`` plugin has these configuration options:

======================== ================================================================================================= ========================================
Parameter                Description                                                                                       Default value
======================== ================================================================================================= ========================================
``sourceDirectory``      The directory containing the documentation source.                                                ``${basedir}/src/site/sphinx``
``outputDirectory``      The directory where the generated output will be placed.                                          ``${project.reporting.outputDirectory}``
``outputName``           The base name used to create the report's output file(s).                                         ``Python-Sphinx``
``name``                 The name of the report.                                                                           ``Sphinx-Docs``
``description``          The description of the report.                                                                    ``Documentation via sphinx``
``builder``              The builder to use. See the `Sphinx commandline documentation`_ for a list of possible builders.  ``html``
``verbose``              Whether Sphinx should generate verbose output.                                                    ``true``
``warningsAsErrors``     Whether warnings should be treated as errors.                                                     ``false``
``force``                Whether Sphinx should generate output for all files instead of only the changed ones.             ``false``
``tags``                 Additional tags to pass to Sphinx. See the `Sphinx tag documentation`_ for more information.
``asReport``             Whether documentation should be generated as a project report (keep default Maven site).          ``false``
======================== ================================================================================================= ========================================

Using PlantUML
==============

The ``sphinx-maven`` plugin has support for converting uml described using *PlantUML* text format within a
*.rst* file to an image. It automatically references the image as part of the documentation in the appropriate
place where the UML was defined in the reStructured Text source file. As mentioned before, PlantUML requires
**GraphViz** to be installed on the local machine.

GraphViz
--------

GraphViz is a software package of opensource tools for drawing graphs described in DOT language scripts. More
information regarding `GraphViz`_ can be found in their website. Windows installer can be downloaded from the
website and the package is available as part of package management provided by the individual operating system
vendor.

Remember this is required only for building html pages containing GraphViz generated images. You don't need
this library for hosting the generated documentation.

PlantUML Config
---------------

.. uml::

    @startuml
    state siteBuildProcess {
        [*] -> buildJavaDocs
        buildJavaDocs: Using maven-javadoc-plugin
        buildJavaDocs -> buildSphinxDocs
        buildSphinxDocs: Using Sphinx and other extensions as needed.
        buildSphinxDocs -> [*]
    }
    @enduml

You will need to add some additional configuration options to your ``conf.py`` file (usually in
``src/site/sphinx``) to tell Sphinx how to work with *.. uml::* directives. The steps involved are

* You will need to add 'sphinxcontrib-plantuml' as an extension within the extension's list defined within
  ``conf.py``
* You will also have to import an environment variable's value within ``conf.py``.::

    import os
    plantuml = os.getenv('plantuml')

Please note that it is absolutely necessary that the environment variable's value is assigned to the variable
*plantuml*, so that the extension works as expected.

Using inline syntax highlighting
================================

.. highlight:: rst

.. role:: python(code)
    :language: python

.. role:: latex(code)
    :language: latex

You can use this extension as follows::

    .. highlight:: rst

    .. role:: python(code)
        :language: python

    .. role:: latex(code)
        :language: latex

    Now here are latex command :latex:`\\setlength` and python command
    :python:`import`, created by ``:python:`import```.  Here is a
    :literal:`literal`, which stays a literal, and
    :code:`.. highlight:: rst` makes code role look as it looks.

Which is rendered as follows:

    Now here are latex command :latex:`\\setlength` and python command
    :python:`import`, created by ``:python:`import```.  Here is a
    :literal:`literal`, which stays a literal, and
    :code:`.. highlight:: rst` makes code role look as it looks.

Using HTTP domain
=================

.. code-block:: rst

   .. http:get:: /users/(int:user_id)/posts/(tag)

      The posts tagged with `tag` that the user (`user_id`) wrote.

      **Example request**:

      .. code-block:: http

         GET /users/123/posts/web HTTP/1.1
         Host: example.com
         Accept: application/json, text/javascript

      **Example response**:

      .. code-block:: http

         HTTP/1.1 200 OK
         Vary: Accept
         Content-Type: text/javascript

         [
           {
             "post_id": 12345,
             "author_id": 123,
             "tags": ["server", "web"],
             "subject": "I tried Nginx"
           },
           {
             "post_id": 12346,
             "author_id": 123,
             "tags": ["html5", "standards", "web"],
             "subject": "We go to HTML 5"
           }
         ]

      :query sort: one of ``hit``, ``created-at``
      :query offset: offset number. default is 0
          :query limit: limit number. default is 30
          :reqheader Accept: the response content type depends on :mailheader:`Accept` header
      :reqheader Authorization: optional OAuth token to authenticate
          :resheader Content-Type: this depends on :mailheader:`Accept` header of request
      :statuscode 200: no error
          :statuscode 404: there's no user

will be rendered as:

    .. http:get:: /users/(int:user_id)/posts/(tag)

       The posts tagged with `tag` that the user (`user_id`) wrote.

       **Example request**:

       .. code-block:: http

          GET /users/123/posts/web HTTP/1.1
          Host: example.com
          Accept: application/json, text/javascript

       **Example response**:

       .. code-block:: http

          HTTP/1.1 200 OK
          Vary: Accept
          Content-Type: text/javascript

          [
            {
              "post_id": 12345,
              "author_id": 123,
              "tags": ["server", "web"],
              "subject": "I tried Nginx"
            },
            {
              "post_id": 12346,
              "author_id": 123,
              "tags": ["html5", "standards", "web"],
              "subject": "We go to HTML 5"
            }
          ]

       :query sort: one of ``hit``, ``created-at``
       :query offset: offset number. default is 0
           :query limit: limit number. default is 30
           :reqheader Accept: the response content type depends on :mailheader:`Accept` header
       :reqheader Authorization: optional OAuth token to authenticate
           :resheader Content-Type: this depends on :mailheader:`Accept` header of request
       :statuscode 200: no error
           :statuscode 404: there's no user


A note on memory usage
======================
Sphinx is run via `Jython`_ which will generate lots of small classes for various Python constructs. This means
that the plugin will use a fair amount of memory, especially PermGen space (a moderate plugin run will likely
use about 80mb of PermGen space). Therefore we suggest to either run maven with at least 256mb of heap and
128mb of PermGen space::

    MAVEN_OPTS="-Xmx256m -XX:MaxPermSize=128m" mvn site


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

  extensions = ['sphinx.ext.autodoc', 'javasphinx', 'sphinxcontrib.httpdomain',
                'sphinxcontrib.inlinesyntaxhighlight', 'sphinxcontrib.plantuml',
                 'sphinxcontrib.scaladomain']

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
