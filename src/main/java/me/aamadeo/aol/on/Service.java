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
		
	private double failProbability = 1.0;
	
	@Transient
	private boolean available = false;
	
	public Service(){}
	
	/**
	 * Constructor principal
	 * @param request	Request a la que se desea proveerle un servicio
	 */
	public Service(Request request) {
		super();
		this.request = request;
	}

	/**
	 * Getter de la request
	 * @return	Request
	 */
	public Request getRequest() {
		return request;
	}

	/**
	 * Obtiene la probabilidad de falla del servicio
	 * @return	Probabilidad de Falla
	 */
	public double getFailProbability() {
		return ((int)(failProbability *10000.0))/100.0;
	}


	/**
	 * Funcion de Simulacion, retorna true si el servicio esta available
	 * @return	Disponibildad del servicio
	 */
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
	
	public void commitPath(){
		if (path == null) return;
		
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
		 * Se busca un subcamino de distancia entre 1 y el 40% de longitud del camino original (4 saltos en promedio).
		 * El subcamino tendra una distancia aleatoria (subcaminoDistancia).
		 * Y el nodo origin tambien sera aleatorio.
		 * 
		 * El camino nuevo tiene tres partes :
		 * Parte A : Origin - Medio1
		 * Parte B : Medio1 - Medio2 (Subcamino nuevo donde no se utilizan los nodos intermedios del camino original) 
		 * Parte C : Medio2 - Fin
		 * 
		 * El nuevo subcamino nuevo se hallar� bloqueando los nodos originales del sub camino, y buscando
		 * otro camino optimo. Luego se desbloquearan los nodos originales del sub camino.
		 */
		double hopCount = originalPath.getHops().size();
		int subPathDistance = Math.max(2,(int)(Math.random()*5));
		
		if ( subPathDistance > hopCount ) return null;
		
		int nodeIndex = (int) (Math.random()*(hopCount-subPathDistance));
		
		Iterator<Hop> hopIterator = originalPath.getHops().iterator();
		
		/*
		 * Se crea primeramente la parte A del camino nuevo
		 */
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
		
		Node middleA = current;
		Path oldSegmentB = new Path(middleA);
				
		/*
		 * Se bloquean los canales intermedios entre Medio1 y Medio2
		 */
		int secuencia = 1;
		while(subPathDistance > 0){
			Link link = hopIterator.next().getLink();
			oldSegmentB.addHop(new Hop(secuencia++, link));
			
			subPathDistance--;
			link.lock();
			current = link.getDestination();
		}
		Node middleB = current;
		middleB.unlock();
				
		Path segmentC = new Path(current);
		secuencia = 1;
		while(hopIterator.hasNext()){
			Link canal = hopIterator.next().getLink();
			segmentC.addHop(new Hop(secuencia++, canal));
			current = canal.getDestination();
			current.lock();
			
		}
		segmentC.getOrigin().unlock();
		
		/*Se calcula la parte B del camino nuevo*/
		Path newSegmentB = middleA.dijkstra(middleB, request.getBadnwidth());
		newSegmentB.unlockUsedLinks();
		originalPath.unlockNodes();
		
		/*
		 * Si no se puede encontrar un camino alternativo sin utilizar
		 * los canales originales, se ignora la mutacion.
		 */
		if(newSegmentB == null) {
			return null;
		}
		
		mutantPath.concat(newSegmentB);
		mutantPath.concat(segmentC);
		
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
	
	public void getFailProbability(double failProbabilityPerLink){
		if(path == null)  return;
		
		failProbability = failProbabilityPerLink * path.getHops().size();
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
