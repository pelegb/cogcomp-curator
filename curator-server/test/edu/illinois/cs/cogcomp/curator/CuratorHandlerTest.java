package edu.illinois.cs.cogcomp.curator;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.thrift.TException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.illinois.cs.cogcomp.thrift.base.AnnotationFailedException;
import edu.illinois.cs.cogcomp.thrift.base.Labeling;
import edu.illinois.cs.cogcomp.thrift.base.ServiceUnavailableException;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.thrift.curator.Curator;
import edu.illinois.cs.cogcomp.thrift.curator.Record;

public class CuratorHandlerTest {

	private static final String propsConfig = "# uncomment if you want to run a slave curator\n"
			+ "# point servers.master to the master curator\n"
			+ "#curator.slave = true\n"
			+ "#servers.master = hostname:port\n"
			+ "# the backend for the curator's archive\n"
			+ "archive = edu.illinois.cs.cogcomp.archive.DatabaseArchive\n"
			+ "# client timeout in seconds (how long to wait before connection to an annotator\n"
			+ "# times out)\n"
			+ "client.timeout = 60\n"
			+ "# how often to print report line in mins\n"
			+ "curator.reporttime = 30\n"
			+ "curator.versiontime = 30\n"
			+ "# change this line to true if you want write access (storeRecord)\n"
			+ "curator.writeaccess = false\n";

	private static final String annosConfig = "<curator-annotators>\n"
			+ "    <annotator>\n"
			+ "        <type>multilabeler</type>\n"
			+ "        <field>sentences</field>\n"
			+ "        <field>tokens</field>\n"
			+ "        <local>edu.illinois.cs.cogcomp.annotation.handler.IllinoisTokenizerHandler</local>\n"
			+ "    </annotator>\n" + "</curator-annotators>\n";

	private static final String archsConfig = "# where to store the database\n"
			+ "database.url = localhost\n"
			+ "# leave this as curator\n"
			+ "database.user = curator\n"
			+ "database.password = curator\n"
			+ "# how often to print database report in mins\n"
			+ "database.reporttime = 30\n"
			+ "# how often to perform database maintenance in mins\n"
			+ "# database maintenance searches for records that have not been accessed within\n"
			+ "# the timeframe specified by database.expiretime and removes them.\n"
			+ "database.maintenancetime = 180\n"
			+ "# force access updates once we have n unique records accessed without update\n"
			+ "# this stops the maintenance from taking so long\n"
			+ "database.updatecount = 5000\n"
			+ "# expiretime in days, delete records after n days of not being accessed\n"
			+ "database.expiretime = 300\n";

	private static File propsConfigFile;
	private static File annosConfigFile;
	private static File archiveConfigFile;

	private Curator.Iface handler = null;

	@BeforeClass
	public static void setUpClass() throws Exception {

		FileOutputStream fos = null;

		propsConfigFile = File.createTempFile("curator-", ".properties");
		propsConfigFile.deleteOnExit();
		try {
			fos = new FileOutputStream(propsConfigFile);
			fos.write(propsConfig.getBytes());
			fos.flush();
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
		annosConfigFile = File.createTempFile("annotators-", ".xml");
		annosConfigFile.deleteOnExit();
		try {
			fos = new FileOutputStream(annosConfigFile);
			fos.write(annosConfig.getBytes());
			fos.flush();
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
		archiveConfigFile = File.createTempFile("database-", ".properties");
		archiveConfigFile.deleteOnExit();
		try {
			fos = new FileOutputStream(archiveConfigFile);
			fos.write(archsConfig.getBytes());
			fos.flush();
		} finally {
			if (fos != null) {
				fos.close();
			}
		}

	}

	@Before
	public void setUpTests() throws Exception {
		handler = new CuratorHandler(propsConfigFile.getAbsolutePath(),
				annosConfigFile.getAbsolutePath(),
				archiveConfigFile.getAbsolutePath());
	}

	@Test
	public void testPing() throws TException {
		Assert.assertTrue(handler.ping());
	}

	@Test
	public void testGetName() throws TException {
		Assert.assertTrue(handler.getName() != null);
		Assert.assertEquals("curator", handler.getName().toLowerCase());
	}

	@Test
	public void testGetVersion() throws TException {
		Assert.assertTrue(handler.getVersion() != null);
		Assert.assertFalse(handler.getVersion().isEmpty());
	}
	
	@Test
	public void testGetSourceIdentifier() throws TException {
		Assert.assertTrue(handler.getSourceIdentifier() != null);
		Assert.assertFalse(handler.getSourceIdentifier().isEmpty());
		Assert.assertTrue(handler.getSourceIdentifier().matches("^[cC]urator\\d+\\.\\d+"));
	}
	
	@Test
	public void testDescribeAnnotations() throws TException {
		Assert.assertTrue(handler.describeAnnotations().containsKey("tokens"));
		Assert.assertTrue(handler.describeAnnotations().containsKey("sentences"));
	}

	@Test
	public void testIsCacheAvailable() throws TException {
		Assert.assertTrue(handler.isCacheAvailable());
	}

	@Test
	public void testProvide() throws TException, ServiceUnavailableException, AnnotationFailedException {

		final String passage = "Welcome back my friends to the show that never ends";
		Record rec = handler.provide("tokens", passage, false);

		Assert.assertNotNull(rec.identifier);
		Assert.assertEquals(passage, rec.getRawText());
		
		Map<String,Labeling> lviews = rec.getLabelViews();
		
		Assert.assertTrue(lviews.containsKey("sentences"));
		Assert.assertTrue(lviews.containsKey("tokens"));
		
		Labeling tlab = lviews.get("tokens");
		
		Assert.assertNotNull(tlab);
		
		List<Span> tspans = tlab.getLabels();
		
		Assert.assertNotNull(tspans);
		Assert.assertEquals(10, tspans.size());

		
		// TODO: add more tests here to fill out exercising the server interface
		
	}
	
	@After
	public void tearDownTests() throws Exception {
		handler = null;
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

}
