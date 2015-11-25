package kr.motd.maven.sphinx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
     *
     * @throws MojoExecutionException
     */
    private void executeSphinx(SphinxRunner sphinxRunner) throws MojoExecutionException{
        try {
            getLog().info("Running Sphinx on " + sourceDirectory + ", output will be placed in "
                    + outputDirectory);
            List<String> args = getSphinxRunnerCmdLine();
            if (sphinxRunner.runSphinx(args) != 0) {
                throw new MavenReportException("Sphinx report generation failed");
            }
        } catch (Exception t) {
            throw new MojoExecutionException("Failed to run the report", t);
        }
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
