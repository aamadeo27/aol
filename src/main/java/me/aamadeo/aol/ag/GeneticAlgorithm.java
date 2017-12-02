package me.aamadeo.aol.ag;

import java.util.TreeSet;
import java.util.LinkedList;
import java.util.Set;

public class GeneticAlgorithm {
	
	public Set<Individual> optimize(int iterations, Set<Individual> initialPopulation, Selector selector){
		
		LinkedList<Individual> population = new LinkedList<Individual>(initialPopulation);
		
		for(int i = 0; i < iterations; i++){
			int individuals = population.size();
			
			Individual [] vPopulation = population.toArray(new Individual[individuals]);
			
			for(int j = 0; j < individuals; j++){
				int a = (int) (Math.random()*((double)individuals));
				Individual individualA = vPopulation[a];
				
				a = (int) (Math.random()*((double)individuals));
				Individual individualB = vPopulation[a];
				
				Individual individualC = individualA.crossover(individualB);
				individualC.mutate();
										
				population.add(individualC);
			}

			population = selector.select(population);
			
			if (selector.getConvergence() > population.size()*0.9 ) break;
		}
		
		return new TreeSet<Individual>(population);
	}
}
