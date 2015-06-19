.. _`rst2pdf bug`: https://code.google.com/p/rst2pdf/issues/detail?id=458
.. _`tomdz repo`: https://github.com/tomdz/sphinx-maven

Updating Sphinx
===============

The project comes with a bash script which will update the embedded sphinx installation and other python packages
automatically. Simple invoke it like ::

    ./src/main/build/build-python-pkgs.sh

Update Script Process
----------------------

The update script expects ``python`` and ``pip`` to be installed on the machine where this script is going to be invoked.

1. It sets up a temporary working directory ``target/sphinx-tmp/site-packages/`` and cd's into it.
2. It uses the ``pip`` program to install ``sphinx``, ``rst2pdf``, ``javasphinx``, ``html5lib`` and ``sphinxcontrib-plantuml``.
3. The ``pip`` program automatically installs all the dependencies that are required for the packages listed above.
4. Both ``sphinx`` and ``javasphinx`` don't work out of the box with Jython's latest version (see e.g. this `rst2pdf bug`_) which
   we'll patch.
5. we now create the ``sphinx.zip`` out of the installed modules, and move it to
   ``src/main/resources`` (which will cause it to be included as a file in the plugin).
6. Finally, we download ``plantuml.jar`` from its site and move it to ``src/main/resources``
   (which will cause it to be included as a file in the plugin).

Please Note if the jar file does not get downloaded, you can download the jar file manually and then add it within
``src/main/resources``

Running Sphinx Under Python
============================

If you want to compare the outputs between this plugin and directly using Sphinx, install it like this::

    pip install sphinx rst2pdf javasphinx sphinxcontrib-plantuml

Now run Sphinx like so in your project's root directory::

    sphinx-build -v -a -E -n -b html src/site/sphinx target/site
    sphinx-build -a -E -n -b pdf src/site/sphinx target/site/pdf



Third Party Libraries
========================

As introduced before, The plugin bundles the third party libraries required for building the documentation. At this point in time,
these are the packages that are bundled along with their version numbers.

=========================== ===========================
Package                     Versions
=========================== ===========================
``PlantUML``                8024.0
``Sphinx``                  1.3.1
``rst2pdf``                 0.93
``javasphinx``              0.9.12
``html5lib``                0.99999
``sphinxcontrib-plantuml``  0.5
=======================================================

Contributors
=============

This project is forked off of `tomdz repo`_ The base project was instrumental in bringing ``Sphinx`` documentation facilities
for Java based applications. This repo just builds on top of that and adds additional functionalities provided by ``JavaSphinx``
and ``PlantUML``.

* Thomas Dudziak
* Bala Sridhar
