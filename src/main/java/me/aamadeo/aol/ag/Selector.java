package me.aamadeo.aol.ag;

import java.util.LinkedList;

public abstract class Selector {	
	
	private int convergence;
	
	public abstract LinkedList<Individual> select(LinkedList<Individual> poblacion);
	public int getConvergence(){
		return this.convergence;
	}
}
