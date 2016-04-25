package kr.motd.maven.sphinx;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;

/**
 * Sphinx Mojo
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.SITE, requiresReports = true)
public class SphinxMojo extends AbstractMojo implements MavenReport {

    private static final String[] CRUFTS = {
            "css/maven-base.css",
            "css/maven-theme.css",
            "css/print.css",
            "css/site.css",
            "css",
            "images/logos/build-by-maven-black.png",
            "images/logos/build-by-maven-white.png",
            "images/logos/maven-feather.png",
            "images/logos",
            "images/collapsed.gif",
            "images/expanded.gif",
            "images/external.png",
            "images/icon_error_sml.gif",
            "images/icon_info_sml.gif",
            "images/icon_success_sml.gif",
            "images/icon_warning_sml.gif",
            "images/newwindow.png",
            "images",
    };

    /**
     * The directory containing the sphinx doc source.
     */
    @Parameter(property = "sphinx.srcDir", defaultValue = "${basedir}/src/site/sphinx", required = true)
    private File sourceDirectory;

    /**
     * Directory where reports will go.
     */
    @Parameter(defaultValue = "${project.reporting.outputDirectory}", required = true, readonly = true)
    private File outputDirectory;
    
    /**
     * The directory for sphinx' source.
     */
    @Parameter(property = "sphinx.sphinxSrcDir", defaultValue = "${project.build.directory}/sphinx", required = true, readonly = true)
    private File sphinxSourceDirectory;

    /**
     * The builder to use. See <a href="http://sphinx.pocoo.org/man/sphinx-build.html?highlight=command%20line">sphinx-build</a>
     * for a list of supported builders.
     */
    @Parameter(property = "sphinx.builder", required = true, alias = "builder", defaultValue = "html")
    private String builder;

    /**
     * The <a href="http://sphinx.pocoo.org/markup/misc.html#tags">tags</a> to pass to the sphinx build.
     */
    @Parameter(property = "sphinx.tags", alias = "tags")
    private List<String> tags;

    /**
     * Whether Sphinx should generate verbose output.
     */
    @Parameter(property = "sphinx.verbose", defaultValue = "true", required = true, alias = "verbose")
    private boolean verbose;

    /**
     * Whether Sphinx should treat warnings as errors.
     */
    @Parameter(property = "sphinx.warningAsErrors", defaultValue = "false", required = true, alias = "warningAsErrors")
    private boolean warningsAsErrors;

    /**
     * Whether Sphinx should generate output for all files instead of only the changed ones.
     */
    @Parameter(property = "sphinx.force", defaultValue = "false", required = true, alias = "force")
    private boolean force;

    @Override
    public void execute() throws MojoExecutionException {
        sourceDirectory = canonicalize(sourceDirectory);
        outputDirectory = canonicalize(outputDirectory);
        sphinxSourceDirectory = canonicalize(sphinxSourceDirectory);

        final SphinxRunner sphinxRunner;
        try {
            sphinxRunner = new SphinxRunner(sphinxSourceDirectory, getLog());
        } catch (Exception ex) {
            throw new MojoExecutionException("Failed to extract libraries.", ex);
        }

        try {
            executeSphinx(sphinxRunner);
            convertLineSeparators();
            deleteCruft();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to run the report", e);
        } finally {
            sphinxRunner.destroy();
        }
    }

    private static File canonicalize(File directory) throws MojoExecutionException {
        if (directory == null) {
            return null;
        }

        try {
            directory.mkdirs();
            return directory.getCanonicalFile();
        } catch (IOException e) {
            throw new MojoExecutionException("failed to create a directory: " + directory, e);
        }
    }

    /**
     * Execute Sphinx
     */
    private void executeSphinx(SphinxRunner sphinxRunner) throws MavenReportException {
        getLog().info("Running Sphinx on " + sourceDirectory + ", output will be placed in "
                      + outputDirectory);
        List<String> args = getSphinxRunnerCmdLine();
        if (sphinxRunner.runSphinx(args) != 0) {
            throw new MavenReportException("Sphinx report generation failed");
        }
    }

    /**
     * Replaces the line separators of the generated text files with the platform default line separator.
     */
    private void convertLineSeparators() throws IOException {
        convertLineSeparators(outputDirectory);
    }

    private static void convertLineSeparators(File dir) throws IOException {
        if (!dir.isDirectory()) {
            return;
        }

        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                convertLineSeparators(f);
            } else if (isTextFile(f)) {
                convertLineSeparators(f, System.lineSeparator());
            }
        }
    }

    private static void convertLineSeparators(File f, String lineSeparatorStr) throws IOException {
        assert f.length() <= Integer.MAX_VALUE : "text file larger than 2 GiB";

        final byte[] content = new byte[(int) f.length()];
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            for (int i = 0; i < content.length;) {
                final int readBytes = raf.read(content, i, content.length - i);
                if (readBytes < 0) {
                    throw new IOException("file size has been changed during processing: " + f);
                }

                i += readBytes;
            }
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream(content.length);
        final byte[] lineSeparator = lineSeparatorStr.getBytes("US-ASCII");
        byte lastByte = 0;

        for (final byte b : content) {
            if (b == 0) {
                // Maybe not a text file or UTF-16; give up.
                return;
            }

            if (b == '\n') {
                out.write(lineSeparator);
            } else {
                if (lastByte == '\r') {
                    out.write(lineSeparator);
                }
                if (b != '\r') {
                    out.write(b);
                }
            }
            lastByte = b;
        }

        if (lastByte == '\r') {
            out.write(lineSeparator);
        }

        final byte[] newContent = out.toByteArray();
        if (Arrays.equals(content, newContent)) {
            return;
        }

        try (FileOutputStream fout = new FileOutputStream(f)) {
            fout.write(newContent);
        }
    }

    private static boolean isTextFile(File f) {
        if (!f.isFile()) {
            return false;
        }

        final String name = f.getName();
        final int lastDotIdx = name.lastIndexOf('.');
        if (lastDotIdx < 0) {
            return false;
        }

        final String extension = name.substring(lastDotIdx + 1);
        switch (extension) {
            case "buildinfo":
            case "html":
            case "js":
            case "svg":
            case "txt":
            case "xml":
                return true;
            case "map":
                return name.endsWith(".css.map") || name.endsWith(".js.map");
        }

        return false;
    }

    /**
     * Deletes the crufts generated by maven-site-plugin.
     */
    private void deleteCruft() {
        final File outputDirectory = this.outputDirectory;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                for (String c : CRUFTS) {
                    new File(outputDirectory, c.replace('/', File.separatorChar)).delete();
                }
            }
        });
    }

    @Override
    public void generate(
            @SuppressWarnings("deprecation") Sink sink, Locale locale) throws MavenReportException {

        try {
            execute();
        } catch (Exception e) {
            throw new MavenReportException("Error generating a Sphinx report:", e);
        }
    }

    @Override
    public String getOutputName() {
        return "Python-Sphinx";
    }

    @Override
    public String getCategoryName() {
        return "Documentation";
    }

    @Override
    public String getName(Locale locale) {
        return "Sphinx-Docs";
    }

    @Override
    public String getDescription(Locale locale) {
        return "Documentation using Python Sphinx Package";
    }

    @Override
    public void setReportOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    @Override
    public File getReportOutputDirectory() {
        return outputDirectory;
    }

    @Override
    public boolean isExternalReport() {
        return true;
    }

    @Override
    public boolean canGenerateReport() {
        return true;
    }

    /**
     * Build the Sphinx Command line options.
     */
    private List<String> getSphinxRunnerCmdLine() {
        List<String> args = new ArrayList<>();

        if (verbose) {
            args.add("-v");
        } else {
            args.add("-Q");
        }

        if (warningsAsErrors) {
            args.add("-W");
        }

        if (force) {
            args.add("-a");
            args.add("-E");
        }

        if (tags != null && !tags.isEmpty()) {
            for (String tag : tags) {
                args.add("-t");
                args.add(tag);
            }
        }

        args.add("-n");

        args.add("-b");
        args.add(builder);

        args.add(sourceDirectory.getPath());
        args.add(outputDirectory.getPath());

        return args;
    }
}
