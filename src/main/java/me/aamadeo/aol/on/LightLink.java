package me.aamadeo.aol.on;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

@Entity
public class LightLink implements Comparable<LightLink>{
	
	//public static String BUFFER_DEBUG = null;

	@Id
	@GeneratedValue
	private long id;
	
	private int fiber;
	private int waveband;
	private int wavelength;
	private int timeSlot;
	
	@ManyToOne(cascade=CascadeType.ALL)
	private Link link;
	
	@Transient
	private boolean blocked = false;
	
	@Transient
	private long lightPathID = 0;
	
	public LightLink(){
		this.lightPathID = -1;
	}

	/**
	 *
	 * @param f 	Fiber identifier
	 * @param b 	Waveband identifier
	 * @param w 	Wavelength identifier
	 * @param t 	Timeslot identifier
	 * @param link	Link in the network
	 */
	public LightLink(int f, int b, int w, int t, Link link) {
		this.fiber = f;
		this.waveband = b;
		this.wavelength = w;
		this.timeSlot = t;
		this.link = link;
		this.lightPathID = -1;
	}
	
	public LightLink(LightLink base){
		this.fiber = base.fiber;
		this.waveband = base.waveband;
		this.wavelength = base.wavelength;
		this.timeSlot = base.timeSlot;
		this.link = base.link;
		this.lightPathID = -1;
	}
	
	public LightLink next(){
		int [] dimensiones = { link.getT(), link.getW(), link.getB(), link.getF() + link.getExtraFibers()};
		int [] spec = {timeSlot, wavelength, waveband, fiber};
		
		int delta = 1;
		for(int i = 0; i < 4; i++){
			 spec[i] = ( spec[i] + delta ) % dimensiones[i];
			 delta = ( spec[i] + delta ) / dimensiones[i];
		}
		
		return new LightLink(spec[3], spec[2], spec[1],spec[0] , link);
	}

	public int getFiber() {
		return fiber;
	}

	public int getWaveband() {
		return waveband;
	}

	public int getWavelength() {
		return this.wavelength;
	}

	public int getTimeSlot() {
		return timeSlot;
	}

	public Node getSource() {
		return link.getOrigin();
	}

	public Node getDestination() {
		return link.getDestination();
	}

	public void initialize() {
		this.blocked = false;
		this.lightPathID = -1;
	}

	public Link getLink() {
		return link;
	}

	public void setFiber(int fiber) {
		this.fiber = fiber;
	}

	public void setWaveband(int waveband) {
		this.waveband = waveband;
	}
	
	public void setWavelength(int wavelength) {
		this.wavelength = wavelength;
	}

	public void setTimeSlot(int timeSlot) {
		this.timeSlot = timeSlot;
	}
	
	public void setLink(Link link) {
		this.link = link;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public boolean isLocked() {
		return blocked;
	}
	
	public void lock(){
		this.blocked = true;
		this.link.reduceBandwidth();
	}
	
	public void unlock(){
		this.link.increaseBandwidth();
		this.blocked = false;
	}
	
	@Override
	public String toString() {
		return link + ".f" + fiber + ".b" + waveband + ".w" + wavelength + ".t" + timeSlot;
	}
		
	@Override
	public boolean equals(Object arg0) {
		if(!(arg0 instanceof LightLink)) return false;
		
		LightLink e = (LightLink) arg0;
		
		boolean resultado = e.link.equals(link) && 
			   e.fiber == fiber &&
			   e.waveband == waveband &&
			   e.wavelength == wavelength &&
			   e.timeSlot == timeSlot;
		
		return resultado;
	}

	public int compareTo(LightLink e) {
		int ordenE = e.waveband*10000 + e.timeSlot *100 + e.wavelength;
		int ordenA = waveband*10000 + timeSlot *100 + wavelength;
		
		if (ordenE != ordenA) return ordenA - ordenE;
		
		return 1;
	}
	
	@Override
	public int hashCode() {
		return timeSlot + wavelength*10 + waveband * 100;
	}

	public long getLightPathID() {
		return lightPathID;
	}

	public void setLightPathID(long lightPathID) {
		this.lightPathID = lightPathID;
	}
	
	public int distance(LightLink e){
		int d = 0;
		d += this.getWaveband() != e.getWaveband() ? 1 : 0;
		d += this.getWavelength() != e.getWavelength() ? 10 : 0;
		d += this.getTimeSlot() != e.getTimeSlot() ? 100 : 0;

		return d;
	}
	
	public boolean [] convert(LightLink out){
		boolean [] conversions = {
			this.waveband == out.waveband,
			this.wavelength == out.wavelength,
			this.timeSlot == out.timeSlot
		};
		
		return conversions;
	}
	
	/* DemuxPortID :  LLLFFFBBBWWW
	 * 
	 * Waveband*1000
	 * Wavelength
	 */
	
	public long getFiberPortId(){
		return (fiber+1)*1000000;
	}
	
	public long getWavebandPortId(){
		return getFiberPortId() + (waveband+1)*1000;
	}
	
	public long getWavelengthPortId(){
		return getWavebandPortId() + wavelength+1;
	}
	
	public LightLink clone(Link link){
		LightLink clone = new LightLink();
		
		clone.id = this.id;
		clone.fiber = this.fiber;
		clone.waveband = this.waveband;
		clone.wavelength = this.wavelength;
		clone.timeSlot = this.timeSlot;
		clone.link = link;
		clone.lightPathID = -1;
		
		return clone;
	}
	
	public boolean isConcurrentWith(LightLink a){
		if ( this.link == a.link ){
			return true;
		}
		
		return false;
	}
} 
