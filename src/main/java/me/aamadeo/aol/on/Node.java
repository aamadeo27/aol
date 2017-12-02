package me.aamadeo.aol.on;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name="Node")

public class Node implements Comparable<Node>{

	private static final int LEVEL_WAVEBAND = 0;
	private static final int LEVEL_WAVELENGTH = 1;
	private static final int LEVEL_TIMESLOT = 2;
	
	@Id
	@GeneratedValue
	private long id;
	
	private String label = "";
	
	@ManyToMany(cascade=CascadeType.ALL)
	private Set<Link> links = new HashSet<Link>();
	
	@Transient
	private boolean locked = false;
	
	@Transient
	private HashMap<Long, LightLink> output = new HashMap<Long, LightLink>();
	
	@Transient
	private HashSet<LightLink> input = new HashSet<LightLink>();
	
	@Transient
	private HashMap<Link,Long> linksId = new HashMap<Link, Long>();

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
	public boolean isLocked(){
		return this.locked;
	}
	
	public void lock(){
		this.locked = true;
	}
	
	public void unlock(){
		this.locked = false;
	}

	public void initialize() {
		this.locked = false;
		input.clear();
		output.clear();
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof Node)) return false;
		Node b = (Node) obj;
		
		return this.label.equalsIgnoreCase(b.label);
	}
	
	public void addLink(Link link){
		if(! links.contains(link)){
			links.add(link);
		}
	}
	
	public Link breakLinkTo(Node neighbor){
		for ( Link c: links){
			if (neighbor.equals(c.getDestination())){
				links.remove(c);
				return c;
			}
		}
		
		return null;
	}

	public void breakAllLinks(Network network){
		for (Link link : links){
			Node vecino = link.getDestination();

			Link reverseLink = vecino.breakLinkTo(this);

			/* Se elimina ambos enlaces de la network */
			network.removeLink(link);
			network.removeLink(reverseLink);
		}

		links.clear();
	}

	public String toString(){
		return this.label;
	}

	public Set<Link> getLinks() {
		return links;
	}
	
	public void addLinkID(Link link){
		if (linksId.get(link) != null) return;

		long fid = 1 + linksId.size();
		linksId.put(link, new Long(fid));
	}

	public void setLinks(Set<Link> links) {
		this.links = links;
	}
	
	private class NodeDijkstra implements Comparable<NodeDijkstra> {
		private final Node node;
		private Path path;
		private int distance = Integer.MAX_VALUE;
		
		public NodeDijkstra(Node node, int distance){
			this.node = node;
			this.distance = distance;
		}
		
		@Override
		public boolean equals(Object o){
			if(o instanceof Node){
				return ((Node)o).label.equalsIgnoreCase(node.label);
			}
			
			if (o instanceof NodeDijkstra){
				return ((NodeDijkstra)o).node.label.equalsIgnoreCase(node.label);
			}
			
			return false;
		}

		public int compareTo(NodeDijkstra arg0) {
			NodeDijkstra b = (NodeDijkstra) arg0;
			
			return this.distance - b.distance;
		}
		
		public int getDistance(){
			return this.distance;
		}

		public Node getNode() {
			return node;
		}

		public Path getPath() {
			return path;
		}

		public void setPath(Path path) {
			this.path = path;
		}
	}
	
	public Path dijkstra(Node destination, int bandwidth) {
		if ( Configuration.getIncreaseDimension() == -1 ) {
			return dijkstra(destination,bandwidth,true);
		}
		
		return dijkstra(destination,bandwidth,false);
	}
	
	/**
	 * Shortest path to destination using Dijkstra's algorithm.
	 * 
	 * @param destination	Node destination
	 * @param bandwidth 	Bandwidth required by the service
	 * @param strict 		If stritct is true, the resources of the link are fixed, and can't
	 *                      be added extra resources.
	 * @return				Shortestpath to destination node.
	 */
	public Path dijkstra(
			Node destination,
			int bandwidth,
			boolean strict ){
		
		PriorityQueue<NodeDijkstra> toVisit = new PriorityQueue<NodeDijkstra>();
		HashMap<Node,Integer> distances = new HashMap<Node,Integer>();
		HashSet<Node> visited = new HashSet<Node>();
		
		NodeDijkstra originNode = new NodeDijkstra(this,0);
		originNode.setPath(new Path(this));
		toVisit.add(originNode);
		distances.put(this, new Integer(0));
		
		while(! toVisit.isEmpty()){
			NodeDijkstra dNode = toVisit.poll();
			Node currentNode = dNode.getNode();
			Path path = dNode.getPath();
			
			if(currentNode.equals(destination)) {
				return path;
			}

			if (visited.contains(currentNode)) continue;
			
			visited.add(currentNode);
			
			for(Link link : currentNode.links){
				Node neighbor = link.getDestination();
				int cost = link.getCost();

				if (visited.contains(neighbor)) continue;
				if ( strict && ! link.itsCapable(bandwidth) ) continue;
				if (link.isLocked()) continue;
				if (neighbor.isLocked()) continue;

				if(distances.containsKey(neighbor)){
					int currentDistance = distances.get(neighbor);
					
					if( currentDistance <= dNode.getDistance() + cost) {
						continue;
					} else {
						toVisit.remove(neighbor);
						distances.remove(neighbor);
					}
				}
				
				NodeDijkstra newNode = new NodeDijkstra(neighbor, dNode.getDistance()+cost);
				Path newPath = new Path(path);
				newPath.addHop(new Hop(path.getHops().size()+1, link));
				newNode.setPath(newPath);
				toVisit.add(newNode);
				distances.put(newNode.getNode(), newNode.distance);
			}
		}
		
		return null;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	@Override
	public int hashCode() {
		int labelInt = Integer.parseInt(label);
		
		if (labelInt > 0) return labelInt;
		
		return super.hashCode();
	}
	
	public int compareTo(Node b){
		return Integer.parseInt(label) - Integer.parseInt(b.label);
	}
	
	public void receive(LightLink lightlink){
		input.add(lightlink);
	}
	
	public void transmit(LightLink lightlink){
		output.put(lightlink.getLightPathID(), lightlink);
	}
	
	public double getCost(){
		
		double cost = 0.0;
		int portsCount = 0;
		int muxdemuxCount = 0;
		
		HashSet<Long> portsIn = new HashSet<Long>();
		HashSet<Long> portsOut = new HashSet<Long>();
		HashSet<Long> lightsIn = new HashSet<Long>();
		
		for( LightLink lightIn : input){
			LightLink lightOut = output.get(lightIn.getLightPathID());
			
			Link linkIn = lightIn.getLink();
			
			long inLinkId = linksId.get(linkIn) * 1000000000;
			
			Link linkOut = null;
			long outLinkId = -1;
			
			boolean [] conversions = null;
			int demuxDepth = 0;
			
			lightsIn.add(lightIn.getLightPathID());
			
			if ( lightOut == null ) {
				conversions = new boolean [3];
				demuxDepth = 3;
			} else {
				linkOut = lightOut.getLink();
				outLinkId = linksId.get(linkOut) * 1000000000;
				conversions = lightIn.convert(lightOut);
				demuxDepth = conversions[2] ? 3 : (
						conversions[1] ? 2 : (
							conversions[0] ? 1 : 0));
			}
			
			for(int level = 0; level < demuxDepth; level++){
				switch(level){
					case LEVEL_WAVEBAND :
						/*FiberPortId LLLFFF Demultiplexacion en Wavebands*/
						long inLightlinkFId = inLinkId + lightIn.getFiberPortId();
						long outLightlinkFId = lightOut == null ? -1 : outLinkId + lightOut.getFiberPortId(); 
						
						if ( ! portsIn.contains(inLightlinkFId) ){
							muxdemuxCount++;
							portsCount += linkIn.getB();
							portsIn.add(inLightlinkFId);
						}
						
						if ( lightOut != null && ! portsOut.contains(outLightlinkFId) ){
							muxdemuxCount++;
							portsCount += linkOut.getB();
							portsOut.add(outLightlinkFId);
						}
						
						if ( conversions[level] ) {
							portsCount+=2;
							cost += Network.COST_WLCONVERSION;
						}
						
						break;
					case LEVEL_WAVELENGTH :
						/*WaveBandPortId LLLFFFBBB Demultiplexacion en Wavelengths*/
						long inLightlinkWBPId = inLinkId + lightIn.getWavebandPortId(); 
						long outLightlinkWBPId = lightOut == null ? -1 : outLinkId + lightOut.getWavebandPortId();
						
						if ( ! portsIn.contains(inLightlinkWBPId) ){
							muxdemuxCount++;
							portsCount += linkIn.getW();
							portsIn.add(inLightlinkWBPId);
						}
						
						if ( lightOut != null && ! portsOut.contains(outLightlinkWBPId) ){
							muxdemuxCount++;
							portsCount += linkOut.getW();
							portsOut.add(outLightlinkWBPId);
						}
						
						if ( conversions[level] ) {
							portsCount += 2;
							cost += Network.COST_WLCONVERSION;
						}
						
						break;
					case LEVEL_TIMESLOT :
						/*WaveLengthPortId Demultiplexacion en Timeslots y Conversi�n*/
						long inLightlinkWLPId = inLinkId + lightIn.getWavelengthPortId();
						long outLightlinkWLPId = lightOut == null ? -1 : inLinkId + lightOut.getWavelengthPortId();
						
						if ( ! portsIn.contains(inLightlinkWLPId) ){
							portsCount++;
							portsIn.add(inLightlinkWLPId);
							
							cost += Network.COST_TSI;
						}
						
						if ( lightOut != null && ! portsOut.contains(outLightlinkWLPId) ){
							portsCount++;
							portsOut.add(outLightlinkWLPId);
						}
					
						break;
				}
			}	
		}
		
		for (LightLink lightOut : output.values()){
			if ( lightsIn.contains(lightOut.getLightPathID())) continue;
			
			long linkId = linksId.get(lightOut.getLink());
					
			/*FiberPortId LLLFFF Multiplexacion en Wavebands*/
			long lightlinkFId = linkId + lightOut.getFiberPortId(); 
			
			if ( ! portsOut.contains(lightlinkFId) ){
				muxdemuxCount++;
				portsCount += lightOut.getLink().getB();
				portsOut.add(lightlinkFId);
			}
			
			/*WaveBandPortId LLLFFFBBB Multiplexacion en Wavelengths*/
			long lightlinkWBPId = linkId + lightOut.getWavebandPortId(); 
			
			if ( ! portsOut.contains(lightlinkWBPId) ){
				muxdemuxCount++;
				portsCount += lightOut.getLink().getW();
				portsOut.add(lightlinkWBPId);
			}
			
			/*WaveLengthPortId Demultiplexacion en Timeslots y Conversi�n*/
			long lightlinkWLPId = linkId + lightOut.getWavelengthPortId(); 
			
			if ( ! portsOut.contains(lightlinkWLPId) ){
				portsCount++;
				cost += Network.COST_TSI;
				portsOut.add(lightlinkWLPId);
			}		
		}
		
		return cost + portsCount* Network.COST_OSW_PORT + muxdemuxCount* Network.COST_MUXDEMUX_PORT;
	}
	
	public Node clone(){
		Node clone = new Node();
		
		clone.id = this.id;
		clone.label = this.label;
		clone.links = new HashSet<Link>();
		
		return clone;
	}
	
	public boolean isConcurrentWith(Node b){
		if ( this == b ) {
			return true;
		}
		
		Iterator<Link> resourcesA = (new TreeSet<Link>(links)).iterator();
		Iterator<Link> resourcesB = (new TreeSet<Link>(b.links)).iterator();
		
		while ( resourcesA.hasNext() && resourcesB.hasNext() ){
			Link resourceA = resourcesA.next();
			Link resourceB = resourcesB.next();
			
			if ( resourceA == resourceB ) {
				return true;
			}
			if ( resourceA.isConcurrentWith(resourceB) ) {
				return true;
			}
		}
		
		return false;
	}
}
