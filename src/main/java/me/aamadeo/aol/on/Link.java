package me.aamadeo.aol.on;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Transient;


@Entity
public class Link implements Comparable<Link> {

	@OneToMany(cascade=CascadeType.ALL)
	@OrderBy("wavelength ASC")
	private Set<LightLink> links = new HashSet<LightLink>();
	
	@Transient
	private Set<LightLink> necessaryLinks = new HashSet<LightLink>();
			
	@Id 
	@GeneratedValue 
	private int id;
	
	@Transient
	private int extraFibers = 0;

	@Transient
	private int extraWavebands = 0;

	@Transient
	private int extraWavelengths = 0;

	@Transient
	private int freeBandwidth = 0;
	
	private int cost;
	
	@ManyToOne(cascade=CascadeType.ALL)
	private Node origin;
	
	@ManyToOne(cascade=CascadeType.ALL)
	private Node destination;
	
	@ManyToOne(cascade=CascadeType.ALL)
	private Network network;
	
	@Transient
	private boolean blocked = false;
	
	public Link(){}

	public Link(Node origin, Node destination, Network network) {
		this.origin = origin;
		this.destination = destination;
		this.extraFibers = 0;
		this.extraWavebands = 0;
		this.extraWavelengths = 0;
		this.network = network;
		this.freeBandwidth = network.getBandWidth();

		this.links.clear();
		crearEnlaces(network);
		this.necessaryLinks.addAll(this.links);
	}
	
	public Link clone(Map<String,Node> nodoMap, Network network){
		Link clone = new Link();
		clone.id = this.id;
		clone.origin = nodoMap.get(this.origin.getLabel());
		clone.destination = nodoMap.get(this.destination.getLabel());
		clone.extraFibers = 0;
		clone.extraWavebands = 0;
		clone.extraWavelengths = 0;
		clone.network = network;
		clone.freeBandwidth = network.getBandWidth();

		clone.cost = this.cost;
		
		clone.origin.addLink(clone);
		clone.links = new HashSet<LightLink>();
		
		for( LightLink lightlink : links){
			clone.links.add(lightlink.clone(clone));
		}
		
		clone.necessaryLinks = new HashSet<LightLink>();
		clone.necessaryLinks.addAll(clone.links);
		
		return clone;
	}
	
	public void crearEnlaces(Network network){
		for (int f = 0; f < network.getF(); f++) {
			for (int b = 0; b < network.getB(); b++) {
				for(int w = 0; w < network.getW(); w++){
					for(int t = 0; t < network.getT(); t++){
						this.links.add(new LightLink(f, b, w, t, this));
					}
				}
			}
		}
	}

	public void initialize() {
		this.unlock();
		this.extraFibers = 0;
		this.extraWavebands = 0;
		this.extraWavelengths = 0;
		
		this.necessaryLinks.clear();
		this.necessaryLinks.addAll(links);
		
		for (LightLink e : links) e.initialize();
	}
	
	public Network getNetwork() {
		return network;
	}

	public void setNetwork(Network network) {
		this.network = network;
	}

	public Node getDestination() {
		return destination;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public boolean contains(LightLink e){
		return necessaryLinks.contains(e);
	}
	
	public void addExtraFiber(Network network){
		this.extraFibers++;
		
		int newFiberId = network.getF() + extraFibers - 1;
		
		for (int b = 0; b < network.getB() + extraWavebands; b++ ){
			for (int w = 0; w < network.getW() + extraWavelengths; w++ ){
				for (int t = 0; t < network.getT(); t++ ){
					necessaryLinks.add( new LightLink(newFiberId, b, w, t, this) );
					freeBandwidth++;
				}
			}
		}
	}
	
	public void addExtraBand(Network network){
		this.extraWavebands++;
		
		int newBandId = network.getB() + extraWavebands - 1;
		
		for (int f = 0; f < network.getF() + extraFibers; f++ ){
			for (int w = 0; w < network.getW() + extraWavelengths; w++ ){
				for (int t = 0; t < network.getT(); t++ ){
					necessaryLinks.add( new LightLink(f, newBandId, w, t, this) );
					freeBandwidth++;
				}
			}
		}
	}
	
	public void addExtraWavelength(Network network){
		this.extraWavelengths++;
		
		int newWavelengthId = network.getW() + extraWavelengths - 1;
		
		for (int f = 0; f < network.getF() + extraFibers; f++ ){
			for (int b = 0; b < network.getB() + extraWavebands; b++ ){
				for (int t = 0; t < network.getT(); t++ ){
					necessaryLinks.add( new LightLink(f, b, newWavelengthId, t, this) );
					freeBandwidth++;
				}
			}
		}
	}
	
	public int getExtraFibers(){
		return this.extraFibers;
	}
	
	public int getExtraWavebands(){
		return this.extraWavebands;
	}

	public int getExtraWavelengths(){
		return this.extraWavelengths;
	}

	public void setDestino(Node b) {
		this.destination = b;
	}

	public Node getOrigin() {
		return this.origin;
	}

	public void setOrigin(Node a) {
		this.origin = a;
	}
		
	public synchronized LightLink getFreeLightLink(){
		if(blocked) return null;
		
		LightLink [] disponibles = new LightLink[necessaryLinks.size()];
		
		int i = 0;
		
		for(LightLink e: this.necessaryLinks){
			if(!e.isLocked()){
				disponibles[i++] = e;
			}
		}
		
		int sorteado = (int)(Math.random()*((double)i));
		
		return disponibles[sorteado];
	}
	
	private class PreferenceComparator implements Comparator<LightLink>{
		
		private LightLink base;

		public int compare(LightLink arg0, LightLink arg1) {
			
			return base.distance(arg0) - base.distance(arg1);
		}
		
		
	}
	
	public synchronized LightLink getFreeLightLink(LightLink base){
		PreferenceComparator pc = new PreferenceComparator();
		pc.base = base;
		TreeSet<LightLink> candidatos = new TreeSet<LightLink>(pc);
		
		for(LightLink e : necessaryLinks){
			
			if ( base.distance(e) == 0 ) {
				if (! e.isLocked() )return e;
			}
			
			if ( e.getLightPathID() >= 0 || e.isLocked() ) continue;
			
			candidatos.add(e);
		}
		
		if ( candidatos.isEmpty() ) System.err.println("Buscando a " + base);
		
		return candidatos.isEmpty() ? null : candidatos.first();
	}

	public void lock() {
		this.blocked = true;
	}

	public void unlock() {
		this.blocked = false;
	}

	public boolean isLocked() {
		return this.blocked;
	}

	public int getCost() {
		return cost;
	}

	public void setCost(int cost) {
		this.cost = cost;
	}

	public Set<LightLink> getLinks() {
		return links;
	}
	
	public LightLink updateReference(LightLink base){
		for( LightLink e: necessaryLinks){
			if( e.equals(base) ) return e;
		}
		
		return null;
	}

	public void setLinks(Set<LightLink> links) {
		this.links = links;
		this.necessaryLinks.clear();
		this.necessaryLinks.addAll(links);
	}

	@Override
	public String toString() {
		return origin + "-" + destination;
	}
	
	public int getUsage(){
		double total = 0;
		double used = 0;

		for(LightLink e : necessaryLinks){
			total+=1;
			if(e.isLocked()) used+=1;
		}
		
		return (int) (100.0*used/total);
	}

	public int compareTo(Link arg0) {
		int cmpOrigen = origin.compareTo(arg0.origin);
		
		if ( cmpOrigen != 0 ) return cmpOrigen;
		
		return destination.compareTo(arg0.destination);
	}

	public synchronized boolean itsCapable(int bandwidth){
        int k = 1;
        for( LightLink e: necessaryLinks){
            if ( ! ( e.isLocked() || e.getLightPathID() >= 0) ) {
                if ( k == bandwidth ) {
                	return true;
                } else {
                	k++;
                }
            }
        }

        return false;
	}
	
	public int getF(){
		return network.getB();
	}

	public int getB(){
		return network.getB();
	}
	
	public int getW(){
		return network.getW();
	}
	
	public int getT(){
		return network.getT();
	}
	
	public int getFreeBandwidth(){
		return this.freeBandwidth;
	}
	
	public void reduceBandwidth(){
		this.freeBandwidth--;
	}
	
	public void increaseBandwidth(){
		this.freeBandwidth++;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Link)) return false;
		
		Link b = (Link) obj;
		
		return this.origin.equals(b.origin) && this.destination.equals(b.destination);
	}
	
	public boolean isConcurrentWith(Link b){
		if ( this == b ) {
			return true;
		}
		
		if ( b.origin == this.origin) {
			return true;
		}
		if ( b.destination == this.destination ) {
			return true;
		}
		
		Iterator<LightLink> resourcesA = (new TreeSet<LightLink>(links)).iterator();
		Iterator<LightLink> resourcesB = (new TreeSet<LightLink>(b.links)).iterator();
		
		while ( resourcesA.hasNext() && resourcesB.hasNext() ){
			LightLink resourceA = resourcesA.next();
			LightLink resourceB = resourcesB.next();
			
			if ( resourceA == resourceB ) {
				return true;
			}
			if ( resourceA.isConcurrentWith(resourceB) ) {
				return true;
			}
		}
		
		if ( links == b.links){
			return true;
		}
		
		if ( necessaryLinks == b.necessaryLinks){
			return true;
		}

		
		return false;
	}
}
