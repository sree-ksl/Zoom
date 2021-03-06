package ro.ds.tpt.test;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.TimeZone;

import org.junit.Test;

import ro.ds.util.Formatting;

public class EstimateTest {

	public void printTimeZones() {
		for(String tz : TimeZone.getAvailableIDs())
			System.out.println(tz);
	}
	
	@Test
	public void estimateTimeZone() {
		Calendar est = Calendar.getInstance(TimeZone.getTimeZone("Europe/Bucharest"));
		assertNotNull(est);
	}
	
	private static String timestamp(long timestamp) {
		Calendar time = Calendar.getInstance(TimeZone.getTimeZone("Europe/Bucharest"));
		time.setTimeInMillis(timestamp);
		// 2013-06-11 07:19:21
		String year = Formatting.zeroLeadingInteger(time.get(Calendar.YEAR), 4);
		String month = Formatting.zeroLeadingInteger(time.get(Calendar.MONTH)+1, 2);
		String day = Formatting.zeroLeadingInteger(time.get(Calendar.DAY_OF_MONTH), 2);
		String hour = Formatting.zeroLeadingInteger(time.get(Calendar.HOUR_OF_DAY), 2);
		String min = Formatting.zeroLeadingInteger(time.get(Calendar.MINUTE), 2);
		String sec = Formatting.zeroLeadingInteger(time.get(Calendar.SECOND), 2);
		return year+"-"+month+"-"+day+" "+hour+":"+min+":"+sec;
	} 
	
	@Test
	public void afterTest() {
		ro.ds.tpt.model.Estimate e1 = new ro.ds.tpt.model.Estimate(null, null, 0, null);
		ro.ds.tpt.model.Estimate e2 = new ro.ds.tpt.model.Estimate(null, null, 0, null);
		String now = timestamp(System.currentTimeMillis());
		e1.putTime("13:30", "13:30", now);
		e2.putTime("13:40", "13:40", now);

		assertTrue(e2.after(e1, false));
		assertFalse(e1.after(e2, true));
	}

	@Test
	public void afterTest2() {
		ro.ds.tpt.model.Estimate e1 = new ro.ds.tpt.model.Estimate(null, null, 0, null);
		ro.ds.tpt.model.Estimate e2 = new ro.ds.tpt.model.Estimate(null, null, 0, null);
		String now = timestamp(System.currentTimeMillis());
		e1.putTime("13:50", "13:50", now);
		e2.putTime("13:20", "13:20", now);

		assertTrue(e1.after(e2, false));
		assertFalse(e2.after(e1, true));
	}
	
	@Test
	public void testParseTimestamp() {
		long nowMilis = System.currentTimeMillis();
		String now = timestamp(nowMilis);
		long estMilis = ro.ds.tpt.model.Estimate.parseUpdateTime(now, 0);
		assertTrue(estMilis != 0);
		assertEquals(now, timestamp(estMilis));
	}
	
	@Test
	public void testParseKnownTimestamp() {
		String timestamp = "2013-06-11 07:19:21";
		long estMilis = ro.ds.tpt.model.Estimate.parseUpdateTime(timestamp, 0);
		assertTrue(estMilis != 0);
		assertEquals(timestamp, timestamp(estMilis));
	}
}
