package me.aamadeo.aol.optimization;

import me.aamadeo.aol.ag.BinaryTournament;
import me.aamadeo.aol.ag.GeneticAlgorithm;
import me.aamadeo.aol.ag.Individual;
import me.aamadeo.aol.on.Configuration;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

/**
 * Command line for executing the optimization of every scenario, bandwith requirement (load level) and capacity
 * on the NSF network.
 * Each scenario (args[0])it's based on an integer [0-8] whose binary interpretation gives: the strategy,
 * the flexibility of a network, if it can add more fibers, more wavebands, more wavelengths or timeslots.
 *
 * The resulting image will be saved in the root directory specified by args[1]
 *
 */
public class Benchmark {
	private static Runtime runtime = Runtime.getRuntime();
	private static EntityManagerFactory emf = Persistence.createEntityManagerFactory("tesis");
	private static EntityManager em = emf.createEntityManager();

	public static void main(String[] args) {
		String root = args[1];
		
		long inicio = System.currentTimeMillis();		
			
		int k = Integer.parseInt(args[0]) % 8;
			
		boolean flexibleFiber = (k / 4 >= 1);
		k = k % 4;
			
		boolean flexibleWaveband = (k / 2 >= 1);
		k = k % 2;
			
		boolean flexibleWavelength = (k == 1);
			
		String strategy = (flexibleFiber ? "F" : "_");
		strategy += (flexibleWaveband ? "B" : "_");
		strategy += (flexibleWavelength ? "W" : "_");
			
		Configuration.setflexibility(Configuration.FIBER_INCREASE, flexibleFiber);
		Configuration.setflexibility(Configuration.BAND_INCREASE, flexibleWaveband);
		Configuration.setflexibility(Configuration.WAVELENGTH_INCREASE, flexibleWavelength);
		
		File strategyDir = new File(root + "/s"+strategy);
		
		if ( ! strategyDir.exists() ) strategyDir.mkdir();

		for ( int capacity = 1; capacity <= 3; capacity++) {
			for ( int loadLevel = 1; loadLevel <= 3 ; loadLevel++){

				long actual = System.currentTimeMillis();
				Set<Individual> population = new HashSet<Individual>();
				String scenarioName = "NSF_G"+loadLevel+"C"+capacity;
				
				File scenarioDir = new File(strategyDir.getAbsolutePath() + "/" + scenarioName);
				if ( ! scenarioDir.exists()) scenarioDir.mkdir();
				
				String sQuery = "From Scenario where name = :name";
				TypedQuery<Scenario> tQuery = em.createQuery(sQuery, Scenario.class);
				tQuery.setParameter("name",scenarioName);
				Scenario aScenario = tQuery.getSingleResult();
				System.out.println("\n[" + (actual-inicio)/1000 +"] Strategy: " + strategy + " Scenario: " + aScenario.getName()  );
	
				GeneticAlgorithm engine = new GeneticAlgorithm();
				String run = "" + System.currentTimeMillis();
				run = Main.SHAsum(run.getBytes()).substring(0,5);
								
				
				for(int i = 0; i < 100; i++){
					Solution Individual = new Solution();
					Individual.setScenario(aScenario);
					Individual.random();
					population.add(Individual);
				} 
								
				BinaryTournament selector = new BinaryTournament();
				selector.setMaxIndividuals(100);
				population = new TreeSet<Individual>(engine.optimize(1000, population, selector));
				Solution fittest = (Solution) population.iterator().next();

				em.getTransaction().begin();
				em.persist(fittest);
				em.getTransaction().commit();
				
				fittest.save(scenarioDir.getAbsolutePath());

				System.out.println("End of Scenario : " + scenarioName);
			}
		}
	}

}
