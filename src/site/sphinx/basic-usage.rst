.. _`Sphinx`: http://sphinx.pocoo.org/
.. _`Sphinx first steps tutorial`: http://sphinx.pocoo.org/tutorial.html
.. _`conf.py`: http://sphinx.pocoo.org/config.html
.. _`Sphinx' examples page`: http://sphinx.pocoo.org/examples.html
.. _`reStructured Text`: http://docutils.sf.net/rst.html
.. _`Werkzeug`: http://werkzeug.pocoo.org/docs/
.. _`Werkzeug's github page`: https://github.com/mitsuhiko/werkzeug/tree/master/docs
.. _`Celery`: http://docs.celeryproject.org/en/latest/index.html
.. _`Celery's github page`: http://docs.celeryproject.org/en/latest/index.html
.. _`Maven 3 site plugin wiki page`: https://cwiki.apache.org/MAVEN/maven-3x-and-site-plugin.html
.. _`Maven 3 site plugin howto`: http://whatiscomingtomyhead.wordpress.com/2011/06/05/maven-3-site-plugin-how-to/

.. _contents:

Using the Plugin
================

The *sphinx-maven-plugin* looks for *.rst* files in the folder structure provided as part of plugin
configuration within your pom file. The default location where the plugin will look for the files is
``src/site/sphinx``.

The folder specified will contain the `reStructured Text`_ source files plus any additional things like themes
and configuration. The `Sphinx first steps tutorial`_ gives a good introduction into the required tasks.
Basically what you need is:

- A configuration file called `conf.py`_ that defines the theme and other options (such as which output formats
  etc.)
- The documentation files in reStructured Text format.
- Additional files such as static files (images etc.), usually in a ``_static`` sub directory.
- Optionally, a customized theme in a sub directory called ``_theme``

For good examples of documentation, see `Sphinx' examples page`_.

Executing within ``site`` lifecycle
===================================

Overriding default Maven site
-----------------------------

Simply add the sphinx-maven-plugin to your ``pom.xml``:

.. parsed-literal::

  <reporting>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-project-info-reports-plugin</artifactId>
        <version>2.9</version>
        <reportSets>
          <reportSet>
            <reports />
          </reportSet>
        </reportSets>
      </plugin>
      <plugin>
        <groupId>kr.motd.maven</groupId>
        <artifactId>sphinx-maven-plugin</artifactId>
        <version>\ |release|\ </version>
        <reportSets>
          <reportSet>
            <reports>
              <report>generate</report>
            </reports>
          </reportSet>
        </reportSets>
      </plugin>
    </plugins>
  </reporting>

Now all you need to do is to generate the documentation::

  mvn site

This will generate the documentation in the `target/site` folder.

Generating documentation as a project report
--------------------------------------------

You can also generate the documentation as a project report in Maven default site :

.. parsed-literal::

  <reporting>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-project-info-reports-plugin</artifactId>
        <version>2.9</version>
      </plugin>
      <plugin>
        <groupId>kr.motd.maven</groupId>
        <artifactId>sphinx-maven-plugin</artifactId>
        <version>\ |release|\ </version>
        <configuration>
          <asReport>true</asReport>
          <name>Project documentation</name>
          <description>Documentation about ${project.name}</description>
        </configuration>
      </plugin>
    </plugins>
  </reporting>

Then :

::

  mvn site

This will generate the documentation in the `target/site/sphinx` folder without affecting default Maven site (remains
in `target/site`). You can access the documentation through "Project reports" in Maven site menu bar.

Executing within normal lifecycle
=================================

You can also bind the plugin to a normal lifecycle phase. This is for instance useful if you want to generate a
documentation artifact and deploy it somewhere.

The plugin configuration is pretty much the same, the only difference is that you need to add an ``execution``
section. It might also be useful to change the ``outputDirectory`` to a different folder as the plugin by
default puts the generated documentation into the ``target/site`` folder.

A sample ``pom.xml`` plugin section could look like this:

.. parsed-literal::

  <build>
    <plugins>
      ...
      <plugin>
        <groupId>kr.motd.maven</groupId>
        <artifactId>sphinx-maven-plugin</artifactId>
        <version>\ |release|\ </version>
        <configuration>
          <outputDirectory>${project.build.directory}/docs</outputDirectory>
        </configuration>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>generate</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      ...
    </plugins>
  </build>
