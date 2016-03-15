/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/

package ilcc.ccgparser.utils;

import ilcc.ccgparser.utils.DataTypes.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ambati
 */

public class Feature implements Comparable<Feature>{
    public enum FeatPrefix {
        s0w, s0p, s0c, s0wp, s0pc, s0wc, s1w, s1p, s1c, s1wp, s1pc, s1wc,
        s2w, s2p, s2c, s2wp, s2pc, s2wc, s3w, s3p, s3c, s3wp, s3pc, s3wc,
        q0w, q0p, q0wp, q1w, q1p, q1wp, q2w, q2p, q2wp, q3w, q3p, q3wp,
        s0Lwc, s0Lpc, s0Rwc, s0Rpc, s0Uwc, s0Upc,
        s1Lwc, s1Lpc, s1Rwc, s1Rpc, s1Uwc, s1Upc,
        
        s0wcs1wc, s0cs1w, s0ws1c, s0cs1c, 
        s0wcq0wp, s0wcq0p, s0cq0wp, s0cq0p, 
        s1wcq0wp, s1cq0wp, s1wcq0p, s1cq0p, 
        
        //Set 5
        s0wcs1cs2c, s0cs1wcs2c, s0cs1cs2wc, s0cs1cs2c, s0ps1ps2p,
        s0wcs1cq0p, s0cs1wcq0p, s0cs1cq0wp, s0cs1cq0p, s0ps1pq0p,
        s0wcq0pq1p, s0cq0wpq1p, s0cq0pq1wp, s0cq0pq1p, s0pq0pq1p,
        //Set 6
        s0cs0hcs0lc, s0cs0lcs1c, s0cs0lcs1w, s0cs0hcs0rc, s0cs0rcq0p, 
        s0cs0rcq0w, s0cs1cs1rc, s0ws1cs1rc, s1cs1hcs1rc,
        //Incremental
        l1c, l2c, l3c, l4c, l5c, l1cs0c, l2cs0c, l3cs0c, l4cs0c, l5cs0c,        
        l1cs0cs1c, l2cs0cs1c, l3cs0cs1c, l4cs0cs1c, l5cs0cs1c,
        l1cs1c, l2cs1c, l3cs1c, l1p, l2p, l3p, 
        l1ps0ps1p, l2ps0ps1p, l3ps0ps1p, l4ps0ps1p, l5ps0ps1p, 
        l1wcs0cs1c, l2wcs0cs1c, l3wcs0cs1c, l4wcs0cs1c, l5wcs0cs1c, 
        
    }
    
    private final String featStr;
    private final static Map<String, Feature> cache = Collections.synchronizedMap(new HashMap<String, Feature>());
    
    public static Feature make(FeatPrefix pre, List flist) {
        StringBuilder sb = new StringBuilder(pre.toString());
        sb.append(":");
        for(int i = 0; i < flist.size(); i++){
            sb.append(flist.get(i).toString());
            sb.append("--");
        }
        String feat = sb.substring(0, sb.length()-2);
        Feature result = cache.get(feat);
        if (result == null) {
            result = new Feature(feat);
            cache.put(feat, result);
        }
        return result;
    }
    
    public static Feature make(String feat) {
        Feature result = cache.get(feat);
        if (result == null) {
            result = new Feature(feat);
            cache.put(feat, result);
        }
        return result;
    }
    
    private Feature(String str){
        featStr = str;
    }
    
    public String getFeatStr(){
        return featStr;
    }
    
    @Override
    public String toString(){
        return featStr;
    }
    
    @Override
    public int compareTo(Feature o) {
        return this.toString().compareTo(o.toString());
    }
}
