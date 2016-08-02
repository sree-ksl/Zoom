package ro.ds.tpt.regression_test;

import java.util.Collection;
import java.util.LinkedHashSet;

import ro.ds.tpt.model.Path;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * This file's sole purpose is to identify changes 
 * within the RATT web site: line/station renaming or 
 * changing IDs or adding new/removing old lines/stations.
 * @author mihai
 *
 */
public class RegressionLineTests extends TestCase {
	private ro.ds.tpt.model.City cActual = null, cExpected = null;
	private String lineName;
	
	public RegressionLineTests(String testMethod, ro.ds.tpt.model.City cActual, ro.ds.tpt.model.City cExpected, String lineName) {
		super(testMethod);
		this.cActual = cActual;
		this.cExpected = cExpected;
		this.lineName = lineName;
	}
	
	public void disabled_testLine_StationCount() {
		ro.ds.tpt.model.Line expected = cExpected.getLineByName(lineName);
		ro.ds.tpt.model.Line actual = cActual.getLineByName(lineName);
		
		assertEquals("",
			expected.getStations().size(), 
			actual.getStations().size()
		);
	}
	
	public void testLine_Stations() {
		ro.ds.tpt.model.Line expected = cExpected.getLineByName(lineName);
		ro.ds.tpt.model.Line actual = cActual.getLineByName(lineName);

		String diff = RegressionTests.diffEntities(expected.getStations(), actual.getStations(),
				BlackListed.blExpectedLineStations(lineName), BlackListed.blActualLineStations(lineName));
		if (!diff.isEmpty())
			System.out.println("\nLine: "+lineName+diff);
		
		assertEquals("", diff,"");
	}
	
	@Override
	public String getName() {
		return super.getName().replaceFirst("Line_", "_"+lineName+"_");
	}

	public static boolean notListed(ro.ds.tpt.model.Line l, Collection<BlackListed> list) {
		// TODO KILL this method
		for(Path p:l.getPaths())
			if(!BlackListed.isIdListed(BlackListed.bl(p.getExtId(), l.getName()), list))
				return true;
		return false;
	}
	
	public static void addTests(TestSuite suite, ro.ds.tpt.model.City cActual, ro.ds.tpt.model.City cExpected) {
		LinkedHashSet<String> lineNames = new LinkedHashSet<String>();
		// TODO Assert paths instead of lines
		for(ro.ds.tpt.model.Line l : cExpected.getLines())
			if (notListed(l, BlackListed.blExpectedLines))
				lineNames.add(l.getName());
		for(ro.ds.tpt.model.Line l : cActual.getLines())
			if (notListed(l, BlackListed.blActualLines))
				lineNames.add(l.getName());
		
		for(String lineName : lineNames) {
			for(String t : RegressionTests.testMethods(RegressionLineTests.class))
				suite.addTest(new RegressionLineTests(t, cActual, cExpected, lineName));
		}
	}
}
