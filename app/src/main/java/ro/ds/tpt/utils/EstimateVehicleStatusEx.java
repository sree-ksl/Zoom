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

import java.util.NoSuchElementException;

public enum EstimateVehicleStatusEx {
	Away(ro.ds.tpt.model.Estimate.VehicleStatus.Away, ro.ds.tpt.R.string.lblVStatusAway),
	Boarding(ro.ds.tpt.model.Estimate.VehicleStatus.Boarding, ro.ds.tpt.R.string.lblVStatusBoarding),
	Arriving(ro.ds.tpt.model.Estimate.VehicleStatus.Arriving, ro.ds.tpt.R.string.lblVStatusArriving),
	Departing(ro.ds.tpt.model.Estimate.VehicleStatus.Departing, ro.ds.tpt.R.string.lblVStatusDeparting),
	ArrivingDeparting(ro.ds.tpt.model.Estimate.VehicleStatus.ArrivingDeparting, ro.ds.tpt.R.string.lblVStatusArrivingDeparting);
	
	public final ro.ds.tpt.model.Estimate.VehicleStatus original;
	public final int descriptionId;

	private EstimateVehicleStatusEx(ro.ds.tpt.model.Estimate.VehicleStatus original, int descriptionId) {
		this.original = original;
		this.descriptionId = descriptionId;
	}
	
	public static EstimateVehicleStatusEx getAndroidEx(ro.ds.tpt.model.Estimate.VehicleStatus kind) {
		for(EstimateVehicleStatusEx ex: values())
			if (ex.original == kind)
				return ex;
		throw new NoSuchElementException();
	}
	
	public static int getDescriptionId(ro.ds.tpt.model.Estimate.VehicleStatus status) {
		return getAndroidEx(status).descriptionId;
	}
	
}
