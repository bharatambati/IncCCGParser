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
public class DerivDepInfo {
    
    private final int lid;
    private final int rid;
    private final boolean isHeadLeft;
    private final String lCat;
    private final String rCat;
    
    public DerivDepInfo(int leftid, int rightid, boolean isheadleft, String lcat, String rcat){
        lid = leftid;
        rid = rightid;
        isHeadLeft = isheadleft;
        lCat = lcat;
        rCat = rcat;
    }
    
    public boolean isHeadLeft(){
        return isHeadLeft;
    }
    
    public String getLeftCat(){
        return lCat;
    }
    
    public String getRightCat(){
        return rCat;
    }
    
    public int getLeftId(){
        return lid;
    }
    
    public int getRightId(){
        return rid;
    }
}
