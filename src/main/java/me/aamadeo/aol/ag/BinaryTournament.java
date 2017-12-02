package me.aamadeo.aol.ag;

import java.util.LinkedList;

public class BinaryTournament extends Selector {

	public Individual fittest = null;
	
	private int maxIndividuals;


	
	@Override
	public LinkedList<Individual> select(LinkedList<Individual> population) {
		LinkedList<Individual> newPopulation = new LinkedList<Individual>();

		int delta = 0;
		if ( fittest != null ){
			newPopulation.add(fittest);
			Individual fittestChild = fittest.crossover(fittest);
			population.add(fittestChild);
			delta = 1;
		}
		
		int individuals = population.size();
		Individual [] vPopulation = population.toArray(new Individual[individuals]);
		int s = 0;
		
		while(s < maxIndividuals - delta){
			s++;
			int i = (int) (Math.random()*((double)individuals));
			Individual individualA = vPopulation[i];
			
			i = (int) (Math.random()*((double)individuals));
			Individual individualB = vPopulation[i];

			if(individualA.compareTo(individualB) > 0){
				newPopulation.add(individualA);
				measure(individualA);
			} else {
				newPopulation.add(individualB);
				measure(individualB);
			}
		}
		
		System.out.println("["+ fittest +"]");
		
		return newPopulation;
	}

	
	private void measure(Individual i){
		if (fittest == null ){
			fittest = i;
		} else {
			int diferencia = i.compareTo(fittest);
			
			if (diferencia > 0) {
				fittest = i;
			}
		}
	}

	public void setMaxIndividuals(int maxIndividuals) {
		this.maxIndividuals = maxIndividuals;
	}
}
