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

public class Benchmark {
	private static Runtime runtime = Runtime.getRuntime();
	private static EntityManagerFactory emf = Persistence.createEntityManagerFactory("tesis");
	private static EntityManager em = emf.createEntityManager();
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		/*
		 * Para cada configuracion
		 * 	Para cada carga de la Network
		 * 		Correr 100000 veces
		 */
		
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
		
		
		for ( int loadLevel = 1; loadLevel <= 3; loadLevel++) {
			for ( int granularity = 1; granularity <= 3 ; granularity++){

				long actual = System.currentTimeMillis();
				Set<Individual> population = new HashSet<Individual>();
				String scenarioName = "NSF_G"+granularity+"C"+loadLevel;
				
				File scenarioDir = new File(strategyDir.getAbsolutePath() + "/" + scenarioName);
				if ( ! scenarioDir.exists()) scenarioDir.mkdir();
				
				String sQuery = "From Scenario where name = :name";
				TypedQuery<Scenario> tQuery = em.createQuery(sQuery, Scenario.class);
				tQuery.setParameter("name",scenarioName);
				Scenario aScenario = tQuery.getSingleResult();
				System.out.println("\n[" + (actual-inicio)/1000 +"] Estrategia: " + strategy + " aScenario: " + aScenario.getName()  );
	
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
				
				/*
				em.getTransaction().begin();
				em.persist(fittest);
				em.getTransaction().commit();
				*/
				
				fittest.save(scenarioDir.getAbsolutePath());

				System.out.println("Fin del aScenario : " + scenarioName);
			}
		}
	}

}
