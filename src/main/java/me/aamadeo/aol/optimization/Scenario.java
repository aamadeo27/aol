package me.aamadeo.aol.optimization;

import me.aamadeo.aol.on.Network;
import me.aamadeo.aol.on.Node;
import me.aamadeo.aol.on.Request;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.*;

@Entity
@Table(name="Scenario")
public class Scenario {
	
	@Id
	private String name;
	
	@ManyToOne
	private Network network;
	
	@OneToMany(cascade=CascadeType.ALL)
	Set<Request> Requests;
	
	public Scenario clone(){
		Scenario clone = new Scenario(network.clone());
		clone.name = this.name;
		
		HashMap<String,Node> nodeMap = new HashMap<String,Node>();
		
		for(Node node : clone.network.getNodes()){
			nodeMap.put(node.getLabel(), node);
		}
		
		for(Request s : Requests){
			clone.Requests.add( s.clone(nodeMap, clone) );
		}
		
		return clone;
	}

	public Network getNetwork() {
		return network;
	}

	public void setNetwork(Network network) {
		this.network = network;
	}

	public Set<Request> getRequests() {
		return Requests;
	}

	public void setRequests(Set<Request> Requests) {
		this.Requests = new TreeSet<Request>(Requests);
	}
	
	public Scenario(){}
	
	/**
	 * Crea un caso randomico.
	 * 
	 * @param network			Network
	 * @param requestCount		Cantidad de Requests que se generaran
	 */
	public Scenario(Network network, int requestCount){
		this.network = network;
		
		Requests = new TreeSet<Request>();
				
		for(int i = 0 ; i < requestCount; i++){
			Request s = null;
			
			Node origin = network.randomNode();
			Node destination = network.randomNode();
			
			while (origin.equals(destination)) destination = network.randomNode();
						
			int bandwidth = (int) (Math.random()*(network.getW()*network.getT()));
			s = new Request(this, origin, destination, bandwidth);
			Requests.add(s);
		}
	}
	
	public Scenario(Network network){
		this.network = network;
		Requests = new TreeSet<Request>();
	}

	/**
	 * Create a request for every {origin,destination} pair
	 * @param bandwidth
	 */
	public void createRequests(int bandwidth ){
		for(Node origin: network.getNodes()){
			for(Node destination: network.getNodes()){
				if ( origin.equals(destination)) continue;
				
				Requests.add( new Request(this, origin, destination, bandwidth) );
			}
		}
	}
	
	@Override
	public String toString() {
		return name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
