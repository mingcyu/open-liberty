package com.ibm.ws.tests.anno.caching;

import static org.junit.Assert.assertTrue;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import spring.test.init.manifest.ManifestInit;
import spring.test.init.sharedlib.SharedLibInit;
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
                .addAsManifestResource(new StringAsset("Manifest-Version: 1.0" + System.lineSeparator() +
                		"Class-Path: manifestLib.jar" + System.lineSeparator()), "MANIFEST.MF") //The Class-Path will not be included without that trailing newline. See https://docs.oracle.com/javase/tutorial/deployment/jar/modman.html
				.addAsWebInfResource(new StringAsset("logging.level.org.springframework.context.annotation=DEBUG"), "application.properties");

		String userDir = System.getProperty("user.dir"); //ends with: com.ibm.ws.anno_fat/build/libs/autoFVT
		String springDir = userDir + "/publish/shared/resources/spring/";
		
		JavaArchive jar = ShrinkWrap.create(JavaArchive.class, APP_NAME + ".jar")
				.addPackages(true, JarInit.class.getPackage());
		
		JavaArchive sharedLib = ShrinkWrap.create(JavaArchive.class, "sharedLib.jar")
				.addPackages(true, SharedLibInit.class.getPackage());
		
       JavaArchive maifestJar = ShrinkWrap.create(JavaArchive.class, "manifestLib.jar")
                   .addPackage(ManifestInit.class.getPackage());

		EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear")
				.addAsModule(war)
				.addAsModule(maifestJar)
				.addAsLibraries(jar);
		
		//Copy spring libs into the server
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(springDir), "*.jar")) {
			for (Path path : stream) {
				if (!Files.isDirectory(path)) {
					//Putting the spring libs in the ear file works. Putting them as a shared library reference means
					//none of the initalizers will fire.

					//However I think this worked fine if I had shared libs enabled. Possibly because I was also scanning
					//Shared libs as web fragments. Leaving this note here so I can start investigating from where I left of
					//if we ever enable scanning for shared libs
					ear.addAsLibraries(path.toFile());
				}
			}
		}
		
		ShrinkHelper.exportToServer(server, "libs", sharedLib);
		ShrinkHelper.exportAppToServer(server, ear, DeployOptions.SERVER_ONLY);

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
		assertTrue("Did not find \"onStartup method found via manifest lib file\" in " + allOutput, allOutput.contains("onStartup method found via manifest lib file"));
		
		//Scanning annotations in shared libs for web fragmnet related annotations is not currently supported
		//assertTrue("Did not find \"onStartup method found via shared library file\" in " + allOutput, allOutput.contains("onStartup method found via shared library file"));
		
		//Since it checks both logs and traces it will find each twice.
		assertTrue("Found too many entries in the logs. Expected 6 Found " + matching.size() + " output: " + allOutput, matching.size() == 6);
	}


}
