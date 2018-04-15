package kr.motd.maven.sphinx;

import java.io.File;
import java.util.Arrays;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SphinxRunnerTest {

    @ClassRule
    public static final TemporaryFolder tmpDir = new TemporaryFolder();

    @Test
    @Ignore
    public void test() throws Exception {
        final File curDir = new File(".").getCanonicalFile();
        final File srcDir = new File(curDir, "src/site/sphinx").getCanonicalFile();
        final File dstDir = new File(curDir, "target/test-site").getCanonicalFile();
        new SphinxRunner(
                tmpDir.getRoot(),
                new SphinxRunnerLogger() {
                    @Override
                    public void log(String msg) {
                        System.err.println(msg);
                    }
                }).run(srcDir, Arrays.asList(
                        "-t", "tagFoo", "-t", "tagBar",
                        srcDir.toString(), dstDir.toString()));
    }
}
