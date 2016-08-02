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
package ro.ds.tpt.model;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ro.ds.util.BPInputStream;
import ro.ds.util.BPMemoryOutputStream;
import ro.ds.util.BPOutputStream;
import ro.ds.util.LineKind;

public class Line extends PersistentEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	private int id;
	private String name;
	private Set<ro.ds.tpt.model.Path> paths;
	private Map<String, ro.ds.tpt.model.Path> pathNames;
	private ro.ds.tpt.model.Path first;
	
	protected Line(int id, String name) {
		this(id, name,-1,null);
	}
	
	private Line(int id, String name, long resId, City city) {
		super(resId, city);
		this.id = id;
		this.name = name;
		this.paths = new HashSet<ro.ds.tpt.model.Path>();
		this.pathNames = new HashMap<String, ro.ds.tpt.model.Path>();
	}
	
	public List<String> getSortedPathNames() {
		ensureLoaded();		
		List<String> pathNames = new ArrayList<String>();
		for(ro.ds.tpt.model.Path p : paths)
			pathNames.add(p.getNiceName());
		Collections.sort(pathNames);
		return pathNames;
	}
	
	public String getPathNames() {
		boolean first = true;
		String pathNames = "";
		for(String pathName : getSortedPathNames())
			if (first) {
				pathNames += pathName;
				first = false;
			} else
				pathNames += ", "+pathName;
		return pathNames;
	}
	
	
	@Override
	public String toString() {
		return "Line: "+name+"["+getPathNames()+"]("+Integer.toHexString(hashCode())+")";
	}
	
	public LineKind getKind() {
		return LineKind.getKind(this);
	}
	
	public boolean isBarred() {
		return LineKind.isLineBarred(this);
	}
	
	public boolean isRouteConvex() {
		return LineKind.isRouteConvex(this);
	}
	
	public ro.ds.tpt.model.Path getPath(String name) {
		ensureLoaded();		
		return pathNames.get(name);
	}
	
	public ro.ds.tpt.model.Path getFirstPath() {
		ensureLoaded();		
		assert(first!=null);
		return first;
	}
	
	public Collection<ro.ds.tpt.model.Path> getPaths() {
		ensureLoaded();		
		return paths;
	}

	public void addPath(ro.ds.tpt.model.Path p) {
		addEagerPath(p);
		pathNames.put(p.getName(), p);
	}
	
	void addEagerPath(ro.ds.tpt.model.Path p) {
		if(paths.isEmpty()) 
			first = p;
		assert p.getLineName().equals(name);
		paths.add(p);
	}
	
	// merges stations from the empty-name-path to the other paths
	public void pathMerge() {
		if(getPaths().size()<=1) return;
		ro.ds.tpt.model.Path tailsPath = getPath("");
		if(tailsPath==null) return;
		
		paths.remove(tailsPath);
		pathNames.remove("");
		for(ro.ds.tpt.model.Path p:paths) {
			for(ro.ds.tpt.model.Estimate e : tailsPath.getEstimatesByPath())
				p.concatenate(e.getStation(), e.getPlan());
		}
	} 
	
	public Set<ro.ds.tpt.model.Station> getStations() {
		ensureLoaded();		
		Set<ro.ds.tpt.model.Station> all = new LinkedHashSet<ro.ds.tpt.model.Station>();
		for(ro.ds.tpt.model.Path p: paths)
			for(ro.ds.tpt.model.Estimate e : p.getEstimatesByPath())
				all.add(e.getStation());
		return all;
	}
	
	public String getName() {
		return name;
	}
	
	public int getId() {
		return id;
	}
	
	public boolean isFake() {
		for (ro.ds.tpt.model.Path p: paths)
			if (p.isFake())
				return true;
		return false;
	}

	@Override
	protected void loadLazyResources(BPInputStream res) throws IOException {
		for(ro.ds.tpt.model.Path p: paths)
			pathNames.put(p.getName(), p);
	}


	@Override
	protected void saveLazyResources(BPMemoryOutputStream lazy) throws IOException {
		for(ro.ds.tpt.model.Path p:paths) {
			assert p.getLineName().equals(name);
		}
	}

	@Override
	public void saveEager(BPOutputStream eager) throws IOException {
		// eager line resources
		eager.writeObjectId(id);
		eager.writeString(name);
	}

	public static Line loadEager(BPInputStream eager, int resId, City city) throws IOException {
		return new Line(eager.readObjectId(), eager.readString(), resId, city);
	}
}

