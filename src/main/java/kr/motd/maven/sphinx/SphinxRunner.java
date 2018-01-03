package kr.motd.maven.sphinx;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import net.sourceforge.plantuml.UmlDiagram;

/**
 * Sphinx Runner.
 */
public final class SphinxRunner implements AutoCloseable {

    private static final String VERSION;
    private static final String DIST_PREFIX =
            SphinxRunner.class.getPackage().getName().replace('.', '/') + "/dist/";

    static {
        final Properties versionProps = new Properties();
        try {
            versionProps.load(SphinxRunner.class.getResourceAsStream("version.properties"));
        } catch (IOException e) {
            throw new IOError(e);
        }
        VERSION = versionProps.getProperty("version");
        if (VERSION == null) {
            throw new IllegalStateException("cannot determine the plugin version");
        }
    }

    private final File sphinxSourceDirectory;
    private final SphinxRunnerLogger logger;

    private final PySystemState sysState;
    private final PythonInterpreter interpreter;
    private final PyObject mainFunc;
    private boolean run;

    public SphinxRunner(File sphinxSourceDirectory, SphinxRunnerLogger logger) {
        this.sphinxSourceDirectory = requireNonNull(sphinxSourceDirectory, "sphinxSourceDirectory");
        this.logger = requireNonNull(logger, "logger");

        final String plantUmlCommand = "java -jar " + findPlantUmlJar().getPath().replace("\\", "\\\\");
        final File sphinxDirectory = extractSphinx();

        PySystemState sysState = null;
        PythonInterpreter interpreter = null;
        boolean success = false;
        try {
            final long startTime = System.nanoTime();
            logger.log("Sphinx directory: " + sphinxDirectory);
            logger.log("PlantUML command: " + plantUmlCommand);
            logger.log("Initializing the interpreter ..");

            // use headless mode for AWT (prevent "Launcher" app on Mac OS X)
            System.setProperty("java.awt.headless", "true");

            // this setting supposedly allows GCing of jython-generated classes but I'm
            // not sure if this setting has any effect on newer jython versions anymore
            System.setProperty("python.options.internalTablesImpl", "weak");

            sysState = new PySystemState();
            sysState.path.append(Py.newString(sphinxDirectory.getPath()));
            logger.log("Jython path: " + sysState.path);

            // Set some necessary environment variables.
            interpreter = new PythonInterpreter(null, sysState);
            interpreter.exec("from os import putenv");
            PyObject env = interpreter.get("putenv");

            // Set the locale for consistency.
            env.__call__(Py.java2py("LANG"), Py.java2py("en_US.UTF-8"));
            env.__call__(Py.java2py("LC_ALL"), Py.java2py("en_US.UTF-8"));

            // Set the command that runs PlantUML.
            env.__call__(Py.java2py("plantuml"), Py.java2py(plantUmlCommand));

            // babel/localtime/_unix.py attempts to use os.readlink() which is
            // unavailable in some OSes. Setting the 'TZ' environment variable
            // works around this problem.
            env.__call__(Py.java2py("TZ"), Py.java2py("UTC"));

            // Import Sphinx to preload it.
            interpreter.exec("from sphinx.application import Sphinx as PreloadedSphinx");

            // Prepare the main function.
            interpreter.exec("from sphinx import build_main");
            mainFunc = interpreter.get("build_main");
            logger.log("Initialized the interpreter. Took " +
                       TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) + "ms.");

            this.sysState = sysState;
            this.interpreter = interpreter;
            success = true;
        } catch (Exception e) {
            throw new SphinxException("Failed to initialize Sphinx: " + e, e);
        } finally {
            if (!success) {
                if (interpreter != null) {
                    interpreter.close();
                }
                if (sysState != null) {
                    sysState.close();
                }
            }
        }
    }

    public int run(List<String> args) {
        requireNonNull(args, "args");
        if (args.isEmpty()) {
            throw new IllegalArgumentException("args is empty.");
        }

        if (run) {
            throw new IllegalStateException("run already");
        }

        run = true;
        try {
            final long startTime = System.nanoTime();
            final PyObject ret = mainFunc.__call__(Py.java2py(args));
            final int exitCode = Py.tojava(ret, Integer.class);
            logger.log("Sphinx exited with code " + exitCode + ". Took " +
                       TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) + "ms.");
            return exitCode;
        } catch (Exception e) {
            throw new SphinxException("Failed to run Sphinx: " + e, e);
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        interpreter.close();
        sysState.close();
    }

    /**
     * Unpack Sphinx and its dependencies into the file system.
     */
    private File extractSphinx() {
        final File sphinxCacheDir = sphinxSourceDirectory;
        sphinxCacheDir.mkdirs();

        final File sphinxDir = new File(sphinxCacheDir, VERSION);
        if (sphinxDir.isDirectory()) {
            return sphinxDir;
        }

        try {
            final File tmpDir = Files.createTempDirectory(sphinxCacheDir.toPath(), VERSION + ".tmp.").toFile();
            logger.log("Extracting Sphinx into: " + tmpDir);

            final JarFile jar = new JarFile(findPluginJar(), false);
            final byte[] buf = new byte[65536];
            for (Enumeration<JarEntry> i = jar.entries(); i.hasMoreElements(); ) {
                final JarEntry e = i.nextElement();
                if (!e.getName().startsWith(DIST_PREFIX)) {
                    continue;
                }

                final File f = new File(tmpDir + File.separator +
                                        e.getName().substring(DIST_PREFIX.length()));

                if (e.isDirectory()) {
                    if (!f.mkdirs() && !f.exists()) {
                        throw new SphinxException("Failed to create a directory: " + f);
                    }
                    continue;
                }

                try (final InputStream in = jar.getInputStream(e);
                     final OutputStream out = new FileOutputStream(f)) {
                    for (;;) {
                        int readBytes = in.read(buf);
                        if (readBytes < 0) {
                            break;
                        }
                        out.write(buf, 0, readBytes);
                    }
                }
            }

            if (!tmpDir.renameTo(sphinxDir)) {
                throw new SphinxException("Failed to rename: " + tmpDir + " -> " + sphinxDir.getName());
            }

            return sphinxDir;
        } catch (SphinxException e) {
            throw e;
        } catch (Exception e) {
            throw new SphinxException("Failed to extract Sphinx: " + e, e);
        }
    }

    private File findPluginJar() {
        return findJar(SphinxRunner.class, "the plugin JAR");
    }

    private File findPlantUmlJar() {
        return findJar(UmlDiagram.class, "PlantUML JAR");
    }

    private File findJar(Class<?> type, String name) {
        final CodeSource codeSource = type.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            throw new SphinxException(
                    "failed to get the location of " + name + " (CodeSource not available)");
        }

        final URL url = codeSource.getLocation();
        logger.log(name + ": " + url);
        if (!"file".equals(url.getProtocol()) || !url.getPath().toLowerCase(Locale.US).endsWith(".jar")) {
            throw new SphinxException(
                    "failed to get the location of " + name + " (unexpected URL: " + url + ')');
        }

        File f;
        try {
            f = new File(url.toURI());
        } catch (URISyntaxException ignored) {
            f = new File(url.getPath());
        }

        return f;
    }
}
