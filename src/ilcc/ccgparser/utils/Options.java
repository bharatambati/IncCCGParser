/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.utils;

/**
 *
 * @author ambati
 */
public class Options {
    
	/** Time (in minutes) before timeout for each parse. */
	private double timeoutMins;
        private boolean timeout;
        private static Options theOptions;

	public Options(){
            theOptions = this;
            timeoutMins = 5;
	}

	/**
	 * Constructor for the command-line interface.
	 * Reads from stdin.
	 * @param args The command-line parameters
	 */
	public Options(String[] args){
            theOptions = this;
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-timeout") || args[i].equals("-to")){
                    timeout = true;
                    if (checkNext(args,i)){
                        System.err.println("No timout value given!");
                        System.exit(1);
                    }
                    else timeoutMins = Double.parseDouble(args[++i]);
                }
            }
	}
	
	private boolean checkNext(String[] args, int i){
		if (args.length <= i+1) return true;
		if (args[i+1].startsWith("-")) return true;
		return false;
	}
	
	public String toString(){
		String text = "### Using settings:\n";
		if (timeout) text += "#Timeout time (mins):\t"+timeoutMins+"\n";
		return text;
	}
	
	public static Options getOptions(){
		//Check if initialised
		return theOptions;
	}
	
        public void setTimeoutMins(double mins){            
            timeout = true;
            timeoutMins = mins;
        }
        
	public double getTimeoutMins() {return timeoutMins;}
}
