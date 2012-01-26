package net.sf.vcsstat.graph;


import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class DataTable {
		
	private static class XEntry{
		SortedMap<String, Integer> series2y = new TreeMap<String, Integer>(){
			@Override
			public Integer get(Object key) {
				Integer ret = super.get(key);
				return (ret == null) ? Integer.valueOf(0) : ret;
			};
		};

		public void compact(Set<String> seriesToKeep, String label) {
			int sum = 0;
			Set<String> keySet = new TreeSet<String>(series2y.keySet());
			for (String series : keySet ) {
				if(!seriesToKeep.contains(series)){
					sum += series2y.get(series);
					series2y.remove(series);
				}
			}
			series2y.put(label, sum);
		}
	}
	SortedMap<String, XEntry> x2entry = new TreeMap<String, DataTable.XEntry>(){
		@Override
		public XEntry get(Object key) {
			XEntry ret = super.get(key);
			if(ret == null){
				ret = new XEntry();
				put((String) key, ret);
			}
			return ret;
		};
	};
	SortedMap<String, Integer> series2lastY = new TreeMap<String, Integer>();
	boolean isComplete;
	private final String xLabel;
	public DataTable(String xLabel) {
		this.xLabel = xLabel;
	}

	public void set(String series, String x, int y){
		series2lastY.put(series, 0);
		x2entry.get(x).series2y.put(series, y);
		isComplete = false;
	}
	
	public void add(String series, String x, int y){
		set(series, x, y + x2entry.get(x).series2y.get(series));
	}
	

	public String asGoogleChart(){
		complete();
		StringBuilder ret = new StringBuilder();
		addColumn(ret, xLabel, "string");
		for(String series : series2lastY.keySet()) {
			addColumn(ret, series, "number");
		}
		ret.append("data.addRows([\n");
		ret.append("");
		
		for(String x : x2entry.keySet()){
			ret.append("\t\t\t['"+x+"'");
			for(Integer y : x2entry.get(x).series2y.values()){
				ret.append(", "+y);
			}
			ret.append("],\n");
		}
		ret.append("]);\n");
		return ret.toString();
	}

	private static final void addColumn(StringBuilder ret, String series, String type) {
		ret.append( "data.addColumn('"+type +"', '");
		ret.append(series);
		ret.append( "');\n");
	}

	
	public DataTable compact(final String label, final int max) {
		complete();
		XEntry sum = new XEntry();
		for(XEntry entry : x2entry.values()) {
			for(String series : series2lastY.keySet()) {
				Integer s = sum.series2y.get(series);
				Integer y = entry.series2y.get(series);
				sum.series2y.put(series, y+s);
			}
		}
		SortedSet<Integer> top = new TreeSet<Integer>(sum.series2y.values());
		if(top.size() > max) {		
			Integer minY = (Integer) top.toArray()[top.size()-max];
			Set<String> seriesToKeep = new TreeSet<String>(sum.series2y.keySet());
			for(String series : sum.series2y.keySet()){
				if(sum.series2y.get(series) < minY){
					seriesToKeep.remove(series);
					series2lastY.remove(series);
				}
			}
			for(XEntry entry : x2entry.values()) {
				entry.compact(seriesToKeep, label);
			}
			series2lastY.put(label, 0);
		}
		
		isComplete = true;
		return this;		
	}
	
	
	private void complete() {
		if(isComplete) return;
		isComplete = true;
		for(XEntry entry : x2entry.values()){
			for(String series : series2lastY.keySet()) {
				Integer y = entry.series2y.get(series);
				if(y < 1){
					entry.series2y.put(series, series2lastY.get(series));
				} else {
					series2lastY.put(series, y);
				}
			}
		}
	}
		
	public String asHighChart(){
		complete();
		StringBuilder ret = new StringBuilder();
		ret.append("categories: [");		
		for(String x : x2entry.keySet()){
			ret.append("'"+x+"', ");
		}
		ret.append("],\n");
		
		for(String series : series2lastY.keySet()){
			ret.append("\n\t{\n\tname: '"+series+"',\n\tdata: [");
			for(XEntry entry : x2entry.values()){
				ret.append(entry.series2y.get(series)+", ");
			}
			ret.append("]\n\t},\n");
		}

		return ret.toString();
	}
	
	
	
	public static void main(String[] args) {
		DataTable data = new DataTable("Year");
		data.set("Milk", "2001", 200);
		data.set("Water", "2001", 50);
		data.set("Milk", "2002", 300);
		data.set("Milk", "2003", 200);
		data.set("Water", "2003", 150);
		data.set("Milk", "2000", 1000);
		data.set("Milk", "2000", 100);
		data.add("Milk", "2000", 100);
		data.set("Oil", "2000", 150);
		data.set("Oil", "2003", 150);
		data.set("One", "2000", 1);
		data.set("One", "2001", 1);
			
		//System.out.println(data.asGoogleChart());
		System.out.println(data.asHighChart());
		System.out.println(data.compact("Other",1).asHighChart());
	}
	
	
}
