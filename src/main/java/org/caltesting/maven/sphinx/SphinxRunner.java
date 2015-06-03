package org.caltesting.maven.sphinx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.reporting.MavenReportException;
import org.python.core.Py;
import org.python.core.PySystemState;

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
     * @param verbose
     * @return
     * @throws Exception
     */
    public int runSphinx(String[] args, File sphinxSourceDirectory, boolean verbose) throws Exception {
        unpackSphinx(sphinxSourceDirectory, verbose);
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

        ScriptEngine engine = new ScriptEngineManager().getEngineByName("python");

        engine.put("args", args);
        engine.eval("import sphinx");
        return (Integer) engine.eval("sphinx.main(args)");
    }

    /**
     * Unpack Sphinx Jar file.
     * @param sphinxSourceDirectory
     * @param verbose
     * @throws MavenReportException
     */
    private void unpackSphinx(File sphinxSourceDirectory, boolean verbose) throws MavenReportException {
        if (!sphinxSourceDirectory.exists() && !sphinxSourceDirectory.mkdirs()) {
            throw new MavenReportException("Could not generate the temporary directory "
                    + sphinxSourceDirectory.getAbsolutePath() + " for the sphinx sources");
        }

        if (verbose) {
            log.info("Unpacking sphinx to " + sphinxSourceDirectory.getAbsolutePath());
        }
        try {
            ArchiveInputStream input = new ArchiveStreamFactory().createArchiveInputStream("jar",
                    SphinxMojo.class.getResourceAsStream("/sphinx.jar"));
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

}
