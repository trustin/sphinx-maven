package kr.motd.maven.sphinx;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class SphinxRunnerTest {
    @Test
    public void test() throws Exception {
        final File curDir = new File(".").getCanonicalFile();
        final File srcDir = new File(curDir, "src/site/sphinx").getCanonicalFile();
        final File dstDir = new File(curDir, "target/test-site").getCanonicalFile();
        final Map<String, String> env = new HashMap<>();
        env.put("ENV_FOO", "bar");
        new SphinxRunner(
                SphinxRunner.DEFAULT_BINARY_URL,
                new File(System.getProperty("user.home") + "/.m2/repository/kr/motd/maven/sphinx-binary").getCanonicalFile(),
                env, null, new SphinxRunnerLogger() {
                    @Override
                    public void log(String msg) {
                        System.err.println(msg);
                    }
                }).run(srcDir, Arrays.asList(
                        "-t", "tagFoo", "-t", "tagBar",
                        srcDir.toString(), dstDir.toString()));
    }
}
