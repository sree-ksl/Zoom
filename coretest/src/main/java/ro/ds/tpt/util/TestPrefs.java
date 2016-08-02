package ro.ds.tpt.util;

import ro.ds.tpt.analytics.Collector;
import ro.ds.tpt.analytics.IAnalyticsService;
import ro.ds.util.IPrefs;

public class TestPrefs implements IPrefs {

	@Override
	public String getBaseUrl() {
		return ro.ds.tpt.RATT.root;
	}

	@Override
	public Collector getAnalyticsCollector() {
		return new Collector(this, getDeviceId());
	}

	@Override
	public IAnalyticsService getAnalyticsService() {
		return new ro.ds.tpt.analytics.NoAnalyticsService();
	}

	@Override
	public String getDeviceId() {
		return IPrefs.DEFAULT_DEVICE_ID;
	}

	@Override
	public void setDeviceId(String deviceId) {
	}

	@Override
	public void setCachedAnalytics(String analytics) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String addCachedAnalytics(String analytics) {
		// TODO Auto-generated method stub
		return null;
	}

}
