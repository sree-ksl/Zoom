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
package ro.ds.tpt;

import java.util.Iterator;

import ro.ds.tpt.utils.CityNotLoadedException;

public class ViewCatFavorites extends ViewCategories {

	@Override
	protected Iterator<ro.ds.tpt.model.Path> getLinePathIterator(ro.ds.tpt.model.City city) {
		return getAppPreferences().getFavoritePaths(city, 6).iterator();
	}

	protected int getContentViewResId() {
    	return R.layout.list_favorites;
    }
	
    protected void addContentOnCreate() throws CityNotLoadedException {
    	// nop
    }
    protected void addContentOnResume() throws CityNotLoadedException {
    	createContent(getCity());
    }
}