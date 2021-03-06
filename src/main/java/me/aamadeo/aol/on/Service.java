package me.aamadeo.aol.on;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

@Entity
public class Service implements Comparable<Service>{
	
	@Id
	@GeneratedValue
	private long id;
	
	@ManyToOne(cascade=CascadeType.ALL)
	private Request request;
	
	@OneToOne(cascade=CascadeType.ALL)
	private Path path;
	
	@Transient
	private boolean available = false;
	
	public Service(){}

	public Service(Request request) {
		super();
		this.request = request;
	}

	public Request getRequest() {
		return request;
	}

	public boolean isAvailable() {
		return available;
	}

	/**
	 * Setter de la disponibilidad del servicio
	 * @param available
	 */
	public void setAvailable(boolean available) {
		this.available = available;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setRequest(Request request) {
		this.request = request;
	}

	public Path getPath() {
		return path;
	}

	public void setPath(Path path) {
		this.path = path;
	}

	/**
	 * Commits the resources of the network to the path
	 */
	public void commitPath(){
		if (path == null) return;

		//Add resources if needed
		path.addResources(request.getBadnwidth(), request.getNetwork());

		path.comitLightLinks(request.getBadnwidth(), id * request.getNetwork().getBandWidth());
	}
	
	public void commitLightLinks(){
		if (path == null) return;
		
		path.commitLinks(request.getNetwork());
	}
	
	public void freeLightLinks(){
		if(path == null) return;
		
		path.unlockLightLinks();
	}
	
	public void mutate(){
		if(path == null) return;
		
		path.unlockLightLinks();
		
		if ( Math.random() < 0.5){
			Path mutant = mutateSegment(path);
		
			if ( mutant != null ) path = mutant;
		}

		commitPath();
	}
		
	public Path mutateSegment(Path originalPath){
		if ( originalPath == null ) return null;
				
		/* 
		 * The method selects a random segment of the path of a random distance between [2;5] hops and mutates that
		 * segment.
		 *
		 * So the original is divided in 3 sections, SectionA, SectionB (to be mutated) and SectionC.
		 *
		 * Origin - PointM1 -  PointM2 - Destination
		 * |  SectionA | Section B |    Section C  |
		 * The mutated segment (new SectionB) is found by locking the nodes on the original (old SectionB),
		 * thus looking for an alternative point between PointM1 and PointM2
		 */
		double hopCount = originalPath.getHops().size();
		int subPathDistance = Math.max(2,(int)(Math.random()*5));
		
		if ( subPathDistance > hopCount ) return null;
		
		int nodeIndex = (int) (Math.random()*(hopCount-subPathDistance));
		
		Iterator<Hop> hopIterator = originalPath.getHops().iterator();

		Path mutantPath = new Path(originalPath.getOrigin());
		Node current = originalPath.getOrigin();
		current.lock();
		
		while(nodeIndex > 0) {
			Hop hop = hopIterator.next();
			mutantPath.addHop(new Hop(hop.getSequence(), hop.getLink()));
			current = hop.getDestination();
			current.lock();
			nodeIndex--;
		}
		
		Node pointM1 = current;
		Path oldSectionB = new Path(pointM1);

		int secuencia = 1;
		while(subPathDistance > 0){
			Link link = hopIterator.next().getLink();
			oldSectionB.addHop(new Hop(secuencia++, link));
			
			subPathDistance--;
			link.lock();
			current = link.getDestination();
		}
		Node pointM2 = current;
		pointM2.unlock();
				
		Path sectionC = new Path(current);
		secuencia = 1;
		while(hopIterator.hasNext()){
			Link canal = hopIterator.next().getLink();
			sectionC.addHop(new Hop(secuencia++, canal));
			current = canal.getDestination();
			current.lock();
			
		}
		sectionC.getOrigin().unlock();
		
		/*Se calcula la parte B del camino nuevo*/
		Path newSectionB = pointM1.dijkstra(pointM2, request.getBadnwidth());
		newSectionB.unlockUsedLinks();
		originalPath.unlockNodes();

		if(newSectionB == null) return null;
		
		mutantPath.concat(newSectionB);
		mutantPath.concat(sectionC);
		
		return mutantPath;
		
	}

	private LinkedList<Path> getSegments(LinkedList<Node> connectors ){
		LinkedList<Path> segmentos = new LinkedList<Path>();
		int sequence = 1;
		
		Node current = connectors.removeFirst();
		Node next = connectors.removeFirst();
		Path segment = new Path(current);
		segmentos.add(segment);
		
		for( Hop hop : path.getHops()){
			segment.addHop(new Hop(sequence++, hop.getLink()));
			current = hop.getDestination();
			
			if (next != null && current.equals(next) ){
				if ( connectors.isEmpty() ) {
					next = null;
				} else {
					next = connectors.removeFirst();
					sequence = 1;
					segment = new Path(current);
					segmentos.add(segment);
				}
			}
		}
		
		return segmentos;
	}
	
	public void mutateSegments(LinkedList<Path> segments){
		
		path = new Path(request.getOrigin());
		
		for (Path segment: segments) segment.lockNodes();
		
		for (Path segment: segments){
			segment.unlockNodes();
			
			Path newSegment = mutateSegment(segment);
			if (newSegment != null) segment = newSegment;
			
			for(Hop h : segment.getHops()){
				if ( ! h.getLink().itsCapable(request.getBadnwidth()) ){
					path.unlockNodes();
					path = null;
					return;
				}
			}
				
			segment.lockNodes();
			path.concat(segment);
		}
		
		path.unlockNodes();
	}
		
	public Service crossover(Service b){
		Service child = new Service(this.request);
		
		Node current = null;
		HashSet<Node> baseNodes = new HashSet<Node>();
		LinkedList<Node> commonNodes = new LinkedList<Node>();
		boolean coin = Math.random() <= 0.5f;
		boolean copy = false;
		
		if ( path == null ) {
			if (b.path == null){
				child.path = null;
				return  child;
			}
			
			copy = true;
			for ( Hop h: b.path.getHops() ){
				Link link = h.getLink();
				
				if ( !link.itsCapable(request.getBadnwidth()) ) {
					child.path = null;
					return child;
				}
			}
		} else if ( b.path == null ){
			copy = true;
			for ( Hop h: path.getHops() ){
				Link link = h.getLink();
				
				if ( !link.itsCapable(request.getBadnwidth()) ) {
					child.path = null;
					return child;
				}
			}			
		}
		
		if ( ! copy ){
			current = request.getOrigin();
			child.path = new Path( coin ? this.path : b.path);
			Path recesivePath = coin ? b.path : this.path;
			
			for(Hop s: recesivePath.getHops()){
				current = s.getDestination();
				baseNodes.add(current);
			}
				
			current = child.path.getOrigin();
			commonNodes.add(current);
			for(Hop s: child.path.getHops()){
				current = s.getDestination();

				if(baseNodes.contains(current)) {
					commonNodes.add(current);
				}
			}
			
			child.mutateSegments(getSegments(commonNodes));
		}
		
		child.commitPath();
		
		return child;
	}

	public void random(){		
		Node origin = request.getOrigin();
		Node destination = request.getDestino();
		
		path = origin.dijkstra(destination, request.getBadnwidth());
		commitPath();
		
		if ( path != null) {
			mutate();
			mutate();
		}
	}
		
	@Override
	public String toString() {
		return "s" + request.getOrigin() + "_" + request.getDestino();
	}

	public int compareTo(Service arg0) {
		return request.compareTo(arg0.request);
	}
	
	@Override
	public boolean equals(Object arg0) {
		if ( ! (arg0 instanceof Service) ) return false;
		
		return toString().equalsIgnoreCase(arg0.toString());
	}
	
	@Override
	public int hashCode() {
		return request.hashCode();
	}

	/**
	 * Creates an image of a network with its allocated resources.
	 * @param dir
	 */
	public void save(String dir){
		try{
			PrintWriter pw = new PrintWriter( new FileWriter(dir + "/s"+ request) );
			
			pw.println(path.getHops().size() + ":" + request.getBadnwidth());
			
			for(Hop j : path.getHops()){
				for ( LightLink ll : j.getLightLinks() ){
					String link = j.getSource()+":"+j.getDestination();
					pw.println(link+":"+ll.getFiber()+":"+ll.getWaveband()+":"+ll.getWavelength()+":"+ll.getTimeSlot());
				}
			}
			
			pw.close();
		} catch (IOException e){
			e.printStackTrace();
			System.exit(1);
		}
	}
}
