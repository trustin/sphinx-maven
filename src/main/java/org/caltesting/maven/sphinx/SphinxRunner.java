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

import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * Sphinx Runner.
 *
 * @author Bala Sridhar & tomdz
 * @version
 */
public class SphinxRunner {

    /** Mojo Logger. */
    private final Log log;

    /**
     * Default Constructor.
     * @param log
     */
    public SphinxRunner(Log log) {
        this.log = log;
    }

    /**
     * Execute Sphinx Documentation Builder.
     * @param args
     * @param sphinxSourceDirectory
     * @return
     * @throws Exception
     */
    public int runSphinx(List<String> args, File sphinxSourceDirectory) throws Exception {
        unpackSphinx(sphinxSourceDirectory);
        unpackPlantUml(sphinxSourceDirectory);
        // use headless mode for AWT (prevent "Launcher" app on Mac OS X)
        System.setProperty("java.awt.headless", "true");

        // this setting supposedly allows GCing of jython-generated classes but I'm
        // not sure if this setting has any effect on newer jython versions anymore
        System.setProperty("python.options.internalTablesImpl", "weak");

        if (sphinxSourceDirectory == null) {
            throw new IllegalArgumentException("sphinxSourceDirectory is empty.");
        }

        PySystemState engineSys = new PySystemState();
        engineSys.path.append(Py.newString(sphinxSourceDirectory.getAbsolutePath()));
        Py.setSystemState(engineSys);

        log.debug("Path: " + engineSys.path.toString());
        log.debug("args: " + Arrays.toString(args.toArray()));
        String plantumlExec = "plantuml = 'java -jar " + sphinxSourceDirectory.getAbsolutePath() + "/plantuml.jar'";
        log.debug(plantumlExec);

        PythonInterpreter pi = new PythonInterpreter();
        pi.exec(Py.newString(plantumlExec));
        pi.exec("from sphinx import main");
        PyObject sphinx = pi.get("main");
        PyObject ret = sphinx.__call__(Py.java2py(args));
        return (Integer) Py.tojava(ret, Integer.class);
    }

    /**
     * Unpack Sphinx Jar file.
     * @param sphinxSourceDirectory
     * @throws MavenReportException
     */
    private void unpackSphinx(File sphinxSourceDirectory) throws MavenReportException {
        if (!sphinxSourceDirectory.exists() && !sphinxSourceDirectory.mkdirs()) {
            throw new MavenReportException("Could not generate the temporary directory "
                    + sphinxSourceDirectory.getAbsolutePath() + " for the sphinx sources");
        }
        log.debug("Unpacking sphinx to " + sphinxSourceDirectory.getAbsolutePath());
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
        log.debug("Unpacking plantuml jar to " + sphinxSourceDirectory.getAbsolutePath());

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
