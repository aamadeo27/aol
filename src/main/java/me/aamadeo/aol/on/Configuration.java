package me.aamadeo.aol.on;

public class Configuration {
	
	public static final int FIBER_INCREASE = 0;
	public static final int BAND_INCREASE = 1;
	public static final int WAVELENGTH_INCREASE = 2;
	
	private static boolean [] flexibility = {false, false, false};
	
	public static void setflexibility(int increaseDimension, boolean isFlexible) {
		flexibility[increaseDimension] = isFlexible;
	}
	
	public static int getIncreaseDimension(){
		boolean isStatic = true;
		
		for ( int f = 0; f < 3; f++) isStatic = isStatic && !flexibility[f];

		if ( isStatic ) return -1;
		
		int increaseDimension = 0;
		
		do {
			increaseDimension = (int) (Math.random()*3.0);
		} while( ! flexibility[increaseDimension]  );
		
		return increaseDimension;
	}
	
}
