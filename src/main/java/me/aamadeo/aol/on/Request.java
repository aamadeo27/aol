package me.aamadeo.aol.on;

import me.aamadeo.aol.optimization.Scenario;

import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.ManyToOne;
import javax.persistence.CascadeType;
import javax.persistence.Id;

@Entity
public class Request implements Comparable<Request>{
	
	@Id
	@GeneratedValue
	private long id;
	
	@ManyToOne(cascade=CascadeType.ALL)
	private Node origin;
	
	@ManyToOne(cascade=CascadeType.ALL)
	private Node destination;
	
	private int badnwidth;
	
	@ManyToOne(cascade=CascadeType.ALL)
	private Scenario scenario;
	
	public Request(){}
	
	/**
	 * The Request has an scenario id that identifies the scenario it belongs to,
	 * the origin and the destination nodes that require connectivity, and the bandwidth
	 * that it needs.
	 *
	 * @param scenario	    Bandwidth scenario
	 * @param origin		Node Origin
	 * @param destination	Node Destino
	 * @param badnwidth		Ancho de banda solicitado.
	 */
	public Request(Scenario scenario, Node origin, Node destination, int badnwidth){
		this.origin = origin;
		this.destination = destination;
		this.badnwidth = badnwidth;
		this.scenario = scenario;
	}
	
	public boolean isConcurrentWith(Request b){
		if ( this == b ) {
			return true;
		}
		
		if ( this.origin == b.origin ) {
			return true;
		}

		if ( this.origin.isConcurrentWith(b.origin) ) {
			return true;
		}
		
		if ( this.destination == b.destination ) {
			return true;
		}

		if ( this.destination.isConcurrentWith(b.destination) ) {
			return true;
		}
		
		return false;
	}
	
	public Request clone(Map<String,Node> nodoMap, Scenario scenario){
		Request clone = new Request();
		clone.id = this.id;
		
		clone.origin = nodoMap.get(this.origin.getLabel());
		clone.destination = nodoMap.get(this.destination.getLabel());
		clone.badnwidth = this.badnwidth;
		clone.scenario = scenario;
		
		return clone;
	}
	
	public Network getNetwork(){
		return scenario.getNetwork();
	}

	public Node getOrigin() {
		return origin;
	}

	public Node getDestino() {
		return destination;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setOrigen(Node origin) {
		this.origin = origin;
	}

	public void setDestino(Node destination) {
		this.destination = destination;
	}

	@Override
	public int hashCode() {
		return origin.hashCode()*100 + destination.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Request)) return false;
		
		Request b = (Request) obj;
		
		return origin.equals(b.origin) && destination.equals(b.destination);
	}
	
	@Override
	public String toString() {
		return origin + "_a_" + destination + "." + badnwidth;
	}

	public int compareTo(Request s) {
		int cmpADB = this.badnwidth - s.badnwidth;
		
		if (cmpADB != 0) return cmpADB;
		
		return hashCode() - s.hashCode();
	}
	
	public int getBadnwidth() {
		return badnwidth;
	}

	public void setBadnwidth(int badnwidth) {
		this.badnwidth = badnwidth;
	}
}
