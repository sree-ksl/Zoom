/*
    TimisoaraPublicTransport - display public transport information on your device
    Copyright (C) 2014  Mihai Balint

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import ro.ds.util.IPrefs;
import ro.ds.util.NullMonitor;

public class JavaCityLoader {
	public static final String cityCacheFileName = "citylines.dat"; 

	/**
	 * Not for running on androids
	 * @return
	 * @throws IOException
	 */
	public static ro.ds.tpt.model.City loadCachedCityOrDownloadAndCache(IPrefs prefs) throws IOException {
		ro.ds.tpt.model.City c;
		File cache = new File(cityCacheFileName);
		if(cache.isFile() && cache.exists() && cache.canRead()) {
			c = new ro.ds.tpt.model.City();
			c.loadFromStream(new ro.ds.util.DetachableStream.FromFile(cityCacheFileName), new NullMonitor(), RATT.CITY_DB_ENTRIES);
		} else {
			c = RATT.downloadCity(prefs, new NullMonitor());
			c.saveToFile(new FileOutputStream(cache));
		}
		
		return c;
	}
}
