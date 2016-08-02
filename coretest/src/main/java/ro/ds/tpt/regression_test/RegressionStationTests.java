package ro.ds.tpt.regression_test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import ro.ds.tpt.model.INamedEntity;
import ro.ds.tpt.model.Path;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import static ro.ds.tpt.regression_test.BlackListed.*;

/**
 * This file's sole purpose is to identify changes 
 * within the RATT web site: line/station renaming or 
 * changing IDs or adding new/removing old lines/stations.
 * @author mihai
 *
 */
public class RegressionStationTests extends TestCase {
	private ro.ds.tpt.model.City cActual = null, cExpected = null;
	
	public RegressionStationTests(String testMethod, ro.ds.tpt.model.City cActual, ro.ds.tpt.model.City cExpected) {
		super(testMethod);
		this.cActual = cActual;
		this.cExpected = cExpected;
	}
	
	public void testLineCount() {
		assertEquals("",
			cExpected.getLines().size(), 
			cActual.getLines().size()
		);
	}
	
	private static Collection<INamedEntity> wrap(Collection<ro.ds.tpt.model.Line> lines) {
		// TODO Kill me and replace me with path based tests
		Collection<INamedEntity> wrapLines = new ArrayList<INamedEntity>();
		for(ro.ds.tpt.model.Line l : lines) {
			HashSet<String> ids = new HashSet<String>();
			for(Path p : l.getPaths())
				if (!ids.contains(p.getExtId())) {
					ids.add(p.getExtId());
					wrapLines.add(bl(p.getExtId(), l.getName()));
				}
		}
		return wrapLines;
	}
	
	public void testLines() {
		String diff = RegressionTests.diffEntities(wrap(cExpected.getLines()), wrap(cActual.getLines()), blExpectedLines, blActualLines);
		if (!diff.isEmpty()) {
			System.out.println("\nLine ids: "+diff);
		}
		assertEquals("", diff,"");
	}

	
	public void testStationCount() {
		assertEquals("",cExpected.getStations().size(), cActual.getStations().size());
	}
	public void testStations() {
		assertEquals("",
			RegressionTests.diffEntities(cExpected.getStations(), cActual.getStations()),"");
	}
	
	
	public static void addTests(TestSuite suite, ro.ds.tpt.model.City cActual, ro.ds.tpt.model.City cExpected) {
		for(String t : RegressionTests.testMethods(RegressionStationTests.class))
			suite.addTest(new RegressionStationTests(t, cActual, cExpected));
	}
}
