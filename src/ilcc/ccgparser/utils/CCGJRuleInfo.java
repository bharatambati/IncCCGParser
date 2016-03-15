/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.utils;

import edinburgh.ccg.deps.CCGcat;
import ilcc.ccgparser.utils.ccgCombinators.RuleType;

/**
 *
 * @author ambati
 * 
 */
public class CCGJRuleInfo {
    
    private final CCGcat leftCat;
    private final CCGcat rightCat;
    private final CCGcat resultCat;
    private boolean headIsLeft;
    private final RuleType combinator;
    private final int level;
    private int count;
    
    public CCGJRuleInfo(CCGcat lcat, CCGcat rcat, CCGcat rescat, boolean dir, RuleType comb, int depth, int rcount){
        leftCat = lcat;
        rightCat = rcat;
        resultCat = rescat;
        headIsLeft = dir;
        combinator = comb;        
        level = depth;
        count = rcount;
    }
    
    public void setHeadDir(boolean flag){
        headIsLeft = flag;
    }
    
    public int getRuleCount(){
        return count;
    }
        
    public RuleType getCombinator(){
        return combinator;
    }
    
    public boolean getHeadDir(){
            return headIsLeft;
    }
    
    public CCGcat getLeftCat(){
        return leftCat;
    }
    
    public CCGcat getRightCat(){
        return rightCat;
    }
    
    public CCGcat getResultCat(){
        return resultCat;
    }
    
    public void setRuleCount(int val){
        count = val;
    }    
    
    public int getLevel(){
        return level;
    }
    
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(leftCat.toString()); sb.append("--");
        if(rightCat != null)
            sb.append(rightCat.toString()); sb.append("--");
        sb.append(resultCat.toString()); sb.append("--");
        sb.append(headIsLeft); sb.append("--");
        sb.append(combinator); sb.append("--");
        sb.append(level);
        
        return sb.toString();
    }
}