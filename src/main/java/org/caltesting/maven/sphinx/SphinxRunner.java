package org.caltesting.maven.sphinx;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.reporting.MavenReportException;
import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Sphinx Runner.
 *
 * @author Bala Sridhar & tomdz
 * @version June 12, 2015
 */
public class SphinxRunner {

    /** PlantUML Jar Exec Script for sphinx-plantuml plugin. */
    private String PLANTUML_JAR;

    /** Maven Logging Capability. */
    private static Log LOG;

    /**
     * Default Constructor.
     */
    public SphinxRunner() {
    }

    /**
     * Initialize Environment to execute the plugin.
     *
     * @param sphinxSourceDirectory
     * @param log
     */
    public void initEnv(File sphinxSourceDirectory, Log log) throws MavenReportException {
        this.LOG = log;
        if (sphinxSourceDirectory == null) {
            throw new IllegalArgumentException("sphinxSourceDirectory is empty.");
        }

        unpackSphinx(sphinxSourceDirectory);
        unpackPlantUml(sphinxSourceDirectory);

        PLANTUML_JAR = "java -jar " + sphinxSourceDirectory.getAbsolutePath() + "/plantuml.jar";
        LOG.debug("PlantUml: " + PLANTUML_JAR);

        // use headless mode for AWT (prevent "Launcher" app on Mac OS X)
        System.setProperty("java.awt.headless", "true");

        // this setting supposedly allows GCing of jython-generated classes but I'm
        // not sure if this setting has any effect on newer jython versions anymore
        System.setProperty("python.options.internalTablesImpl", "weak");

        PySystemState engineSys = new PySystemState();
        engineSys.path.append(Py.newString(sphinxSourceDirectory.getAbsolutePath()));
        Py.setSystemState(engineSys);
        LOG.debug("Path: " + engineSys.path.toString());
    }

    /**
     * Execute Python Script using Jython Python Interpreter.
     *
     * @param script to execute
     * @param functionName the function name to which arguments have to be passed.
     * @param args
     * @param resultExpected
     * @return
     */
    private int executePythonScript(String script, String functionName, List<String> args, boolean
            resultExpected) {
        LOG.debug("args: " + Arrays.toString(args.toArray()));
        PythonInterpreter pi = new PythonInterpreter();

        pi.exec("from os import putenv");
        PyObject env = pi.get("putenv");
        env.__call__(Py.java2py("plantuml"), Py.java2py(PLANTUML_JAR));

        pi.exec(script);
        PyObject func = pi.get(functionName);
        PyObject ret = func.__call__(Py.java2py(args));
        int result = 0;
        if (resultExpected) {
            result = (Integer) Py.tojava(ret, Integer.class);
        }

        pi.close();
        pi.cleanup();

        return result;
    }

    /**
     * Execute Sphinx Documentation Builder.
     *
     * @param args
     * @return
     */
    public int runSphinx(List<String> args) {
        String invokeSphinxScript = "from sphinx import build_main";
        String functionName = "build_main";
        return executePythonScript(invokeSphinxScript, functionName, args, true);
    }

    /**
     * Exceute Java Sphinx Documentation Builder.
     *
     * @return
     */
    public int runJavaSphinx(List<String> args) {
        String invokeJavaSphinxScript = "from javasphinx.apidoc import main";
        String functionName = "main";
        return executePythonScript(invokeJavaSphinxScript, functionName, args, false);
    }


    /**
     * Unpack Sphinx zip file.
     *
     * @param sphinxSourceDirectory
     * @throws MavenReportException
     */
    private void unpackSphinx(File sphinxSourceDirectory) throws MavenReportException {
        if (!sphinxSourceDirectory.exists() && !sphinxSourceDirectory.mkdirs()) {
            throw new MavenReportException("Could not generate the temporary directory "
                    + sphinxSourceDirectory.getAbsolutePath() + " for the sphinx sources");
        }
        LOG.debug("Unpacking sphinx to " + sphinxSourceDirectory.getAbsolutePath());
        try {
            ArchiveInputStream input = new ArchiveStreamFactory().createArchiveInputStream("zip",
                    getClass().getResourceAsStream("/sphinx.zip"));
            ArchiveEntry entry = input.getNextEntry();

            while (entry != null) {
                File archiveEntry = new File(sphinxSourceDirectory, entry.getName());
                archiveEntry.getParentFile().mkdirs();
                if (entry.isDirectory()) {
                    archiveEntry.mkdir();
                    entry = input.getNextEntry();
                    continue;
                }
                OutputStream out = new FileOutputStream(archiveEntry);
                IOUtils.copy(input, out);
                out.close();
                entry = input.getNextEntry();
            }
            input.close();
        } catch (Exception ex) {
            throw new MavenReportException("Could not unpack the sphinx source", ex);
        }
    }

    /**
     * Unpack PlantUML jar.
     *
     * @param sphinxSourceDirectory
     * @throws MavenReportException
     */
    private void unpackPlantUml(File sphinxSourceDirectory) throws MavenReportException {
        if (!sphinxSourceDirectory.exists() && !sphinxSourceDirectory.mkdirs()) {
            throw new MavenReportException("Could not generate the temporary directory "
                    + sphinxSourceDirectory.getAbsolutePath() + " for the sphinx sources");
        }
        LOG.debug("Unpacking plantuml jar to " + sphinxSourceDirectory.getAbsolutePath());

        try {
            InputStream input = getClass().getResourceAsStream("/plantuml.8024.jar");
            File outputFile = new File(sphinxSourceDirectory, "plantuml.jar");
            OutputStream out = new FileOutputStream(outputFile);
            IOUtils.copy(input, out);
            out.close();
            input.close();
        } catch (Exception ex) {
            throw new MavenReportException("Could not unpack the plant uml jar file.", ex);
        }
    }

}
