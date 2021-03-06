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
package ro.ds.tpt.data;

public enum Achievements {
	None(0), 
	Contributor(ro.ds.tpt.R.drawable.badge_contributor);

	private int badgeId, title, description;
	private boolean visible;
	
	private Achievements(int badgeId) {
		this.badgeId = badgeId;
		this.visible = badgeId>0;
	}
	
	public int getBadgeId() {
		return badgeId;
	}
	
	public int getTitle() {
		return title;
	}
	
	public int getDescription() {
		return description;
	}
	
	public boolean isVisible() {
		return visible;
	}
	
}
