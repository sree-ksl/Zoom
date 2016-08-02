package ro.ds.tpt.test;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class RestTest {
	private String url;

	@Before
	public void setUp() throws IOException, InterruptedException, Exception {
		url = "https://aeliptus.com/tpt-analytics";
		//url = "http://localhost:8080";
	}
	
	@After
	public void tearDown() throws IOException {
	}

	@Test
	public void testPostData() throws IOException {
		URL u = new URL(url+"/generate_device_id");
		URLConnection con = u.openConnection();
		
		con.setDoOutput(true);
		con.setAllowUserInteraction(false);
        
		PrintStream ps = new PrintStream(con.getOutputStream());
        ps.print("zuzu\r\n\r\n");
        ps.close();
        
		BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String result = br.readLine();
		br.close();
		assertTrue(result!=null && result.length()>0);
	}

	@Test
	public void testAnalytics() throws IOException {
		ro.ds.tpt.analytics.AnalyticsService as = new ro.ds.tpt.analytics.AnalyticsService(url);
		String generatedId = as.generateDeviceId();
		
		assertTrue(generatedId.length() == 128);
		as.postTimesBundle(generatedId, "TEST-DATA");
		try {
			as.postTimesBundle("TEST-WHAT-DOES-THIS-DO", "bla");
			fail();
		} catch(java.io.FileNotFoundException e) {
		}
	}
	
	@Test
	public void testPostTimesBundle() throws IOException {
		ro.ds.tpt.analytics.AnalyticsService as = new ro.ds.tpt.analytics.AnalyticsService(url);
		String generatedId = "TEST";
		
		try {
			as.postTimesBundle(generatedId, "TEST-DATA");
			fail();
		} catch(java.io.FileNotFoundException e) {
		}
	}
	
}
