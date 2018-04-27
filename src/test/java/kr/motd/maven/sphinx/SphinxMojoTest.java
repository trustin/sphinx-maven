package kr.motd.maven.sphinx;

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.Before;
import org.junit.Test;

public class SphinxMojoTest extends AbstractMojoTestCase {

	// --------------------------- test prerequisites

	@Before
	@Override
	public void setUp() throws Exception {
		// required for mojo lookups to work
		super.setUp();
	}

	// --------------------------- test cases

	@Test
	public void testExecute() throws Exception {

		File pom = new File(getBasedir(), "src/test/resources/kr/motd/maven/sphinx/test-plugin-mojo.xml");
		assertNotNull(pom);
		assertTrue(pom.exists());

		SphinxMojo mojo = (SphinxMojo) lookupMojo("generate", pom);
		assertNotNull(mojo);

		mojo.execute();
	}

}
