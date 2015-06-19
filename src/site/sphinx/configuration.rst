.. _`Sphinx commandline documentation`: http://sphinx.pocoo.org/man/sphinx-build.html?highlight=command%20line
.. _`Sphinx tag documentation`: http://sphinx.pocoo.org/markup/misc.html#tags
.. _`Jython`: http://www.jython.org/
.. _`rst2pdf manual`: http://lateral.netmanagers.com.ar/static/manual.pdf

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
``javaSphinxIncludeDir`` The directory containing the Java Source files.
``javaSphinxOutputDir``  The directory where the generated output will be placed.                                          ``${sourceDirectory}/javadocs/``
``javaSphinxVerbose``    Whether JavaSphinx should generate verbose output.                                                ``true``
``javaSphinxForce``      Whether JavaSphinx should generate output for all files instead of only the changed ones.         ``false``
======================== ================================================================================================= ========================================

Building PDFs
=============

The ``sphinx-maven`` plugin has experimental support for PDF generation. You'll turn it on
by using the pdf builder, e.g.::

    <plugin>
      <groupId>org.caltesting.maven</groupId>
      <artifactId>sphinx-maven-plugin</artifactId>
      <version>3.0.0</version>
      <configuration>
        <builder>pdf</builder>
        <outputDirectory>${project.reporting.outputDirectory}/pdf</outputDirectory>
      </configuration>
    </plugin>

You'll likely also have to add some additional configuration options to your ``conf.py``
file (usually in ``src/site/sphinx``) to tell the pdf builder what to do. At a minimum
you'll probably need to point it to the index page by adding this to the end::

    # -- Options for PDF output ---------------------------------------------------
    pdf_documents = [
        ('index', u'<file name>', u'<document name>', u'<author>'),
    ]

For additional options see the Sphinx section of the `rst2pdf manual`_.

Please note that alpha channels in the images (i.e. PNGs) are not supported, and will be replaced with
black pixels. This is most likely not what you want, so please don't use alpha channels in the images.

Building JavaDocs
==================

The ``sphinx-maven`` plugin has support for JavaDocs generation. You can turn it on by passing the necessary
arguments to the plugin configuration. e.g::

    <plugin>
      <groupId>org.caltesting.maven</groupId>
      <artifactId>sphinx-maven-plugin</artifactId>
      <version>3.0.0</version>
      <configuration>
          <javaSphinxVerbose>true</javaSphinxVerbose>
          <javaSphinxIncludeDir>
              <entry>${basedir}/src/main/java/</entry>
          </javaSphinxIncludeDir>
          <javaSphinxOutputDir>${basedir}/src/site/sphinx/javadoc/</javaSphinxOutputDir>
      </configuration>
    </plugin>

You will need to add some additional configuration options to your ``conf.py`` file
(usually in ``src/site/sphinx``) to tell Sphinx how to interpret the JavaSphinx generated *.rst* files.
You will have to add 'javasphinx' as an extension within the extension's list defined within ``conf.py``.

Please note that the output directory for *javasphinx* generated documentation should be within the *sourceDirectory*
folder structure specified for *Sphinx* configuration. So that *Sphinx* knows to also include these files as well when
generating the html/pdf output.

Using PlantUML
===============

The ``sphinx-maven`` plugin has support for converting uml described using *PlantUML* text format within a *.rst* file
to an image. It automatically references the image as part of the documentation in the appropriate place where the UML
was defined in the reStructured Text source file.

.. uml::

    @startuml
    state pluginBuildProcess {
        [*] -> buildJavaDocs
        buildJavaDocs: Using JavaSphinx
        buildJavaDocs -> buildSphinxDocs
        buildSphinxDocs: Using Sphinx and other extensions as needed.
        buildSphinxDocs -> [*]
    }
    @enduml

You will need to add some additional configuration options to your ``conf.py`` file (usually in ``src/site/sphinx``)
to tell Sphinx how to work with *.. uml::* directives. The steps involved are

* You will need to add 'sphinxcontrib-plantuml' as an extension within the extension's list defined within ``conf.py``
* You will also have to import an environment variable's value within ``conf.py``.::

    import os
    plantuml = os.getenv('plantuml')

Please note that it is absolutely necessary that the environment variable's value is assigned to the variable *plantuml*,
so that the extension works as expected.

A note on memory usage
======================

Sphinx is run via `Jython`_ which will generate lots of small classes for various Python constructs. This means that
the plugin will use a fair amount of memory, especially PermGen space (a moderate plugin run will likely use about 80mb
of PermGen space). Therefore we suggest to either run maven with at least 256mb of heap and 128mb of PermGen space, e.g.

    MAVEN_OPTS="-Xmx256m -XX:MaxPermSize=128m" mvn site
