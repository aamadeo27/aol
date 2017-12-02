package me.aamadeo.aol.on;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.OneToMany;
import javax.persistence.ManyToOne;
import javax.persistence.CascadeType;
import javax.persistence.Id;
import javax.persistence.OrderBy;


/**
 * Clase Path, representa un camino por su nodo origin, y una lista de
 * lightLinks que debe seguir
 * @author albert
 *
 */
@Entity
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
	
	/**
	 * Constructor principal
	 * @param origin
	 */
	public Path(Node origin){
		this.origin = origin;
		this.destination = origin;
		this.hops.clear();
		this.distance = 0;
	}
	
	/**
	 * Constructor apartir de un camin existente
	 * @param c	Path existente
	 */
	public Path(Path c) {
		this.origin = c.origin;
		
		this.hops = new TreeSet<Hop>();
		this.hops.addAll(c.hops);
		this.destination = c.destination;
		this.distance = c.distance;
	}
	
	/**
	 * Metodo para agregar un hop al camino
	 * @param hop
	 */
	public void addHop(Hop hop){
		hops.add(hop);
		
		if (destination == null) destination = origin;
		
		destination = hop.getDestination();
		distance += hop.getLink().getCost();
	}
	
	/**
	 * Utilizado para obtener el destination del camino
	 * @return El ultimo nodo visitado en el camino
	 */
	public Node getDestination(){
		return this.destination;
	}
	
	/**
	 * Utilizado para obtener la longitud del camino en hops
	 * @return La cantidad de hops, es decir el tama�o de la lista hops
	 */
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
	
	public void addResources(int bandwidth, Network network){
		for(Hop s: hops){
			Link c = s.getLink();
			
			if ( ! c.itsCapable(bandwidth) ){
				switch(Configuration.getIncreaseDimension()){
				case Configuration.FIBER_INCREASE : 
					c.addExtraFiber(network);

					break;
				case Configuration.BAND_INCREASE :
					c.addExtraBand(network);

					break;
				case Configuration.WAVELENGTH_INCREASE :
					c.addExtraWavelength(network);

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
