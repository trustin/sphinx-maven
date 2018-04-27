package kr.motd.maven.stubs;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.Proxy;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;

public class MavenSessionStub extends MavenSession {

	// --------------------------- constructor

	public MavenSessionStub() throws PlexusContainerException {
		super(createContainer(), createRepositorySession(), createRequest(), createResponse());
		initSessionStub();
	}

	// --------------------------- static implementation

	protected static PlexusContainer createContainer() throws PlexusContainerException {
		return new DefaultPlexusContainer();
	}

	protected static RepositorySystemSession createRepositorySession() {
		DefaultRepositorySystemSession repoSession = new DefaultRepositorySystemSession();
		// repoSession.setLocalRepositoryManager(LegacyLocalRepositoryManager
		// .overlay(new StubArtifactRepository("target/local-repo"), repoSession, null));
		return repoSession;
	}

	protected static MavenExecutionRequest createRequest() {
		return new DefaultMavenExecutionRequest();
	}

	protected static MavenExecutionResult createResponse() {
		return new DefaultMavenExecutionResult();
	}

	// --------------------------- stub implementation

	protected void initSessionStub() {
		Proxy proxy = new Proxy();
		proxy.setId("default");
		proxy.setActive(true);
		proxy.setProtocol("http");
		proxy.setHost("web-proxy.bbn.hpecorp.net");
		proxy.setPort(8080);
		getSettings().addProxy(proxy);
	}

}
