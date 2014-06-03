/**
 * Helper functions for the density estimation algorithm.
 * 
 * @author Daniel Weinand & Gedeon Nyengele
 * 
 */
import java.util.ArrayList;
import java.util.Collections;

import de.erichseifert.gral.data.DataTable;


public class DensityHelper {
	
	private static double[] oldSamples;		// The old samples in the window
	private static int N;					// How many samples have been read in
	
	/**
	 * Checks that the sample point X is within the domain of the density function.
	 * @param X : the data point to check
	 * @return  : whether or not the point is in the domain
	 */
	public static boolean inRange (double X) {
		return (X >= Settings.getMinimumRange() && X <= Settings.getMaximumRange());
	} // end inRange.
	
	/**
	 * Updates the function coefficients based on the incoming data point and
	 * the data point leaving the sliding window.
	 * 
	 * Post: the coefficients are updated as needed
	 * 
	 * @param Xnew : the new data point to update the coefficients based on
	 */
	public static void updateCoefficients(double Xnew){
		
		// The normalizing constant for the scaling basis functions
		double scaleNormalizer = Math.pow(2, Settings.startLevel/2.0);
		if (Settings.agingFlag == Settings.windowAge) {
			scaleNormalizer /= Settings.windowSize;
		}
		else if (Settings.agingFlag == Settings.caudleAge) {
			scaleNormalizer *= (1 - Settings.agingTheta);
		}
		
		// Scale coefficients if Caudle aging is being used
		if (Settings.agingFlag == Settings.caudleAge) {
			for (int scalIndex = 0; 
					scalIndex < Transform.scalingCoefficients.size();
					scalIndex++) {
				double newCoef = Settings.agingTheta*Transform.scalingCoefficients.get(scalIndex);
				Transform.scalingCoefficients.set(scalIndex,  newCoef);
			}
				
		}
		
		// Subtract old samples effect if window aging is used
		else if (Settings.agingFlag == Settings.windowAge) {
			
			// Only remove a sample if there have been more than window size samples
			if (N > Settings.windowSize){ 
				double Xold = oldSamples[N % Settings.windowSize];
			
			
				//Loop through the translations for the scaling basis functions
				int scaleInd = 0;
				for (double k : Transform.scalingTranslates) {
				
					// Get the translated & scaled data point
					double xScaled = Math.pow(2, Settings.startLevel) * Xold - k;
				
					// If the wavelet supports the data point, update the coefficient
					if (Wavelet.inSupport(xScaled)) {
						double scaleNew = Transform.scalingCoefficients.get(scaleInd) 
							          	- scaleNormalizer*Wavelet.getPhiAt(xScaled);
						Transform.scalingCoefficients.set(scaleInd, scaleNew);
					
					}
					scaleInd++;
				}
			}
			
			oldSamples[N % Settings.windowSize] = Xnew;
			N++;
		}
		
		//Loop through the translations for the scaling basis functions
		int scaleInd = 0;
		for (double k : Transform.scalingTranslates) {
			
			// Get the translated & scaled data point
			double xScaled = Math.pow(2, Settings.startLevel) * Xnew - k;
			
			// If the wavelet supports the data point, update the coefficient
			if (Wavelet.inSupport(xScaled)) {
				double scaleNew = Transform.scalingCoefficients.get(scaleInd) 
						          + scaleNormalizer*Wavelet.getPhiAt(xScaled);
				Transform.scalingCoefficients.set(scaleInd, scaleNew);
				
			}
			scaleInd++;
		}
	} // end updateCoefficients
	
	/**
	 * Find the maximum and minimum translation indices which
	 * support the incoming data point.
	 * 
	 * @param X : the data point
	 * @param j : the resolution level
	 * @return : An array containing the minimum and maximum
	 *           translation indices which support the data point
	 */
	public static double[] findRelevantKIndices(double X, int j) {
		//NOT YET VETTED
		
		// Get the max & min values for the wavelet's support
		double[] waveletMinMax = Wavelet.getSupport();
		
		double kMin = Math.ceil(Math.pow(2,j*X) - waveletMinMax[0]);
		double kMax = Math.floor(Math.pow(2,j*X) - waveletMinMax[1]);
		double[] kMinMax = new double[2];
		kMinMax[0] = kMin;
		kMinMax[1] = kMax;
		return (kMinMax);
		
	} //end findRelevantKIndices
	
	/**
	 * Initializes the translates for the scaling basis functions,
	 * based off of the maximum/minimum values supported and
	 * the starting resolution level.
	 * 
	 * Post: the translates in Transform are set appropriately
	 * @return the translates for the scaling basis functions
	 */
	public static void initializeTranslates() {
		
		// Initialize the scaling translates
		Transform.scalingTranslates = new ArrayList<Double> ();
		int startTranslate = (int) Math.floor((Math.pow(2,Settings.startLevel)*Settings.getMinimumRange())-Wavelet.getSupport()[1]);
		int stopTranslate = (int) Math.ceil((Math.pow(2,Settings.startLevel)*Settings.getMaximumRange())-Wavelet.getSupport()[0]);
		for (double k = startTranslate; k <= stopTranslate; k++){
			Transform.scalingTranslates.add(k);
		}
		
		
		// Initialize the wavelet translates if wavelets are being used
		if (Settings.waveletFlag) {
			Transform.waveletTranslates = new ArrayList<ArrayList<Double>> ();
		
			// Loop through resolutions
			for (int j = Settings.startLevel; j <= Settings.stopLevel; j++){
				
				ArrayList<Double> jTranslates = new ArrayList<Double> ();
				int startWTranslate = (int) Math.floor((Math.pow(2,j)*Settings.getMinimumRange())-Wavelet.getSupport()[1]);
				int stopWTranslate = (int) Math.ceil((Math.pow(2,j)*Settings.getMaximumRange())-Wavelet.getSupport()[0]);
				for (double k = startWTranslate; k <= stopWTranslate; k++){
					jTranslates.add(k);
				}
				Transform.waveletTranslates.add(jTranslates);
			}
		}
	} //end initializeTranslates
	
	/**
	 * Initializes the arrays to hold the scaling basis function
	 * and wavelet basis function coefficients based off
	 * of the resolution levels.
	 * 
	 * Post: the coefficients arrays are of the appropriate size.
	 */
	public static void initializeCoefficients() {
		
		// Create window to store old samples
		if (Settings.agingFlag == Settings.windowAge) {
			oldSamples = new double[Settings.windowSize];
			N = 0;
		}
		
		// Set all scaling coefficients to 0
		Transform.scalingCoefficients = new ArrayList<Double>(Collections.nCopies(Transform.scalingTranslates.size(), 0.0));
		
		/*if (Settings.waveletFlag) {
			Transform.waveletCoefficients = new ArrayList<ArrayList<Double>> ();
			
			// Loop through resolutions
			for (int j = Settings.startLevel; j <= Settings.stopLevel; j++){
				
				// Set all wavelet at this resolution coefficients to 0
				ArrayList<Double> jCoefficients = new ArrayList<Double> (Collections.nCopies
													(Transform.waveletTranslates.get(j).size(), 0.0));
				Transform.waveletCoefficients.add(jCoefficients);
			}
		}*/
	} //end intializeCoefficients
	
	/**
	 * Calculates the density at each discrete point in the
	 * range pre-specified.
	 * 
	 * Pre: the coefficient matrices have been created and
	 *      are the proper size
	 * @return the normalized density estimate
	 */
	public static ArrayList<Double> getDensity() {
		
		ArrayList<Double> density = new ArrayList<Double> ();
		double scaleNormalizer = Math.pow(2, Settings.startLevel/2.0);
		
		// Calculate un-normalized density for each point in domain
		for (double i = Settings.getMinimumRange(); 
				i < Settings.getMaximumRange(); i += Settings.discretization) {
			
			// Density at point i
			double iDense = 0.0;
			
			// Cycle through translates for each point
			int scalIndex = 0;
			for (double k : Transform.scalingTranslates) {
				double Xi = Math.pow(2, Settings.startLevel)*i - k;
				
				// Only update if the point is supported
				if (Wavelet.inSupport(Xi)) {
					iDense += Transform.scalingCoefficients.get(scalIndex) 
							  * Wavelet.getPhiAt(Xi) * scaleNormalizer;
				}
				scalIndex++;
			}
			density.add(iDense);
		}
		
		//Normalize density
		density = normalizeDensity(density);
		
		return(density);
	}
	
	/**
	 * Takes in an un-normalized density estimate and returns the
	 * normalized version, using the normalization procedure from
	 * Gajek (1986) 'On improving density estimators which are
	 * not Bona Fide functions'.
	 * 
	 * @param unNormDensity : the un-normalized density estimate
	 *                        over the domain range.
	 * @return the normalized density.
	 */
	private static ArrayList<Double> normalizeDensity(ArrayList<Double> unNormDensity){
		
		ArrayList<Double> normDens = unNormDensity;
		int iter = 0;
		double threshold = Math.pow(10, -8);
		double densityDomainSize = Settings.getMaximumRange() - Settings.getMinimumRange();
		
		while (iter < 1000) {
			
			// Zero negative points
			for (int i = 0; i < unNormDensity.size(); i++) {
				if (normDens.get(i) < 0.0) {
					normDens.set(i,0.0);
				}
			}
			
			// Sum over probability density over interval
			double integralSum = 0.0;
			for (int i = 0; i < unNormDensity.size(); i++) {
				integralSum += normDens.get(i)*Settings.discretization;
			}
			
			// Return if error is under threshold
			if (Math.abs(integralSum - 1) < threshold) {
				return (normDens);
			}
			
			// Modify density so that it integrates to 1
			double normalizeConstant = (integralSum - 1) / densityDomainSize;
			for (int i = 0; i < unNormDensity.size(); i++) {
				
				normDens.set(i, normDens.get(i) - normalizeConstant);
			}
			
			iter++;
		}
		
		// Settle for the current approximation
		return normDens;
	} //end normalizeDensity
	
	/**
	 * Takes the normalized density over the supported range and
	 * returns the same, formatted for GRAL compatibility
	 * @param density : the data table containing the density information
	 * @return the density over the support
	 */
	public static void updateDensity(DataTable densityTable) {
		
		ArrayList<Double> normDensity = getDensity();
		
		// Update each density in the range
        for (int i = 0; i < normDensity.size(); i++) {
            double Yi = normDensity.get(i);
            densityTable.set(1, i, Yi);
        }

	}
	
}

