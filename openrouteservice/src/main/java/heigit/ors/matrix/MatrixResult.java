/*|----------------------------------------------------------------------------------------------
 *|														Heidelberg University
 *|	  _____ _____  _____      _                     	Department of Geography		
 *|	 / ____|_   _|/ ____|    (_)                    	Chair of GIScience
 *|	| |  __  | | | (___   ___ _  ___ _ __   ___ ___ 	(C) 2014-2017
 *|	| | |_ | | |  \___ \ / __| |/ _ \ '_ \ / __/ _ \	
 *|	| |__| |_| |_ ____) | (__| |  __/ | | | (_|  __/	Berliner Strasse 48								
 *|	 \_____|_____|_____/ \___|_|\___|_| |_|\___\___|	D-69120 Heidelberg, Germany	
 *|	        	                                       	http://www.giscience.uni-hd.de
 *|								
 *|----------------------------------------------------------------------------------------------*/
package heigit.ors.matrix;

import java.util.HashMap;
import java.util.Map;

public class MatrixResult {
  public Map<Integer, float[]> _tables;
  
  public MatrixResult()
  {
	  _tables = new HashMap<Integer, float[]>();
  }
  
  public void setTable(int metric, float[] values)
  {
	  
  }
  
  public float[] getTable(int metric)
  {
	  return null;
  }
  
  //public void setDestinations(Coordinate[] coords,)
}
