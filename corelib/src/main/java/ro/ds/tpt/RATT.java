/*
    TimisoaraPublicTransport - display public transport information on your device
    Copyright (C) 2011-2014  Mihai Balint

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/
package ro.ds.tpt;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.*;

public class RATT {
	public static final int CITY_DB_ENTRIES = 875;
	public static final String root = "http://www.ratt.ro/txt/";
	
	public static List<ro.ds.tpt.model.Station> downloadStations(ro.ds.util.IPrefs prefs, ro.ds.util.IMonitor mon, ro.ds.tpt.model.City c) throws IOException {
		return new StationReader(new URL(prefs.getBaseUrl()+"select_statie.php"), c).readAll(mon);
	}

	public static String[] downloadTimes(ro.ds.util.IPrefs prefs, String pathId, String stationId) throws IOException {
		URL url = new URL(prefs.getBaseUrl()+"afis_msg.php?id_traseu="+pathId+"&id_statie="+stationId);
		ro.ds.util.FormattedTextReader rd = new ro.ds.util.FormattedTextReader(url.openStream());
		String lineName = rd.readString("Linia: </font>", "<");
		String timestamp = rd.readString("<br><br>", "<");
		String time1 = rd.readString("Sosire1: ", "<");
		String time2 = rd.readString("Sosire2: ", "<");
		rd.close();
		return new String[]{time1, time2, timestamp, lineName};
	}
	
	public static ro.ds.tpt.model.City downloadCity(ro.ds.util.IPrefs prefs, ro.ds.util.IMonitor mon) throws IOException {
		ro.ds.tpt.model.City c = new ro.ds.tpt.model.City();
		mon.setMax(1200);
		List<ro.ds.tpt.model.Station> stations = new StationReader(new URL(prefs.getBaseUrl()+"select_statie.php"), c).readAll(mon);
		int cnt = 0;
		for(ro.ds.tpt.model.Station s : stations) {
			new LineReader(c,s, new URL(prefs.getBaseUrl()+"select_traseu.php?id_statie="+s.getId())).readAll(new ro.ds.util.NullMonitor());
			cnt++;
			if((cnt % 10)==0)
				System.out.println(cnt + "/" + stations.size());
			mon.workComplete();
		}
		
		return c;
	}
	
	public static void addLatLongCoords(ro.ds.tpt.model.City c, InputStream in) throws IOException {
		ro.ds.util.StationsXMLReader rd = new ro.ds.util.StationsXMLReader(new ro.ds.util.FormattedTextReader(in));
		String[] stCoords;
		
		HashMap<String, ro.ds.tpt.model.Station> stationExtIdMap = new HashMap<String, ro.ds.tpt.model.Station>();
		for(ro.ds.tpt.model.Station s:c.getStations())
			stationExtIdMap.put(s.getExtId(), s);
		
		while(null != (stCoords = rd.readStationCoords())) {
			// coords = {name, id, lat,lng}
			ro.ds.tpt.model.Station s = stationExtIdMap.get(stCoords[1]);
			if(s==null) {
				System.out.println(stCoords[1]+" - "+stCoords[0]+": "+stCoords[2]+"x"+stCoords[3]);
				continue;
			}
			assert(s!=null);
			if(!s.getName().trim().equals(stCoords[0].trim())) {
				System.out.println("Name Diff: "+stCoords[1]+" - "+stCoords[0]+": "+s.getId()+" - "+s.getName());
			}
			s.setCoords(stCoords[2], stCoords[3]);
		}
		rd.close();
	}
	
	public static class StationReader extends OptValBuilder<ro.ds.tpt.model.Station> {
		private ro.ds.tpt.model.City c;
		public StationReader(ro.ds.util.FormattedTextReader in, ro.ds.tpt.model.City c) { super(in); this.c = c; }
		public StationReader(URL url, ro.ds.tpt.model.City c) throws IOException { super(url); this.c = c; }
		
		protected ro.ds.tpt.model.Station create(String val, String opt) {
			ro.ds.tpt.model.Station s = c.newStation(val, opt);
			return s; 
		}
	}

	public static class LineReader extends OptValBuilder<ro.ds.tpt.model.Line> {
		private ro.ds.tpt.model.Station st;
		private ro.ds.tpt.model.City c;
		public LineReader(ro.ds.tpt.model.City c, ro.ds.tpt.model.Station st, URL url) throws IOException {
			super(url);
			this.st = st;
			this.c = c;
		}
		public LineReader(ro.ds.tpt.model.City c, ro.ds.tpt.model.Station st, ro.ds.util.FormattedTextReader in) throws IOException {
			super(in);
			this.st = st;
			this.c = c;
		}
		
		protected ro.ds.tpt.model.Line create(String val, String opt) {
			if (opt.startsWith(" [0]  ") || opt.startsWith(" [1]  "))
				opt = opt.substring(6);
			ro.ds.tpt.model.Line l = c.getLineByName(opt);
			if (null==l)
				l = c.newLine(opt);
			ro.ds.tpt.model.Path p;
			if (l.getPaths().isEmpty()) {
				p = c.newPath(l, val, "");
				l.addPath(p);
			} else
				p = l.getFirstPath();
			p.concatenate(st, new ro.ds.tpt.model.HourlyPlan());
			st.addPath(p);
			return l; 
		}
	}

	public static class Est {
		public String lineName, pathFrom, pathTo, stopCrypticName, stopEst;
		public int stopNo;
		
		public Est(String lineName, String pathFrom, String pathTo, String stopCrypticName, int stopNo, String stopEst) {
			this.lineName = lineName;
			this.pathFrom = pathFrom;
			this.pathTo = pathTo;
			this.stopCrypticName = stopCrypticName;
			this.stopNo = stopNo;
			this.stopEst = stopEst;
		}
	}
	public static class EstimateIterator implements Enumeration<Est> {
		private ro.ds.util.FormattedTextReader rd;
		private String lineName, pathFrom, pathTo;
		private int stopNo;
		private Est currentStop;
		
		public EstimateIterator(ro.ds.util.FormattedTextReader rd, String lineName) {
			this.rd = rd;
			this.lineName = lineName;
			this.currentStop = null;
			nextStop();
		}

		@Override
		public boolean hasMoreElements() {
			return currentStop != null;
		}

		@Override
		public Est nextElement() {
			Est stop = currentStop;
			nextStop();
			return stop;
		}
		
		private boolean nextPath() {
			try {
				String pathFrom = rd.readString("<b>", "--->");
				String pathTo = rd.readString("--->", "</b></td>");
				if (pathFrom == null || pathTo == null) 
					return false;
				this.pathFrom = pathFrom.trim();
				this.pathTo = pathTo.trim();
				return true;
			} catch (IOException e) {
				return false;
			}
		}
		
		private boolean nextStop() {
			try {
				if (currentStop == null || currentStop.stopCrypticName.equalsIgnoreCase(pathTo)) {
					stopNo = 0;
					if (!nextPath()) 
						throw new IOException();
				}
				if (!rd.skipAfter("<b>"+lineName+"</b></td>", true))
					throw new IOException("File format error");
				String stopCrypticName = rd.readString("<b>", "</b></td>");
				String stopEst = rd.readString("<b>", "</b></td>");
				if (stopCrypticName == null || stopEst == null)
					throw new IOException("File format error");
				currentStop = new Est(lineName, pathFrom, pathTo, stopCrypticName.trim(), stopNo, stopEst.trim());
				stopNo += 1;
				return true;
			} catch (IOException e) {
				this.currentStop = null;
				return false;
			}
		}
		
	}
	
	public static Enumeration<Est> downloadTimes2(ro.ds.util.IPrefs prefs, String lineName, String pathId) throws IOException {
		URL url = new URL("http://86.122.170.105:61978/html/timpi/sens0.php?param1="+pathId);
		ro.ds.util.FormattedTextReader rd = new ro.ds.util.FormattedTextReader(url.openStream());
		if (!rd.skipAfter("<table", true))
			throw new IOException("'File format error");
		return new EstimateIterator(rd, lineName);
	}
	
	
	public static String[] downloadTimes2(ro.ds.util.IPrefs prefs, String lineName, String pathId, String stationId) throws IOException {
		URL url = new URL("http://86.122.170.105:61978/html/timpi/sens0.php?param1="+pathId);
		ro.ds.util.FormattedTextReader rd = new ro.ds.util.FormattedTextReader(url.openStream());
		if (!rd.skipAfter("<table", true))
			throw new IOException("'File format error");
		Enumeration<Est> est = new EstimateIterator(rd, lineName);
		while(est.hasMoreElements()) {
			Est e = est.nextElement();
			if (stationId.equals(e.stopCrypticName))
				return new String[]{e.stopEst, "", "", lineName};
		}
		return new String[]{""};
	}
	
}
