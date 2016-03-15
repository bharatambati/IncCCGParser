/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/

package ilcc.ccgparser.test;

import ilcc.ccgparser.utils.DataTypes.*;
import ilcc.ccgparser.utils.Feature;
import ilcc.ccgparser.utils.Feature.*;
import java.util.Arrays;
import java.util.HashMap;

/**
 *
 * @author ambati
 */
public class FeatureTest {
    
    public static void main(String[] args){
        HashMap<Feature, Integer> map = new HashMap<>();
        Feature feat1 = Feature.make(FeatPrefix.s0w, Arrays.asList(Word.make("with"), POS.make("IN")));
        Feature feat2 = Feature.make(FeatPrefix.s0w, Arrays.asList(Word.make("with"), POS.make("IN")));
        map.put(feat1, 1);
        if(map.get(feat2) != null)
        //if(feat1.equals(feat2))
            System.out.println("Equals");
    }
}
