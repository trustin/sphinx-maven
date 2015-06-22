package org.caltesting.maven.sphinx;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Sphinx Mojo
 *
 * @author tomdz & Bala Sridhar
 * @version June 12, 2015
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.SITE, requiresReports = true)
public class SphinxMojo extends AbstractMojo implements MavenReport {
    /**
     * The maven project object.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * The base directory of the project.
     */
    @Parameter(defaultValue = "${basedir}", required = true, readonly = true)
    private File basedir;

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

    /**
     * Whether Java Sphinx should generate output for all files instead of only the changed ones.
     */
    @Parameter(property = "javaSphinx.force", defaultValue = "false", required = false, alias = "javaSphinxForce")
    private boolean javaSphinxForce;

    /**
     * Whether Java Sphinx should generate verbose output.
     */
    @Parameter(property = "javaSphinx.verbose", defaultValue = "true", required = false, alias = "javaSphinxVerbose")
    private boolean javaSphinxVerbose;

    /**
     * Provide the location where Java Sphinx should copy the java docs created.
     */
    @Parameter(property = "javaSphinx.outputDir", defaultValue = "${sphinx.srcDir}/javadocs/", required = false)
    private String javaSphinxOutputDir;

    /**
     * Provide the list of directories that needs to be scanned to generate javadocs.
     */
    @Parameter(property = "javaSphinx.includeDir", required = false)
    private List<String> javaSphinxIncludeDir;

    /** Sphinx Executor. */
    private final SphinxRunner sphinxRunner;

    /**
     * Default Constructor.
     */
    public SphinxMojo() {
        sphinxRunner = new SphinxRunner();
    }

    @Override
    public void execute() throws MojoExecutionException {
        try {
            sphinxRunner.initEnv(sphinxSourceDirectory, getLog());
        } catch (Exception ex) {
            throw new MojoExecutionException("Failed to extract libraries.", ex);
        }
        runJavaSphinx();
        executeSphinx();
    }

    /**
     * Execute Java Sphinx
     *
     * @throws MojoExecutionException
     */
    private void runJavaSphinx() throws MojoExecutionException {
        try {
            List<String> args = getJavaSphinxCmdLine();
            if (args == null || args.isEmpty()) {
                return;
            }
            getLog().info("Running Java Sphinx, output will be placed in " + javaSphinxOutputDir);

            int result;
            try {
                result = sphinxRunner.runJavaSphinx(args);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new MavenReportException("Could not generate Java Sphinx documentation", ex);
            }
            if (result != 0) {
                throw new MavenReportException("Java Sphinx report generation failed");
            }
        } catch (MavenReportException ex) {
            throw new MojoExecutionException("Failed to run the report", ex);
        }
    }

    /**
     * Execute Sphinx
     *
     * @throws MojoExecutionException
     */
    private void executeSphinx() throws MojoExecutionException{
        try {
            getLog().info("Running Sphinx on " + sourceDirectory.getAbsolutePath() + ", output will be placed in "
                    + outputDirectory.getAbsolutePath());
            List<String> args = getSphinxRunnerCmdLine();
            int result;
            try {
                result = sphinxRunner.runSphinx(args);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new MavenReportException("Could not generate documentation", ex);
            }
            if (result != 0) {
                throw new MavenReportException("Sphinx report generation failed");
            }
        } catch (MavenReportException ex) {
            throw new MojoExecutionException("Failed to run the report", ex);
        }
    }


    @Override
    public void generate(Sink sink, Locale locale) throws MavenReportException {
        try {
            this.execute();
        } catch (Exception ex) {
            throw new MavenReportException("Error Generating Report", ex);
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
        return this.outputDirectory;
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
     *
     * @return
     */
    private List<String> getSphinxRunnerCmdLine() {
        List<String> args = new ArrayList<String>();

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


        args.add(sourceDirectory.getAbsolutePath());
        args.add(outputDirectory.getAbsolutePath() + File.separator + builder);
        return args;
    }

    /**
     * Build the Java Sphinx Command Line Options.
     *
     * @return
     */
    private List<String> getJavaSphinxCmdLine() {
        // If the options are not specified then allow the process to continue.
	if (javaSphinxOutputDir == null || javaSphinxIncludeDir == null || javaSphinxIncludeDir.isEmpty()) {
		return null;
	}
	List<String> javaSphinxArgs = new ArrayList<String>();

        if (javaSphinxVerbose) {
            javaSphinxArgs.add("-v");
        }

        if (javaSphinxForce) {
            javaSphinxArgs.add("-f");
        } else {
            javaSphinxArgs.add("-u");
        }

        javaSphinxArgs.add("-o");
        javaSphinxArgs.add(javaSphinxOutputDir);

        int count = 0;
        for (String includeDir : javaSphinxIncludeDir) {
            if (count > 0) {
                javaSphinxArgs.add("-I");
            }
            javaSphinxArgs.add(includeDir);
            count++;
        }
        return javaSphinxArgs;
    }
}
