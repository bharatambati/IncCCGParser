/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.incderivation;

import ilcc.ccgparser.utils.Feature;
import ilcc.ccgparser.utils.ArcJAction;
import java.util.HashMap;

/**
 *
 * @author ambati
 */
public class TempAgenda {
   
   private final PStateItem state;
   private final ArcJAction action;
   private final HashMap<Feature, Integer> featMap;
   private final HashMap<Feature, Integer> sentMap;
   private final double score;
   
   public TempAgenda(PStateItem item, ArcJAction act, HashMap<Feature, Integer> fMap, HashMap<Feature, Integer> sfMap, double val){
       state = item;
       action = act;
       featMap = fMap;
       sentMap = sfMap;
       score = val;
   }
   
   public double getScore(){
       return score;
   }
   
   public PStateItem getState(){
       return state;
   }
   
   public ArcJAction getAction(){
       return action;
   }
   
   public HashMap<Feature, Integer> getFeatureList(){
       return featMap;
   }
   
   public HashMap<Feature, Integer> getSentFeatureList(){
       return sentMap;
   }
}
