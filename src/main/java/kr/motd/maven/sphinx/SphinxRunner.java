package kr.motd.maven.sphinx;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import net.sourceforge.plantuml.UmlDiagram;

import kr.motd.maven.os.DetectionException;
import kr.motd.maven.os.Detector;

/**
 * Sphinx Runner.
 */
public final class SphinxRunner {

    public static final String DEFAULT_BINARY_BASE_URL =
            "https://github.com/trustin/sphinx-binary/releases/download/";
    public static final String DEFAULT_BINARY_VERSION = "v0.1.1";

    private static final String VERSION;
    private static final String USER_AGENT;

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
        USER_AGENT = SphinxRunner.class.getSimpleName() + '/' + VERSION;
    }

    private final String binaryBaseUrl;
    private final String binaryVersion;
    private final File binaryCacheDir;
    private final SphinxRunnerLogger logger;
    private final String plantUmlCommand;

    public SphinxRunner(String binaryBaseUrl, String binaryVersion,
                        File binaryCacheDir, SphinxRunnerLogger logger) {

        this.binaryBaseUrl = appendTrailingSlash(requireNonNull(binaryBaseUrl, "binaryBaseUrl"));
        if (!binaryBaseUrl.startsWith("http://") &&
            !binaryBaseUrl.startsWith("https://")) {
            throw new IllegalArgumentException("binaryBaseUrl must start with 'http://' or 'https://':" +
                                               binaryBaseUrl);
        }
        this.binaryVersion = requireNonNull(binaryVersion, "binaryVersion");
        this.binaryCacheDir = requireNonNull(binaryCacheDir, "binaryCacheDir");
        this.logger = requireNonNull(logger, "logger");
        plantUmlCommand = "java -Djava.awt.headless=true -jar " +
                          findPlantUmlJar().getPath().replace("\\", "\\\\");
    }

    private static String appendTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url;
        } else {
            return url + '/';
        }
    }

    public int run(File workingDir, List<String> args) {
        requireNonNull(workingDir, "workingDir");
        requireNonNull(args, "args");
        if (args.isEmpty()) {
            throw new IllegalArgumentException("args is empty.");
        }

        final Path sphinxBinary = downloadSphinxBinary();
        final List<String> fullArgs = new ArrayList<>();
        fullArgs.add(sphinxBinary.toString());
        fullArgs.addAll(args);

        final ProcessBuilder builder = new ProcessBuilder(fullArgs);
        final Map<String, String> env = builder.environment();
        builder.directory(workingDir);
        builder.inheritIO();

        // Set the locale and timezone for consistency.
        env.put("LANG", "en_US.UTF-8");
        env.put("LC_ALL", "en_US.UTF-8");
        env.put("TZ", "UTC");
        // Set the command that runs PlantUML.
        env.put("plantuml", plantUmlCommand);

        try {
            final long startTime = System.nanoTime();
            final Process process = builder.start();
            final int exitCode = process.waitFor();
            logger.log("Sphinx exited with code " + exitCode + ". Took " +
                       TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) + "ms.");
            return exitCode;
        } catch (Exception e) {
            throw new SphinxException("Failed to run Sphinx: " + e, e);
        }
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

    private Path downloadSphinxBinary() {
        final OsDetector osDetector = new OsDetector();
        final String osClassifier = osDetector.classifier();
        final File binaryDir = new File(binaryCacheDir, binaryVersion);
        binaryDir.mkdirs();

        if (!binaryDir.isDirectory()) {
            throw new SphinxException(
                    "failed to create a cache directory: " + binaryDir);
        }

        final String ext = osDetector.isWindows() ? ".exe" : "";
        final String binaryName = "sphinx." + osClassifier + ext;
        final String sha256Name = binaryName + ".sha256";
        final Path binary = new File(binaryDir, binaryName).toPath();
        final Path sha256 = new File(binaryDir, sha256Name).toPath();
        if (Files.exists(binary)) {
            // Downloaded already.
            return binary;
        }

        final URI binaryUri = URI.create(binaryBaseUrl + binaryVersion + '/' + binaryName);
        final URI sha256Uri = URI.create(binaryBaseUrl + binaryVersion + '/' + sha256Name);
        Path tmpBinary = null;
        Path tmpSha256 = null;
        try {
            // Download the binary and sha256 checksum.
            tmpBinary = Files.createTempFile(
                    binary.getParent(), binaryName + '.', ".tmp",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x")));
            download(binaryUri, tmpBinary);
            tmpSha256 = Files.createTempFile(
                    binary.getParent(), sha256Name + '.', ".tmp",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-r--r--")));
            download(sha256Uri, tmpSha256);

            // Make sure the sha256 checksum is valid.
            final List<String> sha256Lines = Files.readAllLines(tmpSha256, StandardCharsets.US_ASCII);
            if (sha256Lines.size() != 1 || !sha256Lines.get(0).matches("^[0-9a-fA-F]{64}(?:\\s.*$|$)")) {
                throw new SphinxException("invalid content: " + sha256Uri);
            }

            final Sha256 digest = new Sha256();
            final byte[] buffer = new byte[65536];
            try (FileInputStream in = new FileInputStream(tmpBinary.toFile())) {
                for (;;) {
                    final int readBytes = in.read(buffer);
                    if (readBytes < 0) {
                        break;
                    }
                    if (readBytes != 0) {
                        digest.update(buffer, 0, readBytes);
                    }
                }
            }

            final byte[] actualSha256Sum = new byte[digest.getDigestLen()];
            digest.finishDigest(actualSha256Sum, 0);
            if (!new BigInteger(sha256Lines.get(0), 16).equals(new BigInteger(1, actualSha256Sum))) {
                throw new SphinxException("mismatching checksum: " + binaryUri);
            }

            // Move the downloaded and verified files to the desired locations.
            Files.move(tmpSha256, sha256,
                       StandardCopyOption.ATOMIC_MOVE,
                       StandardCopyOption.REPLACE_EXISTING);
            tmpSha256 = null;

            Files.move(tmpBinary, binary,
                       StandardCopyOption.ATOMIC_MOVE,
                       StandardCopyOption.REPLACE_EXISTING);
            tmpBinary = null;
            return binary;
        } catch (SphinxException e) {
            throw e;
        } catch (Exception e) {
            throw new SphinxException("failed to download Sphinx binary at: " + binaryUri, e);
        } finally {
            if (tmpSha256 != null) {
                try {
                    Files.deleteIfExists(tmpSha256);
                } catch (IOException e) {
                    // Swallow.
                }
            }
            if (tmpBinary != null) {
                try {
                    Files.deleteIfExists(tmpBinary);
                } catch (IOException e) {
                    // Swallow.
                }
            }
        }
    }

    private void download(URI uri, Path path) {
        URL url;
        try {
            url = uri.toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }

        for (;;) {
            logger.log("Download " + url);
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Accept",
                                        "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                conn.setRequestProperty("Accept-Encoding", "identity");
                conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                conn.setRequestProperty("Cache-Control", "no-cache");
                conn.setRequestProperty("Pragma", "no-cache");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setUseCaches(false);

                switch (conn.getResponseCode()) {
                    case 301:
                    case 302:
                    case 303:
                    case 307:
                    case 308:
                        // Handle redirect.
                        final String location = conn.getHeaderField("Location");
                        if (location == null) {
                            throw new SphinxException(
                                    "missing 'Location' header in a redirect response: " + url);
                        }
                        final URI newUri;
                        try {
                            newUri = URI.create(location);
                        } catch (Exception e) {
                            throw new SphinxException(
                                    "invalid 'Location' header in a redirect response: " + url);
                        }
                        if (!newUri.isAbsolute()) {
                            // It's valid to have a relative URL in a 'Location' header,
                            // but we fail here to simplify the logic.
                            throw new SphinxException(
                                    "relative 'Location' header in a redirect response: " + url);
                        }

                        url = newUri.toURL();
                        continue;
                    case 200:
                        // Handle below.
                        break;
                    default:
                        throw new SphinxException(
                                "unexpected response code '" + conn.getResponseCode() + "':" + url);
                }

                // Download the content into a new file.
                final String contentLength = conn.getHeaderField("Content-Length");
                long lastLogTimeNanos = System.nanoTime();
                try (InputStream in = conn.getInputStream()) {
                    final byte[] buffer = new byte[65536];
                    long progress = 0;
                    try (FileOutputStream out = new FileOutputStream(path.toFile())) {
                        for (;;) {
                            final int readBytes = in.read(buffer);
                            if (readBytes < 0) {
                                break;
                            }
                            if (readBytes != 0) {
                                out.write(buffer, 0, readBytes);
                                progress += readBytes;
                                final long currentTimeNanos = System.nanoTime();
                                if (currentTimeNanos - lastLogTimeNanos >= TimeUnit.SECONDS.toNanos(1)) {
                                    logger.log("Download " + progress + '/' +
                                               (contentLength != null ? contentLength : "?"));
                                    lastLogTimeNanos = currentTimeNanos;
                                }
                            }
                        }
                    }
                }
                return;
            } catch (SphinxException e) {
                throw e;
            } catch (Exception e) {
                throw new SphinxException("failed to download: " + url, e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
    }

    private static class OsDetector extends Detector {

        private String classifier;

        String classifier() {
            if (classifier != null) {
                return classifier;
            }

            final Properties properties = new Properties();
            try {
                detect(properties, Collections.<String>emptyList());
            } catch (DetectionException e) {
                throw new SphinxException(e.getMessage());
            }
            return classifier = properties.getProperty(Detector.DETECTED_CLASSIFIER);
        }

        boolean isWindows() {
            return classifier().startsWith("windows");
        }

        @Override
        protected void log(String s) {}

        @Override
        protected void logProperty(String s, String s1) {}
    }
}
