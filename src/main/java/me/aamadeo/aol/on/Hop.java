package me.aamadeo.aol.on;

import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;


@Entity
public class Hop implements Comparable<Hop>{

	@Transient
	private Link link;
	
	@ManyToMany(cascade=CascadeType.ALL)
	private Set<LightLink> lightlinks = new TreeSet<LightLink>();
	
	private int sequence;
	
	@Id
	@GeneratedValue
	private long id;
	
	public Hop(){}
	
	public Hop(int sequence, Link c){
		this.link = c;
		this.sequence = sequence;
	}
	
	public int getSequence() {
		return sequence;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}
	
	@Override
	public int hashCode() {
		return link.hashCode()*100 + sequence;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof Hop)) return false;
		
		Hop b = (Hop) obj;
		
		return this.hashCode() == b.hashCode();
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Link getLink() {
		return link;
	}
	
	public Node getSource(){
		return link.getOrigin();
	}
	
	public Node getDestination(){
		return link.getDestination();
	}

	public void setLink(Link c) {
		this.link = c;
		this.lightlinks.clear();
	}

	public Set<LightLink> getLightLinks() {
		return lightlinks;
	}

	public void setLightLinks(Set<LightLink> lightlinks) {
		this.lightlinks = lightlinks;
	}

	public int compareTo(Hop b) {
		return this.sequence - b.sequence;
	}

	public void commitLightLinks(LightLink [] e, long sid){

		for (int k = 0; k < e.length; k++){
			if ( e[k] == null ) {
				if( k == 0 ){
					e[k] = link.getFreeLightLink();
				} else {
					e[k] = e[k-1].next();
				}
			} else {
				e[k] = link.getFreeLightLink(e[k]);
			}
			
			link.getDestination().receive(e[k]);
			link.getOrigin().transmit(e[k]);
			
			lightlinks.add(e[k]);
			
			e[k].lock();
			e[k].setLightPathID(sid + k);
		}
	}
	
	public void unlockLightLinks(){
		for( LightLink l : lightlinks ){
			l.unlock();
			l.setLightPathID(-1);
		}
	}
	
	@Override
	public String toString() {
		return "("+ sequence +","+link+")";
	}
}
