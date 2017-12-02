package me.aamadeo.aol.ag;

public interface Individual {
	public Individual crossover(Individual i);
	
	public int compareTo(Individual i);
	
	public double getFitness();
	
	public void mutate();
	
	public void random();

}
