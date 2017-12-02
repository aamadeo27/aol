package me.aamadeo.aol.on;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.*;


@Entity
@Table(name="Path")
public class Path {
	
	//public static String BUFFER_DEBUG = "";
	
	@Id
	@GeneratedValue
	private long id;
	
	@ManyToOne(cascade=CascadeType.ALL)
	private Node origin;
	
	@ManyToOne(cascade=CascadeType.ALL)
	private Node destination;
	
	@OneToMany(cascade=CascadeType.ALL)
	@OrderBy("sequence ASC")
	private Set<Hop> hops = new TreeSet<Hop>();
	
	private int distance = 0;
	
	public Path(){ }

	public Path(Node origin){
		this.origin = origin;
		this.destination = origin;
		this.hops.clear();
		this.distance = 0;
	}

	public Path(Path p) {
		this.origin = p.origin;
		
		this.hops = new TreeSet<Hop>();
		this.hops.addAll(p.hops);
		this.destination = p.destination;
		this.distance = p.distance;
	}

	public void addHop(Hop hop){
		hops.add(hop);
		
		if (destination == null) destination = origin;
		
		destination = hop.getDestination();
		distance += hop.getLink().getCost();
	}

	public Node getDestination(){
		return this.destination;
	}

	public int getDistance(){
		return this.distance;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	public Set<Hop> getHops(){
		return hops;
	}
	
	public void setHops(Set<Hop> hops){
		this.hops = hops;
	}

	public void setDestino(Node destination) {
		this.destination = destination;
	}

	/**
	 * It evaluates the need to change the network allowing more resources
	 * per link, and adapt the network on this path according to the
	 * IncreaseDimension given by the Configuration of the Scenario.
	 *
	 * @param bandwidth
	 * @param network
	 */
	public void addResources(int bandwidth, Network network){
		for(Hop s: hops){
			Link link = s.getLink();
			
			if ( ! link.itsCapable(bandwidth) ){
				switch(Configuration.getIncreaseDimension()){
				case Configuration.FIBER_INCREASE : 
					link.addExtraFiber(network);

					break;
				case Configuration.BAND_INCREASE :
					link.addExtraBand(network);

					break;
				case Configuration.WAVELENGTH_INCREASE :
					link.addExtraWavelength(network);

					break;
				}
			}
		}
	}

	public void lockUsedLinks(){
		for(Hop hop : hops) hop.getLink().lock();
	}
	
	public void unlockUsedLinks(){
		for(Hop hop : hops){
			hop.getLink().unlock();
		}
	}
	
	public void lockNodes(){
		this.origin.lock();
		
		for(Hop hop : hops) hop.getDestination().lock();
	}
	
	public void unlockNodes(){
		this.origin.unlock();
		
		for(Hop hop : hops) hop.getDestination().unlock();
	}
	
	public void unlockLightLinks(){
		for(Hop hop : hops) hop.unlockLightLinks();
	}

	public Set<Link> getLinks(){
		Set<Link> links = new HashSet<Link>();
		
		for(Hop s: hops){
			links.add(s.getLink());
		}
		
		return links;
	}

	public Node getOrigin() {
		return origin;
	}

	public void setOrigin(Node origin) {
		this.origin = origin;
	}
	
	public void concat(Path c){
		if ( ! c.origin.equals(this.destination) ) return;
		
		int sequence = this.hops.size()+1;
		
		for(Hop s: c.hops){
			Hop newHop = new Hop(sequence++, s.getLink());
			this.hops.add(newHop);
		}
		
		this.distance = distance + c.distance;
		this.destination = c.destination;
	}

	/**
	 * Assign the resources of this path to the service that it belongs to.
	 * @param network
	 */
	public void commitLinks(Network network){
		for(Hop hop : hops){
			Link link = hop.getLink();
			Set<LightLink> lightLinks = new HashSet<LightLink>();
			
			for (LightLink e : hop.getLightLinks()) {
				if( ! link.contains(e) ){
					int extraResourcesNeeded = (e.getFiber()+1) - ( network.getF() + link.getExtraFibers() );
					for ( int i = 0; i < extraResourcesNeeded; i++ ) link.addExtraFiber(network);
					
					extraResourcesNeeded = (e.getWaveband()+1) - ( network.getB() + link.getExtraWavebands() );
					for ( int i = 0; i < extraResourcesNeeded; i++ ) link.addExtraBand(network);
					
					extraResourcesNeeded = (e.getWavelength()+1) - ( network.getW() + link.getExtraWavelengths() );
					for ( int i = 0; i < extraResourcesNeeded; i++ ) link.addExtraWavelength(network);

					e = link.updateReference(e);
				}
				
				lightLinks.add(e);
				
				e.lock();
			}
			
			hop.setLightLinks(lightLinks);
		}
	}
		
	public void comitLightLinks(int bandwidth, long serviceID){
		LightLink [] e = new LightLink[bandwidth];
		for(Hop hop : hops) hop.commitLightLinks(e, serviceID);
	}

	@Override
	public String toString(){
		String camino = origin.toString();
		
		for(Hop s: hops) camino = camino + "-" + s.getDestination();
		
		return camino;
	}
	
	public boolean itsCapable(int badnwidth){
		for(Hop j : hops){
			if (! j.getLink().itsCapable(badnwidth)) return false;
		}
		
		return true;
	}
}
