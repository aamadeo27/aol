package me.aamadeo.aol.optimization;

import me.aamadeo.aol.ag.BinaryTournament;
import me.aamadeo.aol.ag.GeneticAlgorithm;
import me.aamadeo.aol.ag.Individual;
import me.aamadeo.aol.on.Link;
import me.aamadeo.aol.on.Network;
import me.aamadeo.aol.on.Node;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

/**
 * Clase de prueba de persistencia.
 * @author albert
 *
 */
public class Main {
	private static EntityManagerFactory emf = Persistence.createEntityManagerFactory("main");
	private static EntityManager em = emf.createEntityManager();
	
	public static void main(String args []){
		if(args.length > 0){
			if(args[0].equalsIgnoreCase("genNetworks")) genNetworks();

			if(args[0].equalsIgnoreCase("genCases")){
				TypedQuery<Network> q = em.createQuery("From Network", Network.class);
				Calendar date = Calendar.getInstance();
				String id = "" + date.get(Calendar.DAY_OF_MONTH) + (1+date.get(Calendar.MONTH)) + date.get(Calendar.YEAR);
				
				for(Network Network: q.getResultList()){
					System.out.println("Generando cases de "+Network.getName());
					genScenario(Network,Network.getName());
				}
			}

			if(args[0].equalsIgnoreCase("optimize")){
				String sQuery = "From senario where name = :name";
				TypedQuery<Scenario> tQuery = em.createQuery(sQuery, Scenario.class);
				tQuery.setParameter("name", args[1]);
				
				Scenario scenario = tQuery.getSingleResult();
				
				optimize(scenario);
			}
		}
	}
	
	public static String SHAsum(byte[] convertme){
	    MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
			return byteArray2Hex(md.digest(convertme));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} 
		
	    return null;
	}

	private static String byteArray2Hex(final byte[] hash) {
	    Formatter formatter = new Formatter();
	    for (byte b : hash) {
	        formatter.format("%02x", b);
	    }
	    String result = formatter.toString();
	    formatter.close();
	    
	    return result;
	}
	
	private static void optimize(Scenario aScenario) {
		GeneticAlgorithm engine = new GeneticAlgorithm();
		Set<Individual> population = new HashSet<Individual>();
		String runID = "" + System.currentTimeMillis();
		runID = SHAsum(runID.getBytes()).substring(0,5);
		
		System.out.println("Se crea la poblacion inicial");
		for(int i = 0; i < 100; i++){
			Solution Individual = new Solution();
			Individual.setScenario(aScenario);
			Individual.random();
			population.add(Individual);
		} 
		
		System.out.println("El Top5 Inicial");
		int j = 1;
		for(Individual i : population){
			Solution s = (Solution) i;
			
			if(j++ > population.size() - 5) {
				System.out.println(s + ".fitness() = " + s.getFitness());
			}
		}
		
		System.out.println("Se inicia la optimizacion");
		BinaryTournament selector = new BinaryTournament();
		selector.setMaxIndividuals(100);
		population = new TreeSet<Individual>(engine.optimize(100, population, selector));
		
		System.out.println("-----\n==========================\n-----");
		
		System.out.println("El Top5("+population.size()+") Final");
		j = 1;
		
		
		for(Individual i : population){
			Solution s = (Solution) i;
			
			if(j > 95) {
				System.out.println(s + ".fitness() = " + s.getFitness());
				s.setNombre(runID + ".top" + (101-j) + "_" + Math.floor(s.getFitness()*10)/10);
				s.genGraphs();
			}
			j++;
		}
	}


	public static void genNetworks(){
		
		int [] [] nsf_links = {
				{1,2,1346},{1,3,2024},{1,8,3414},{2,3,770},{2,4,1232},{3,6,2373},
				{4,5,848},{4,10,2616},{5,6,1661},{5,7,805},{6,13,2285},{6,14,1279},
				{7,8,878},{8,9,777},{9,11,511},{9,12,542},{9,14,1104},{10,11,734},
				{10,2,991},{11,13,565},{12,13,295}
		};
		persistNet(14,nsf_links, "NSF");
		
		/*chinaNet
		int [] [] chinaNet_enlaces = {

		{1,2,1},{2,3,1},{2,8,1},{3,4,1},{4,5,1},{5,6,1},{5,7,1},{7,8,1},{8,9,1},{8,12,1},{8,13,1},
		{9,10,1},{9,11,1},{10,11,1},{10,23,1},{10,24,1},{11,23,1},{11,12,1},{12,23,1},{12,22,1},{12,27,1},{12,21,1},
		{12,20,1},{12,13,1},{13,20,1},{13,14,1},{14,15,1},{14,16,1},{14,17,1},{15,20,1},{15,29,1},{15,19,1},{15,16,1},
		{16,19,1},{17,18,1},{18,19,1},{18,31,1},{19,30,1},{19,35,1},{19,31,1},{20,28,1},{21,22,1},{21,28,1},{22,23,1},
		{22,26,1},{23,24,1},{23,25,1},{24,25,1},{24,41,1},{25,41,1},{25,54,1},{25,26,1},{26,39,1},{26,27,1},{27,58,1},
		{27,38,1},{28,38,1},{28,29,1},{29,37,1},{29,30,1},{30,36,1},{31,32,1},{31,33,1},{32,33,1},{33,35,1},{34,35,1},
		{34,61,1},{35,36,1},{35,62,1},{35,61,1},{36,37,1},{37,38,1},{38,58,1},{39,40,1},{39,56,1},{39,57,1},{39,59,1},
		{40,56,1},{40,41,1},{41,42,1},{41,54,1},{42,52,1},{42,43,1},{43,50,1},{43,44,1},{44,49,1},{44,45,1},{45,46,1},
		{46,47,1},{46,49,1},{47,48,1},{48,49,1},{48,51,1},{49,50,1},{50,51,1},{50,53,1},{50,52,1},{51,53,1},{52,54,1},
		{53,55,1},{54,56,1},{55,56,1},{56,57,1},{57,58,1},{58,65,1},{58,66,1},{59,65,1},{59,60,1},{60,64,1},{60,62,1},
		{60,61,1},{61,63,1},{62,64,1},{62,63,1},{63,64,1},{64,68,1},{64,67,1},{65,66,1},{66,68,1},{66,67,1},{67,68,1}

		};
		persistNet(68,chinaNet_enlaces, "chinaNet");
		
		/*eufrance
		int [] [] eufrance_enlaces = {

		{1,2,1},{1,4,1},{2,5,1},{3,4,1},{3,9,1},{4,5,1},{4,7,1},{5,6,1},{5,7,1},{6,7,1},{6,16,1},
		{7,18,1},{7,8,1},{8,15,1},{8,14,1},{8,9,1},{9,13,1},{9,10,1},{10,12,1},{10,11,1},{11,12,1},{12,13,1},
		{12,26,1},{13,14,1},{13,25,1},{14,15,1},{14,25,1},{15,16,1},{15,18,1},{15,24,1},{16,17,1},{16,18,1},{17,19,1},
		{18,22,1},{18,23,1},{18,24,1},{18,35,1},{19,20,1},{19,22,1},{20,21,1},{20,22,1},{21,23,1},{23,41,1},{24,38,1},
		{25,26,1},{25,34,1},{26,27,1},{26,28,1},{27,32,1},{28,32,1},{28,29,1},{29,30,1},{30,31,1},{32,33,1},{33,36,1},
		{33,35,1},{34,35,1},{35,36,1},{35,37,1},{35,38,1},{37,38,1},{37,39,1},{38,39,1},{39,40,1},{40,41,1},{40,43,1},
		{41,42,1},{41,43,1},{42,43,1},
		
		};
		persistNet(43,eufrance_enlaces, "eufrance");
		
		/*eugerman
		int [] [] eugerman_enlaces = {

		{1,4,1},{1,6,1},{2,4,1},{2,3,1},{3,5,1},{4,5,1},{5,9,1},{6,8,1},{6,7,1},{7,8,1},{7,13,1},
		{7,12,1},{7,9,1},{8,13,1},{9,12,1},{9,14,1},{9,10,1},{10,11,1},{11,15,1},{12,13,1},{12,14,1},{14,15,1},
		{14,17,1},{15,16,1},{16,17,1},
		};
		persistNet(17,eugerman_enlaces, "eugerman");
		*/
	}
	
	public static void genScenario(Network Network, String nombre){
		em.getTransaction().begin();
		
		int lightlinks = (int) Math.ceil(500.0 / Network.getBandWidth());
		Scenario aScenario = new Scenario(Network);
		aScenario.allVsAll(lightlinks);
		aScenario.setName(nombre + "C1");
		em.persist(aScenario);
		
		lightlinks = (int) Math.ceil(1000.0 / Network.getBandWidth());
		aScenario = new Scenario(Network);
		aScenario.allVsAll(lightlinks);
		aScenario.setName(nombre + "C2");
		em.persist(aScenario);
		
		lightlinks = (int) Math.ceil(5000.0 / Network.getBandWidth());
		aScenario = new Scenario(Network);
		aScenario.allVsAll(lightlinks);
		aScenario.setName(nombre + "C3");
		em.persist(aScenario);
		
		em.getTransaction().commit();
	}
	
	public static void persistNet(int nodes, int [] [] links, String name){
		persistNet(nodes,links,name+"_G1",1,3,10,10);
		persistNet(nodes,links,name+"_G2",1,3,5,5);
		persistNet(nodes,links,name+"_G3",1,3,3,3);
	}
	
	public static void persistNet(int nodes, int [] [] links, String name, int F, int B, int W, int T){
		
		HashMap<String,Node> nodoMap = new HashMap<String,Node>();
		Network net = new Network();
		net.setName(name);
		net.setF(1);
		net.setB(4);
		net.setW(10);
		net.setT(10);
		
		em.getTransaction().begin();
		for(int i = 1; i <= nodes; i++){
			Node nodo = new Node();
			nodo.setLabel(""+i);
			nodoMap.put(""+i, nodo);
			net.addNode(nodo);
		}
		em.persist(net);
		em.getTransaction().commit();
		
		em.getTransaction().begin();
		for(int i = 0; i < links.length; i++){
			Node a = nodoMap.get(""+links[i][0]);
			Node b = nodoMap.get(""+links[i][1]);
			
			Link c = net.addLink(a,b,links[i][2]);
			a.addLink(c);
			
			c = net.addLink(b,a,links[i][2]);
			b.addLink(c);
		}
		em.persist(net);
		em.getTransaction().commit();
	}
}
