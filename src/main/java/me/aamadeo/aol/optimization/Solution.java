package me.aamadeo.aol.optimization;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import me.aamadeo.aol.ag.Individual;
import me.aamadeo.aol.on.*;

@Entity
public class Solution implements Individual, Comparable<Individual> {
	//public static final String BASEDIR="C:/Users/amadeoa/Tesis/graph";
	public static final String BASEDIR="C:/Users/albert/Tesis/graph";
	
	//public static String BUFFER_DEBUG = "";
	private static int solucionSeq = 1;
		
	@Id
	@GeneratedValue
	private long id;
	
	@Transient
	private long seq;
	
	private String nombre;
	
	private double costo = Double.MIN_VALUE;
	
	@Transient
	private Map<Request,Service> serviciosPorRequest = new HashMap<Request,Service>();
	
	@OneToMany(cascade=CascadeType.ALL)
	private Set<Service> services = null;
	
	@ManyToOne(cascade=CascadeType.ALL)
	private Scenario scenario = null;
	
	private int requestNotServed = 0;
	
	public Solution(){
		this.seq = Solution.solucionSeq++;
		this.services = new TreeSet<Service>();
	}
	
	public Individual crossover(Individual i) {
		if(! (i instanceof Solution) ) return null;
		Solution padreB = (Solution) i;
		
		Solution hijo = new Solution();
		hijo.setScenario(this.scenario);
		scenario.getNetwork().initialize();
		
		for(Request Request: scenario.getRequests()){
			
			Service servA = this.getServicio(Request);
			Service servB = padreB.getServicio(Request);

			Service servHijo = servA.crossover(servB);
			hijo.addServicio(Request,servHijo);
		}
		 
		hijo.calcular();
		
		return hijo;
	}
		
	private void calcular(){
		double cost = 0.0;
		
		HashSet<Node> NodesUtilizados = new HashSet<Node>();
		HashSet<Link> linksUsed = new HashSet<Link>();

		requestNotServed = 0;
		
		for(Service service : services){
			Node anterior = service.getRequest().getOrigin();
			Node actual = anterior;
			NodesUtilizados.add(anterior);
			
			if (service.getPath() == null){
				requestNotServed++;
				continue;
			}
			
			for(Hop s: service.getPath().getHops()){
				anterior = actual;
				actual = s.getDestination();
				
				if ( ! NodesUtilizados.contains(actual) ){
					NodesUtilizados.add(actual);
				}
				
				if ( ! linksUsed.contains(s.getLink()) ){
					linksUsed.add(s.getLink());
				}
				
				cost += s.getLink().getCost() * Network.COST_KM;
			}
		}
		
		for (Node Node: NodesUtilizados){
			cost += Node.getCost();
		}
				
		/*
		 * El fitness se representa como una constante positiva (1000) menos
		 * el costo y menos la penalizacion.
		 */
		
		costo = cost;		
	}

	public void mutate() {
		for(Service s: services){
			if ( Math.random() < 0.21) s.mutate();
		}
		
		calcular();
	}

	public void random() {
		scenario.getNetwork().initialize();
		serviciosPorRequest.clear();
		services.clear();
		
		for(Request Request: scenario.getRequests()){
			Service service = new Service(Request);
		
			initExceptionManagement();
			try { service.random(); }
			catch(Exception e){
				manageException(e);
			}
						
			addServicio(Request, service);
			
			//if ( DEBUG ) scenario.getNetwork().usage(BASEDIR,"");
		}
		
		calcular();
	}

	public long getId() {
		return seq;
	}

	public void setId(long seq) {
		this.seq = seq;
	}

	public Service getServicio(Request Request) {
		return serviciosPorRequest.get(Request);
	}

	public Set<Service> getServices() {
		return services;
	}
	
	public void addServicio(Request Request, Service service){
		if(serviciosPorRequest.containsKey(Request)) return;
		serviciosPorRequest.put(Request, service);
		services.add(service);
	}

	public void setServices(Set<Service> services) {
		this.services = services;
		
		for(Service service : services){
			serviciosPorRequest.put(service.getRequest(), service);
		}
	}

	public Scenario getScenario() {
		return scenario;
	}

	public void setScenario(Scenario aScenario) {
		this.scenario = aScenario;
		this.nombre = aScenario.getName() + "_sol" + this.seq;
	}

	public void genGraphs() {
		String dir =  BASEDIR + "/" + this.nombre;

		File dirFile = new File(dir);
		dirFile.mkdir();
		
		for(Service s : services){
			scenario.getNetwork().drawService(s, dir,null);
		}
		
		scenario.getNetwork().usage(dir,"");
	}
	
	public void save(String dir) {
		for(Service s : services){
			s.save(dir);
		}
	}
	
	@Override
	public String toString() {
		
		return nombre+":"+costo+":"+requestNotServed;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getNombre() {
		return nombre;
	}
	
	public int compareTo(Solution s){
		return (int) (this.seq - s.seq);
	}

	public int compareTo(Individual i) {
		Solution b = (Solution) i;
		
		if (b.requestNotServed != 0 || this.requestNotServed != 0)
			return b.requestNotServed - this.requestNotServed;
		
		return (int) (10000*(b.costo - this.costo));
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Solution)) return false;
		return equals((Solution)obj);
	}
	
	public boolean equals(Solution b){
		return this.seq == b.seq;		
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		Solution clone = new Solution();
		clone.setScenario(scenario);
		
		for(Request Request : scenario.getRequests()){
			Service originalService = serviciosPorRequest.get(Request);
			
			Service clonService = new Service(Request);
			clonService.setPath(originalService.getPath());
			
			clone.addServicio(Request, clonService);
		}
		
		return clone;
	}
	
	private void initExceptionManagement(){
		//Service.BUFFER_DEBUG = "";
//		Hop.BUFFER_DEBUG = "";
		//Node.BUFFER_DEBUG = "";
		//Path.BUFFER_DEBUG = "";
		//Link.BUFFER_DEBUG = "";
	}
	
//*	
	private void manageException(Exception e){
		System.err.println("En el hilo : " + Thread.currentThread().getName());
//		System.err.println("--BUFFER SERVICIO--");
//		System.err.println(Service.BUFFER_DEBUG);

//		if ( Node.BUFFER_DEBUG.length() > 0){
//			System.err.println("--BUFFER CAMINO--");
//			System.err.println(Path.BUFFER_DEBUG);
//		}
		
//		if ( Hop.BUFFER_DEBUG.length() > 0){
//			System.err.println("--BUFFER SALTO--");
//			System.err.println(Hop.BUFFER_DEBUG);
//		}
		
//		if ( Node.BUFFER_DEBUG.length() > 0){
//			System.err.println("--BUFFER Node--");
//			System.err.println(Node.BUFFER_DEBUG);
//		}
		
//		if ( Link.BUFFER_DEBUG.length() > 0){
//			System.err.println("--BUFFER CANAL OPTICO--");
//			System.err.println(Link.BUFFER_DEBUG);
//		}
		
		e.printStackTrace();
		System.exit(1);
	}
	//*/
		
	public double getFitness() {
		return -costo;
	}

	public void setCosto(double costo) {
		this.costo = costo;
	}
	
	public double getCosto(){
		return this.costo;
	}
}
