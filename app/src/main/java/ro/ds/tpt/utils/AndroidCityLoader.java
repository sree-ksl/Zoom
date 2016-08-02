/*
    TimisoaraPublicTransport - display public transport information on your device
    Copyright (C) 2011  Mihai Balint

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
package ro.ds.tpt.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import android.content.Context;
import android.content.res.Resources;

import ro.ds.util.IPrefs;
import ro.ds.util.NullMonitor;

public class AndroidCityLoader {
	private static final String cityCacheFileName = "citylines.dat"; 

	public static ro.ds.tpt.model.City loadFromAppResources(Context ctx) throws IOException {
		ro.ds.tpt.model.City c = new ro.ds.tpt.model.City();
		c.loadFromStream(new AndroidDetachableStream.FromRawResource(ctx, ro.ds.tpt.R.raw.citylines), new NullMonitor(), ro.ds.tpt.RATT.CITY_DB_ENTRIES);
		return c;
	}
	
	public static ro.ds.tpt.model.City loadStoredCityOrDownloadAndCache(IPrefs prefs, Context ctx, ro.ds.util.IMonitor mon) throws IOException {
		ro.ds.tpt.model.City c = new ro.ds.tpt.model.City();
		try {
			// read resources file
			c.loadFromStream(new AndroidDetachableStream.FromRawResource(ctx, ro.ds.tpt.R.raw.citylines),mon, ro.ds.tpt.RATT.CITY_DB_ENTRIES);
		} catch(Resources.NotFoundException ex) {
			try {
				// read the proper cache
				c.loadFromStream(new AndroidDetachableStream.FromFile(ctx,cityCacheFileName),mon, ro.ds.tpt.RATT.CITY_DB_ENTRIES);
			} catch(FileNotFoundException e) {
				// download and parse new stuff
				c = ro.ds.tpt.RATT.downloadCity(prefs, mon);
			} finally {
				OutputStream os = ctx.openFileOutput(cityCacheFileName, Context.MODE_PRIVATE);
				c.saveToFile(os);
			} 
		} catch(ro.ds.tpt.SaveFileException e) {
			// the file must be saved again because it was in an older format
			OutputStream os = ctx.openFileOutput(cityCacheFileName, Context.MODE_PRIVATE);
			c.saveToFile(os);
		}

		return c;
	}
}
