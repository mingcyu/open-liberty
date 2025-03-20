package com.ibm.ws.tests.anno.caching;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import spring.test.init.jar.JarInit;
import spring.test.init.war.WebInit;

@RunWith(FATRunner.class)
public class AnnotationScanInJarTest extends FATServletClient {

	public static final String APP_NAME = "springTest";
	public static final String SERVER_NAME = "springTest_server";

	@Server(SERVER_NAME)
	public static LibertyServer server;

	@ClassRule
	public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME,
			EERepeatActions.EE10,
			EERepeatActions.EE11);

	@BeforeClass
	public static void setUp() throws Exception {

		WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
				.addPackages(true, WebInit.class.getPackage())		
				.addAsWebInfResource(new StringAsset("logging.level.org.springframework.context.annotation=DEBUG"), "application.properties");

		String userDir = System.getProperty("user.dir"); //ends with: com.ibm.ws.anno_fat/build/libs/autoFVT
		String springDir = userDir + "/publish/shared/resources/spring/";


		
		JavaArchive jar = ShrinkWrap.create(JavaArchive.class, APP_NAME + ".jar")
				.addPackages(true, JarInit.class.getPackage());

		EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear")
				.addAsModule(war)
				.addAsLibraries(jar);
		
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(springDir), "*.jar")) {
			for (Path path : stream) {
				if (!Files.isDirectory(path)) {
					ear.addAsLibraries(path.toFile());
				}
			}
		}

		ShrinkHelper.exportDropinAppToServer(server, ear, DeployOptions.SERVER_ONLY);

		server.startServer();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		server.stopServer();
	}


	@Test
	public void testSpringAnnotationFoundInwar() throws Exception {
		List<String> matching = server.findStringsInLogsAndTraceUsingMark("AnnotationScanInJarTest test output");
		
		String allOutput = String.join(" : ", matching);
		
		assertTrue("Did not find \"onStartup method in war file\" in " + allOutput, allOutput.contains("onStartup method in war file"));
		assertTrue("Did not find \"onStartup method found via jar file\" in " + allOutput, allOutput.contains("onStartup method found via jar file"));
	}


}
