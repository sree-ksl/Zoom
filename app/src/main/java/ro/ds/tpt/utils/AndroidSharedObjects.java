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

import ro.ds.tpt.conf.TravelOpportunity;

public class AndroidSharedObjects {
	private ro.ds.tpt.model.City city;
	private ro.ds.tpt.model.Path linePath;
	private TravelOpportunity pathSelection;
	
	public ro.ds.tpt.model.City getCity() throws CityNotLoadedException {
		if (null==city) 
			throw new CityNotLoadedException();
			// this happens when re-entering the app in certain activities.
	
		return city;
	}
	public void setCity(ro.ds.tpt.model.City city) {
		this.city = city;
	}
	
	public TravelOpportunity getPathSelection() throws CityNotLoadedException {
		if (null==pathSelection) 
			throw new CityNotLoadedException();
		return pathSelection;
	}
	public ro.ds.tpt.model.Path getLinePath() throws CityNotLoadedException {
		if (null==linePath) 
			throw new CityNotLoadedException();
		return linePath;
	}
	public void setLinePath(ro.ds.tpt.model.Path linePath) {
		this.linePath = linePath;
		this.pathSelection = new TravelOpportunity(linePath);
		this.pathSelection.selectAllStations();
	}
	public void setLinePath(TravelOpportunity pathSelection) {
		this.linePath = pathSelection.getPath();
		this.pathSelection = pathSelection;
	}
	
	
	private static AndroidSharedObjects shared = new AndroidSharedObjects();
	
	public static AndroidSharedObjects instance() {
		return shared;
	}
}
