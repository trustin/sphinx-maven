package kr.motd.maven.sphinx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.settings.Proxy;
import org.codehaus.doxia.sink.Sink;

/**
 * Sphinx Mojo
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.SITE, requiresReports = true)
public class SphinxMojo extends AbstractMojo implements MavenReport {

    @Parameter( defaultValue = "${session}", readonly = true )
    private MavenSession session;

    /**
     * Boolean to keep default site and make Sphinx doc a project report
     */
    @Parameter(property = "sphinx.asReport", defaultValue = "false", alias = "asReport")
    private boolean asReport;

    /**
     * Name of the report in "Project reports" section (default Maven site)
     */
    @Parameter(property = "sphinx.name", defaultValue = "Sphinx-Docs", alias = "name")
    private String name;

    /**
     * Description of the report in "Project reports" section (default Maven site)
     */
    @Parameter(property = "sphinx.description", defaultValue = "Documentation using Python Sphinx Package", alias = "description")
    private String description;

    /**
     * Sub-directory to store Sphinx output (used only if {@code asReport} is true)
     */
    private static final String sphinxSiteSubDirectory = "sphinx";

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
     * The base URL of the Sphinx binary, which will be used when downloading the Sphinx binary; must start
     * with {@code http://} or {@code https://}.
     */
    @Parameter(property = "sphinx.binUrl", readonly = true)
    private String binaryUrl = SphinxRunner.DEFAULT_BINARY_URL;

    /**
     * The directory for Sphinx binary cache.
     */
    @Parameter(property = "sphinx.binCacheDir", defaultValue = "${settings.localRepository}/kr/motd/maven/sphinx-binary", required = true, readonly = true)
    private File binaryCacheDir;

    /**
     * The environment variables to set when launching Sphinx.
     */
    @Parameter(property = "sphinx.env", readonly = true)
    private Map<String, String> environments = Collections.emptyMap();

    /**
     * The path to Graphviz {@code dot} binary.
     */
    @Parameter(property = "sphinx.dotBin", readonly = true)
    private String dotBinary;

    /**
     * The builder to use. See <a href="http://www.sphinx-doc.org/en/master/builders.html">Available builders</a>
     * for a list of supported builders.
     */
    @Parameter(property = "sphinx.builder", required = true, alias = "builder", defaultValue = "html")
    private String builder;

    /**
     * The <a href="http://www.sphinx-doc.org/en/master/usage/restructuredtext/directives.html#tags">tags</a>
     * to pass to Sphinx.
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
     * Whether Sphinx execution should be skipped.
     */
    @Parameter(property = "sphinx.skip", defaultValue = "false", required = true, alias = "skip")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping Sphinx execution.");
            return;
        }

        sourceDirectory = canonicalize(sourceDirectory);
        outputDirectory = canonicalize(outputDirectory);
        binaryCacheDir = canonicalize(binaryCacheDir);

        // to avoid Maven overriding resulting index.html, update index.rst to force re-building of index
        if (isHtmlReport()) {
            new File(sourceDirectory.getPath() + "/index.rst").setLastModified(System.currentTimeMillis());
        }

        configureProxy();

        try {
            final SphinxRunner sphinxRunner = new SphinxRunner(
                    binaryUrl, binaryCacheDir, environments,
                    "".equals(dotBinary) ? null : dotBinary,
                    new SphinxRunnerLogger() {
                        @Override
                        public void log(String msg) {
                            getLog().info(msg);
                        }
                    });

            getLog().info("Running Sphinx; output will be placed in " + outputDirectory);
            final List<String> args = getSphinxRunnerCmdLine();
            if (sphinxRunner.run(sourceDirectory, args) != 0) {
                throw new MavenReportException("Sphinx report generation failed");
            }

            SphinxUtil.convertLineSeparators(outputDirectory);
            // only delete crufts if Maven site is overridden (default behavior)
            if (!asReport) {
                deleteCruft();
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to run the report", e);
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

    private void configureProxy() {
        if (session != null) {
            final Proxy proxy = session.getSettings().getActiveProxy();
            if (proxy != null && proxy.getProtocol() != null &&
                proxy.getProtocol().toLowerCase().startsWith("http")) {
                for (String protocol : Arrays.asList("http", "https")) {
                    System.setProperty(protocol + ".proxyHost", proxy.getHost());
                    System.setProperty(protocol + ".proxyPort", String.valueOf(proxy.getPort()));
                    if (proxy.getUsername() != null && proxy.getPassword() != null) {
                        System.setProperty(protocol + ".proxyUser", proxy.getUsername());
                        System.setProperty(protocol + ".proxyPassword", proxy.getPassword());
                    }
                    if (proxy.getNonProxyHosts() != null) {
                        System.setProperty(protocol + ".nonProxyHosts", proxy.getNonProxyHosts());
                    }
                }
                if (System.getProperty("jdk.http.auth.tunneling.disabledSchemes") == null) {
                    System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
                }
                if (System.getProperty("https.protocols") == null) {
                    System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
                }
            }
        }
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
        } catch (SphinxException e) {
            final MavenReportException cause = new MavenReportException(e.getMessage());
            if (e.getCause() != null) {
                cause.initCause(e.getCause());
            }
            throw cause;
        } catch (Exception e) {
            throw new MavenReportException("Error generating a Sphinx report:", e);
        }
    }

    private boolean isHtmlReport() {
        return asReport && "html".equals(builder);
    }

    @Override
    public String getOutputName() {
        if (isHtmlReport()) {
            return sphinxSiteSubDirectory + "/index";   // if report, clicking on report will lead to sphinx/index.html
        }
        return "Python-Sphinx";
    }

    @Override
    public String getCategoryName() {
        return MavenReport.CATEGORY_PROJECT_REPORTS;
    }

    @Override
    public String getName(Locale locale) {
        return name;
    }

    @Override
    public String getDescription(Locale locale) {
        return description;
    }

    @Override
    public void setReportOutputDirectory(File outputDirectory) {
        // if documentation is generated as a report
        if (asReport) {
            // output sphinx doc to outputDirectory/sphinx instead of outputDirectory
            this.outputDirectory = new File(outputDirectory.getPath() + '/' + sphinxSiteSubDirectory);
        } else {
            this.outputDirectory = outputDirectory;
        }
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
