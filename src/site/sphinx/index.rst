.. _`Maven site plugin`: http://maven.apache.org/plugins/maven-site-plugin/
.. _`Sphinx`: http://sphinx.pocoo.org/
.. _`reStructured Text`: http://docutils.sf.net/rst.html
.. _`Markdown`: http://daringfireball.net/projects/markdown/
.. _`JavaSphinx`: http://bronto.github.io/javasphinx/
.. _`PlantUML`: http://plantuml.sourceforge.net/

Introduction
============

The *sphinx-maven* plugin is a `Maven site plugin`_ that uses `Sphinx`_ to generate the main documentation.
Sphinx itself was originally created by the Python community for the new Python documentation. It uses a
plain text format called `reStructured Text`_ which it then compiles into a variety of documentation formats
such as HTML, LaTeX (for PDF), epub. reStructured Text is similar to `Markdown`_ but - at least via Sphinx -
has better support for multi-page documentation.

The *sphinx-maven* plugin is BSD licensed just as Sphinx itself is. The plugin was created for Java-based Applications.
The idea was to introduce the benefits of reStructured Text format and Sphinx Documentation Generator for generating
documentation for custom applications.

.. Explain what comes along with this plugin. ..

This plugin contains the python packages and its dependencies that are needed to generate the documentation using
`Sphinx`_. The plugin only supports the default themes that come along with `Sphinx`_ python package.
It also incorporates other open source plugins that are helpful while explaining complex concepts within documentation.
These plugins are:

`JavaSphinx`_
--------------
This plugin is another way of incorporating javadocs directly into your sphinx documentation. This plugin provides a
way to automatically read *javadoc* documentation from source code and convert it into restructured text format for
Sphinx consumption. It also provides you bunch of reStructured Text markups that can be used to describe any
pseudo code within documentation.

`PlantUML`_
-------------

PlantUML is an open source tool that allows developers to create UML diagrams from plain text language. This project
is written in Java and a sphinx extension named *sphinxcontrib-plantuml* had to be used for integrating PlantUML with
Sphinx. Underneath PlantUML, it uses *GraphViz* to generate the diagrams. A sample uml diagram is given below:

.. uml::

   @startuml
   Alice -> Bob: Hi!
   Alice <- Bob: How are you?
   @enduml


.. toctree::
   :maxdepth: 2

   basic-usage
   configuration
   development
   faq
   javadoc/packages
