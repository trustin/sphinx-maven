.. _`Maven site plugin`: http://maven.apache.org/plugins/maven-site-plugin/
.. _`Sphinx`: http://sphinx.pocoo.org/
.. _`reStructured Text`: http://docutils.sf.net/rst.html
.. _`Markdown`: http://daringfireball.net/projects/markdown/
.. _`PlantUML`: http://plantuml.sourceforge.net/
.. _`Thomas Dudziak`: https://github.com/tomdz/sphinx-maven
.. _`Bala Sridhar`: https://github.com/balasridhar/sphinx-maven

Introduction
============

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

`PlantUML`_
-----------

PlantUML is an open source tool that allows developers to create UML diagrams from plain text language. This project
is written in Java and a sphinx extension named *sphinxcontrib-plantuml* had to be used for integrating PlantUML with
Sphinx. Underneath PlantUML, it uses *GraphViz* to generate the diagrams. A sample uml diagram is given below:

.. uml::

   @startuml
   Alice -> Bob: Hi!
   Alice <- Bob: How are you?
   @enduml

Credits and changes
-------------------
This plugin was originally written by `Thomas Dudziak`_. `Bala Sridhar`_ since then upgraded Sphinx to 1.3.1
and added PlantUML and JavaSphinx support in his fork. I'd like to appreciate their effort that did all the
heavy lifting. This fork includes the following small additional changes:

- Available in Maven central repository
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
