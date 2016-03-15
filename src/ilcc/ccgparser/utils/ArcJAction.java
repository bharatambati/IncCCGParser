/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/

package ilcc.ccgparser.utils;

import ilcc.ccgparser.utils.Utils.SRAction;
import ilcc.ccgparser.utils.DataTypes.CCGCategory;
import ilcc.ccgparser.utils.ccgCombinators.RuleType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author ambati
 */

public class ArcJAction implements Comparable<ArcJAction> {
    
    private final SRAction sraction;
    private final int level;
    private final CCGCategory ccgcat;
    private final RuleType ccgrule;
    private final static Map<String, ArcJAction> cache = Collections.synchronizedMap(new HashMap<String, ArcJAction>());
    
    public static ArcJAction make(SRAction act, int depth, String cat, RuleType ruleType) {
        ArcJAction result = cache.get(act+"--"+cat);
        if (result == null) {
            result = new ArcJAction(act, depth, cat, ruleType);
            cache.put(act+"--"+depth+"--"+cat, result);
        }
        return result;
    }
    
    private ArcJAction(SRAction act, int depth, String cat, RuleType ruleType){
        sraction = act;
        ccgcat = CCGCategory.make(cat);
        level = depth;
        ccgrule = ruleType;
    }
    
    public SRAction getAction(){
        return sraction;
    }
    
    public CCGCategory getccgCat(){
        return ccgcat;
    }
    
    public int getLevel(){
        return level;
    }
    
    public RuleType getRuleType(){
        return ccgrule;
    }
    
    @Override
    public int compareTo(ArcJAction act) {
        return this.toString().compareTo(act.toString());
    }
    
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(sraction.toString());sb.append("--");
        sb.append(level);sb.append("--");
        sb.append(ccgcat.toString());sb.append("--");
        sb.append(ccgrule);
        return sb.toString();
    }
    
    public String toString2(){
        StringBuilder sb = new StringBuilder();
        sb.append(sraction.toString());sb.append("--");
        sb.append(ccgcat.toString());sb.append("--");
        sb.append(ccgrule);
        if(sraction == SRAction.RREVEAL)
            sb.append(level);
        return sb.toString();
    }
    
    @Override
    public int hashCode(){
        return toString().hashCode();
    }
    
    @Override
    public boolean equals(Object object2) {
        return object2 instanceof ArcJAction && toString().equals(((ArcJAction)object2).toString());
    }
}
