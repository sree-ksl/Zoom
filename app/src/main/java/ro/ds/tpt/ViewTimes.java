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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import ro.ds.tpt.conf.ChangeOpportunity;
import ro.ds.tpt.conf.Constants;
import ro.ds.tpt.conf.TravelOpportunity;
import ro.ds.tpt.utils.CityActivity;
import ro.ds.tpt.utils.CityNotLoadedException;
import ro.ds.tpt.utils.EstimateStatusEx;
import ro.ds.tpt.utils.LineKindAndroidEx;
import ro.ds.tpt.utils.StartActivity;
import ro.ds.tpt.model.Path;
import ro.ds.tpt.utils.AndroidSharedObjects;
import ro.ds.tpt.utils.EstimateVehicleStatusEx;
import ro.ds.util.LineKind;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;

public class ViewTimes extends CityActivity {
	private TableLayout timesTable;
	private LayoutInflater inflater;
	private ro.ds.tpt.model.City city;
	private TravelOpportunity path;
	private UpdateTimes updater;
	private UpdateQueue queue;

	/** Called when the activity is first created. */
    @Override
	protected void onCreateCityActivity(Bundle savedInstanceState) throws CityNotLoadedException {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
    	setContentView(R.layout.list_times);
    	city = getCity();
    	city.getClass();
    	
    	path = AndroidSharedObjects.instance().getPathSelection();
		queue = new UpdateQueue();
		View pathView = this.findViewById(R.id.PathView);
		PathView.fillPathView(pathView, this.getResources(), path, new PathSwitcher(), false);
		
		LineKindAndroidEx kind = LineKindAndroidEx.getAndroidEx(path.getLineKind());
		pathView.findViewById(R.id.LinePathLine).setBackgroundResource(kind.line_top);
		pathView.findViewById(R.id.LinePathBullet).setBackgroundResource(kind.line_bullet);
		pathView.findViewById(R.id.LinePath).setVisibility(View.VISIBLE);
		
    	Button update = (Button)findViewById(R.id.UpdateButton);
    	update.setOnClickListener(updater = new UpdateTimes(this));
    	
    	Button connections = (Button)findViewById(R.id.ConnectionsButton);
    	connections.setOnClickListener(new SelectConnectionKinds());
    	
    	Button favorite = (Button)findViewById(R.id.StarButton);
    	favorite.setOnClickListener(new FavoritePathToggle(favorite));
    	
    	
    	
    	timesTable = (TableLayout)findViewById(R.id.StationTimesTable);
    	inflater = this.getLayoutInflater();
    	inflateTable();
    	updater.onClick(null); // start update immediately after starting the activity
    }
    
    public void queueUIUpdate(Runnable r) {
    	queue.add(r);
    	runOnUiThread(queue);
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	updater.killUpdate();
    }
    
	private void inflateTable() {
		timesTable.removeAllViews();
		List<ChangeOpportunity> changeOpportunities = path.getDisembarkOpportunities();

		int stationNo = 0; // java is zero based, therefore first = even
		View previousRow = null;
		for(ChangeOpportunity stop: changeOpportunities) {
			ro.ds.tpt.model.Estimate est = stop.getDisembarkEstimate();
			boolean evenRow = (stationNo % 2) == 0;
			boolean last = stationNo == changeOpportunities.size()-1;
			
			View newRow;
			if(stationNo == 0) {
				newRow = updatePathView(stop);
			} else {
				newRow = newStationEstimateView(stop, evenRow, last);
		    	timesTable.addView(newRow);
			}
			updateVehicleArrivingBullet(est.getVehicleStatus(), newRow, previousRow);
			previousRow = newRow;
	    	
	    	for(ro.ds.tpt.model.Estimate connection : stop.getConnections()) {
	    		newRow = newConnectionEstimateView(connection, evenRow, last);
	    		timesTable.addView(newRow);
	    		previousRow = newRow;
	    	}
	    	stationNo++;
		}
	}
	
	private View updatePathView(ChangeOpportunity stop) {
		ro.ds.tpt.model.Estimate est = stop.getDisembarkEstimate();
		View timesRow = findViewById(R.id.PathView);
		PathView.addDepartureClickListener(timesRow, new ShowStationConnections(stop));
		int background = est.isUpdating() ? R.drawable.row_back_updating : R.drawable.row_back_even;
		timesRow.setBackgroundResource(background);
		LineKindAndroidEx kind = LineKindAndroidEx.getAndroidEx(path.getLineKind());
		
		TextView stationTime = (TextView)timesRow.findViewById(R.id.StationTime);
		stationTime.setText(est.isBoarding() 
				? getResources().getString(EstimateVehicleStatusEx.Boarding.descriptionId)
				: est.estimateTimeString());
		stationTime.setTextColor(getResources().getColor(kind.colorId));
		stationTime.setVisibility(View.VISIBLE);
		
		View boardingBullet = timesRow.findViewById(R.id.VehicleBoardingBullet);
		boardingBullet.setVisibility(View.GONE);
		View departingBullet = timesRow.findViewById(R.id.VehicleDepartingBullet);
		departingBullet.setVisibility(View.GONE);
		
		return timesRow;
	}

	private Random random = new Random();
	private HashMap<String, Integer> waiting = new HashMap<String, Integer>();
    private class IncrementWaiting implements OnClickListener {
    	private String key;
    	
    	public IncrementWaiting(String key) {
    		this.key = key;
    	}
    	
		public void onClick(View v) {
			if (!waiting.containsKey(key))
				return;
			waiting.put(key, waiting.get(key) + 1);
	    	runOnUiThread(new UpdateView());
		}
    }
    
    private String getAprText(String key) {
		int people;
		if (!waiting.containsKey(key)) {
			switch(random.nextInt(5)) {
			case 0: case 1: 
				people = random.nextInt(5); break;
			case 2: 
				people = 6+random.nextInt(6); break;
			default:
				people = 0; break;
			}
			waiting.put(key, people);
		}
		people = waiting.get(key);
		if (people == 0)
			return "";
		if (people == 1)
			return "O persoana asteapta aici";
		else if (people < 15) 
			return people + " persoane asteapta aici";
		else if (people < 20)
			return "Multe persoane asteapta aici";
		else if (people < 25)
			return "Foarte multi asteapta aici";
		else if (people < 35)
			return "Cei mai multi asteapta aici";
		else if (people < 45)
			return "Smartphone party!";
		else if (people < 60)
			return "Prima zi din Aprilie?";
		else if (people < 75)
			return "Aici e revolutie!";
		else if (people < 85)
			return "Err:123 - utilizator defect.";
		else if (people < 100)
			return "Stii, asta e telefonul tau";
		else if (people < 150)
			return "Auuuu! ma doare ecranul!";
		else if (people < 200)
			return "Serios?";
		else if (people < 250)
			return "Inca nu te-ai plictisit?";
		else if (people < 300)
			return "Oare cat mai tine bateria?";
		else if (people < 350)
			return "Pune androidul jos, ACUM!";
		else if (people < 400)
			return "Crezi ca primesti ceva premiu?";
		else if (people < 500)
			return "Nu e nici o luminita in tunel.";
		else if (people < 505)
			return "Bravo! Ai dat 500 clickuri!";
		else if (people < 515)
			return "Un review la aplicatie?";
		else if (people < 530)
			return "De aici nu mai urmeaza nimic!";
		else if (people < 550)
			return "Pe bune asta a fost";
		else if (people < 600)
			return "Ai prins tramvaiul?";
		else if (people < 700)
			return "Cat timp din viata ai pierdut?";
		else if (people < 800)
			return "Incep sa cedez.";
		else if (people < 900)
			return "Eu plec. Ciao!";
		else if (people < 1000)
			return "Mai tine bateria asta?";
		else if (people < 1010)
			return "1000 de clickuri?";
		else if (people < 1020)
			return "La reclame dai 1000 de clickuri?";
		else if (people < 1050)
			return "Atat a fost";
		else if (people < 100000)
			return people + " clickuri";
		else if (people < 100005)
			return "100k clickuri. Inca e 1 Apr?";
		else
			return people + " clickuri";
    }
	
	
	private View newStationEstimateView(ChangeOpportunity stop, boolean evenRow, boolean last) {
		ro.ds.tpt.model.Estimate est = stop.getDisembarkEstimate();
		View timesRow = inflater.inflate(R.layout.infl_station_time, timesTable, false);
		timesRow.setOnClickListener(new ShowStationConnections(stop));
		LineKindAndroidEx kind = LineKindAndroidEx.getAndroidEx(path.getLineKind());

		TextView stationLabel = (TextView)timesRow.findViewById(R.id.StationLabel);
		stationLabel.setText(est.getStation().getNicestNamePossible());
		
		TextView stationTime = (TextView)timesRow.findViewById(R.id.StationTime);
		stationTime.setText(est.isBoarding() 
				? getResources().getString(EstimateVehicleStatusEx.Boarding.descriptionId)
				: est.estimateTimeString());
		stationTime.setTextColor(getResources().getColor(kind.colorId));
		
		timesRow.findViewById(R.id.LinePathLine).setBackgroundResource(
				last ? kind.line_bottom : kind.line_middle);
		timesRow.findViewById(R.id.LinePathBullet).setBackgroundResource(kind.line_bullet);
		
		int background = R.drawable.row_back_odd;
		if (est.isUpdating()) {
			background = R.drawable.row_back_updating;
		} else {
			if (est.hasErrors()) {
				updater.setHasErrors();
				TextView error = (TextView)timesRow.findViewById(R.id.StationError);
				error.setText(EstimateStatusEx.getDescriptionId(est.getStatus()));
			}
			if (evenRow)
				background = R.drawable.row_back_even;
		}

		View row = timesRow.findViewById(R.id.StationStatusRow);
		row.setBackgroundResource(background);
		
		Calendar now = Calendar.getInstance();
		if ((now.get(Calendar.DAY_OF_MONTH) == 1 && now.get(Calendar.MONTH) == Calendar.APRIL) ||
				getAppPreferences().getAchieveUserName().equals("20140401")) {
			String key = path.getPath().getExtId()+est.getStation().getId();
			TextView aprfool = (TextView)timesRow.findViewById(R.id.aprfooltext);
			aprfool.setText(getAprText(key));
			aprfool.setVisibility(View.VISIBLE);
			Button aprbutton = (Button)timesRow.findViewById(R.id.aprfoolbutton); 
			aprbutton.setVisibility(View.VISIBLE);
			aprbutton.setOnClickListener(new IncrementWaiting(key));
		}
		
		return timesRow;
	}

	private View newConnectionEstimateView(ro.ds.tpt.model.Estimate est, boolean evenRow, boolean last) {
		View timesRow = inflater.inflate(R.layout.infl_connection_time, timesTable, false);

		Path connectingPath = est.getPath();
		ro.ds.tpt.model.Line connectingLine = connectingPath.getLine();
		Resources res = getResources();
		
		TextView lineNameLabel = (TextView)timesRow.findViewById(R.id.LineName);
		lineNameLabel.setTextColor(res.getColor(LineKindAndroidEx.getColorId(connectingLine.getKind())));
		lineNameLabel.setText(LineKindAndroidEx.getLineNameLabel(connectingLine));

		timesRow.findViewById(R.id.LineBarredKind).setVisibility(
				connectingLine.isBarred() ? View.VISIBLE : View.GONE);

		TextView lineDirectionLabel = (TextView)timesRow.findViewById(R.id.LineDirection);
		lineDirectionLabel.setText(connectingPath.getNiceName());

		TextView stationTime = (TextView)timesRow.findViewById(R.id.StationTime);
		stationTime.setTextColor(res.getColor(LineKindAndroidEx.getColorId(connectingLine.getKind())));
		stationTime.setText(est.isBoarding() 
				? getResources().getString(EstimateVehicleStatusEx.Boarding.descriptionId)
				: est.estimateTimeString());
		
		LineKindAndroidEx kind = LineKindAndroidEx.getAndroidEx(path.getLineKind());
		timesRow.findViewById(R.id.LinePathLine).setBackgroundResource(
				last ? R.drawable.line_middle_empty : kind.line_middle); 
		
		int background = R.drawable.row_back_odd;
		if (est.isUpdating()) {
			background = R.drawable.row_back_updating;
		} else {
			if (est.hasErrors())
				updater.setHasErrors();
			if (evenRow)
				background = R.drawable.row_back_even;
		}
		View row = timesRow.findViewById(R.id.StationStatusRow);
		row.setBackgroundResource(background);
		
		return timesRow;
	}
	
	private void updateVehicleArrivingBullet(ro.ds.tpt.model.Estimate.VehicleStatus vehicle, View thisRow, View previousRow) {
		if (vehicle.isArriving()) {
			View arrivingBullet = thisRow.findViewById(R.id.VehicleArrivingBullet);
			arrivingBullet.setVisibility(View.VISIBLE);
			assert(previousRow!=null);
			View prevDepartingBullet = previousRow.findViewById(R.id.VehicleDepartingBullet);
			prevDepartingBullet.setVisibility(View.VISIBLE);
		} else {
			if(previousRow!=null) {
				View prevDepartingBullet = previousRow.findViewById(R.id.VehicleDepartingBullet);
				prevDepartingBullet.setVisibility(View.GONE);
			}
			if (vehicle.isBoarding()) {
				View boardingBullet = thisRow.findViewById(R.id.VehicleBoardingBullet);
				boardingBullet.setVisibility(View.VISIBLE);
			}
		}
	}

	private void updateVehicleDepartingBullet(ro.ds.tpt.model.Estimate.VehicleStatus vehicle, View thisRow, View nextRow) {
		if (vehicle.isDeparting()) {
			View nextBoardingBullet = nextRow.findViewById(R.id.VehicleBoardingBullet);
			nextBoardingBullet.setVisibility(View.GONE);
		}
	}

	private static class UpdateTimes implements Runnable, OnClickListener {
		private AtomicBoolean running = new AtomicBoolean(false);
		private AtomicBoolean hasErrors = new AtomicBoolean(false);
		private ViewTimes act;
		private ReportError err;
		
		public UpdateTimes(ViewTimes act) {
			this.act = act;
		}
		
		public void run() {
			hasErrors.set(false);
			List<ChangeOpportunity> stations = act.path.getDisembarkOpportunities();
			int ec = 0, index = 0, stationNo = 0;
			for(ChangeOpportunity stop: stations) {
				if(!running.get()) return;
				
				boolean evenRow = (stationNo % 2) == 0;
				boolean last = stationNo == stations.size()-1;
				ec = updateStationRowView(ec, index, evenRow, last, stop);
				index++;
				for(ro.ds.tpt.model.Estimate connection : stop.getConnections()) {
					if(!running.get()) return;
					ec = updateConnectionRowView(ec, index, evenRow, last, connection);
					index++;
				}
				stationNo++;
			}
			killUpdate();
			act.runOnUiThread(act.new UpdateView());
			if (hasErrors.compareAndSet(true, false))
				act.runOnUiThread(newError());
		}
		
		private ReportError newError() {
			if(err!=null) 
				err.dismiss();
			err = new ReportError(act);
			return err;
		}

		private int updateConnectionRowView(int ec, final int rowIndex, final boolean even, final boolean last, final ro.ds.tpt.model.Estimate connection) {
			Runnable upd = new Runnable() {
				public void run() {
					if (rowIndex > 0) {
						// connection rows are never arriving, therefore their predecessor can 
						// never be departing
						View previousRow = act.timesTable.getChildAt(rowIndex-1);
						previousRow.findViewById(R.id.VehicleDepartingBullet).setVisibility(View.GONE);
					}
					// row index 0 is for the big path view
					act.timesTable.removeViewAt(rowIndex-1);
					act.timesTable.addView(act.newConnectionEstimateView(connection, even, last), rowIndex-1);
				}
			};
			connection.startUpdate();
			act.queueUIUpdate(upd);
			ec = connection.updateStation(act.getAppPreferences(), ec);
			act.queueUIUpdate(upd);
			return ec;
		}

		private int updateStationRowView(int ec, final int rowIndex, final boolean even, final boolean last, final ChangeOpportunity stop) {
			final ro.ds.tpt.model.Estimate disembark = stop.getDisembarkEstimate();
			Runnable upd = new Runnable() {
				public void run() {
					View newRow;
					View previousRow, nextRow;
					if (rowIndex == 0) {
						newRow = act.updatePathView(stop);
						previousRow = null;
						nextRow = (act.timesTable.getChildCount() > 0) 
								? act.timesTable.getChildAt(0) 
								: null;
					} else {
						newRow = act.newStationEstimateView(stop, even, last);
						act.timesTable.removeViewAt(rowIndex-1);
						act.timesTable.addView(newRow, rowIndex-1);
						previousRow = rowIndex > 1 
								? act.timesTable.getChildAt(rowIndex-2) 
								: act.findViewById(R.id.PathView);
						nextRow = (rowIndex < act.timesTable.getChildCount()) 
								? act.timesTable.getChildAt(rowIndex)
								: null;
					}
					ro.ds.tpt.model.Estimate.VehicleStatus vehicle = disembark.getVehicleStatus();
					act.updateVehicleArrivingBullet(vehicle, newRow, previousRow);					
					act.updateVehicleDepartingBullet(vehicle, newRow, nextRow);					
				}
			};
			disembark.startUpdate();
			act.queueUIUpdate(upd);
			ec = disembark.updateStation(act.getAppPreferences(), ec);
			act.queueUIUpdate(upd);
			return ec;
		}
		
		public void onClick(View v) {
			if (running.compareAndSet(false, true)) 
				new Thread(this).start();
		}
		
		public void killUpdate() {
			running.set(false);
			act.path.clearAllUpdates();
			if(err!=null) { 
				err.dismiss();
				err = null;
			}
		}
		
		public void setHasErrors() {
			hasErrors.set(true);
		}
		
		public boolean isRunning() {
			return running.get();
		}
	}
	
    private class UpdateView implements Runnable {
    	private long last=0;
    	
		public void run() {
			long crt = System.currentTimeMillis(); 
			if(crt>last && (crt-last)<500) return;
			last = crt;
			inflateTable();
		}
	}
    
    private class ShowStationConnections implements OnClickListener {
    	private ChangeOpportunity stop;
    	
    	public ShowStationConnections(ChangeOpportunity stop) {
    		this.stop = stop;
    	}
    	
		public void onClick(View v) {
			if (updater.isRunning()) 
				return;
			if (stop.hasConnections()) {
				stop.clearConnections();
			} else {
				path.clearConnections();
				stop.addAllChangeOpportunities();
			}
	    	runOnUiThread(new UpdateView());
		}
    }
    
    private class SelectConnectionKinds implements OnClickListener, DialogInterface.OnClickListener {
    	private List<List<Path>> pathList;
    	
		public void onClick(View v) {
			pathList = new ArrayList<List<Path>>();
		   	Set<Path> connections = new TreeSet<Path>(new Path.LabelComparator());
	    	for(ChangeOpportunity sel : path.getDisembarkOpportunities()) {
    			ro.ds.tpt.model.Station selStation = sel.getDisembarkEstimate().getStation();
	    		// (1) the paths passing through this exact same station
	    		Set<Path> stationPaths = new HashSet<Path>(selStation.getPaths());
	    		Path excludePath = path.getPath();
				for(Path p:stationPaths)
					if (p!=excludePath)
						connections.add(p);
    			// (2) not the empty junction (contains unrelated stations)
    			// Note that right now we actually do not have a junction with an empty name
    			// So the correct way of doing this would be to actually check that the distance 
    			// between sel.getStation() and any of sel.getStation().getJunction().getStations()
    			// is smaller than some given constant
    			if (selStation.getJunctionName().trim().length() == 0)
	    			continue;
    			// (3) the paths passing through the stations of the junction
	    		for(ro.ds.tpt.model.Station s:selStation.getJunction().getStations()) {
	    			if (s==selStation) continue; 
	    			boolean haveDistance = false;
	    			int dist = 0;
    				for(Path p:s.getPaths())
    					if (!stationPaths.contains(p) && p!=excludePath) {
							if(!haveDistance) {
								haveDistance = true;
								dist = selStation.distanceTo(s);
							}
							if (dist < Constants.MAX_CONNECTION_DIST)
								connections.add(p);
    					}
	    		}
	    	}
	    	
	    	List<String> labels = new ArrayList<String>();
	    	for(LineKind k : LineKind.values()) {
	    		List<Path> paths = new ArrayList<Path>();
	    		for(Path p:connections)
	    			if (k==p.getLine().getKind())
	    				paths.add(p);
	    		if(paths.size()>0) {
	    			// add all paths for this connection type
	    			pathList.add(paths);
	    			labels.add(  paths.size()>1 
	    				? getString(LineKindAndroidEx.getLabelId(k)) // more than one? give generic label
	    				: paths.get(0).getLabel()); // single one? use it's own label
	    		}
	    	}
	    	
			final CharSequence[] items = new CharSequence[labels.size()];
	    					
	    	Iterator<String> it = labels.iterator();
			for(int i=0;i<items.length;i++) 
				items[i] = it.next();

			new AlertDialog.Builder(ViewTimes.this)
				.setTitle(getString(R.string.selConnections))
				.setItems(items, this)
				.create()
				.show();
		}

		public void onClick(DialogInterface dialog, int which) {
			List<Path> connections = pathList.get(which);
			if (connections.size() > 1) {
				// more than one to select
				final CharSequence[] items = new CharSequence[connections.size()];
				
		    	Iterator<Path> it = connections.iterator();
				for(int i=0;i<items.length;i++) {
					Path p = it.next();
					items[i] = p.getLabel();
				}

				new AlertDialog.Builder(ViewTimes.this)
					.setTitle(getString(R.string.selConnections))
					.setItems(items, new SelectConnections(connections))
					.create()
					.show();
				
			} else {
				// a single connection of this type, no use popping a new dialog
		    	path.addConnections(connections.get(0));
		    	runOnUiThread(new UpdateView());
			}
		}
    }
    
    private class SelectConnections implements DialogInterface.OnClickListener {
    	private List<Path> pathList;
    	
    	public SelectConnections(List<Path> pathList) {
    		this.pathList = pathList;
		}
    	
		public void onClick(DialogInterface dialog, int which) {
	    	path.addConnections(pathList.get(which));
	    	runOnUiThread(new UpdateView());
		}
    }
    
    private static class ReportError implements Runnable, DialogInterface.OnClickListener {
		private AlertDialog dialog;
		private Context act;
		
		public ReportError(Context act) {
			this.act = act;;
		}
    	public synchronized void run() {
    		dismiss();
			dialog = new AlertDialog.Builder(act)
				.setMessage(R.string.upd_error)
				.setPositiveButton("Ok", this)
				.create();
    		dialog.show();
		}
    	public synchronized void dismiss() {
    		if (dialog!=null) 
    			dialog.dismiss();
			dialog = null;
    	}
    	
		public void onClick(DialogInterface dialog, int which) {
			dismiss();
		}
    }
    
    private class PathSwitcher implements View.OnClickListener {
		public void onClick(View v) {
        	Path p = path.getPath();
        	ArrayList<Path> paths = new ArrayList<Path>(p.getLine().getPaths());
        	paths.remove(p);
        	if(paths.size()==1) {
        		Path opposite = paths.get(0);
            	new StartActivity(ViewTimes.this, ViewTimes.class)
    	    		.addCity(city)
    	    		.addLinePath(opposite)
    	    		.replace();
        	} 
		}
    }
    
    private class FavoritePathToggle implements View.OnClickListener {
    	private Button favorite;
    	private boolean isFavorite;
    	
    	public FavoritePathToggle(Button favorite) {
    		this.favorite = favorite;
    		isFavorite = getAppPreferences().isFavoritePath(path.getPath()); 
			updateImage();
		}
    	
    	private void updateImage() {
    		if (isFavorite) 
    			favorite.setBackgroundResource(R.drawable.star_on_button);
    		else
    			favorite.setBackgroundResource(R.drawable.star_off_button);
    	}
    	
		public void onClick(View v) {
			isFavorite = !isFavorite;
			if (isFavorite) 
				getAppPreferences().addFavoritePath(path.getPath());
			else
				getAppPreferences().removeFavoritePath(path.getPath());
			updateImage();
		}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.times_menu, menu);
        return true;
    }    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.switch_direction:
        	Path p = path.getPath();
        	ArrayList<Path> paths = new ArrayList<Path>(p.getLine().getPaths());
        	paths.remove(p);
        	if(paths.size()==1) {
        		Path opposite = paths.get(0); 
            	new StartActivity(this, ViewTimes.class)
    	    		.addCity(city)
    	    		.addLinePath(opposite)
    	    		.replace();
        	} 
            return true;
        case R.id.app_settings: 
        	launchPrefs();
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
}