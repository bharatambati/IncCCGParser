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
public class CCGDepList {
    
    private final int headId;
    private final int argId;
    private final int slot;
    private final String headCat;
    private final boolean extracted;
    private CCGDepList next;
    
    public CCGDepList(int hid, int aid, int slt, String cat, boolean ext, double time){
        headId = hid;
        argId = aid;
        slot = slt;
        headCat = cat;
        extracted = ext;
        next = null;
    }
    
    public void append(CCGDepList deplist){
        if (next != null)
            next.append(deplist);
        else next = deplist;
    }
    
    public int getSlot(){
        return slot;
    }
    
    public String getCat(){
        return headCat;
    }
    
    public int getHeadId(){
        return headId;
    }
    
    public int getArgId(){
        return argId;
    }
    
    public boolean getExtract(){
        return extracted;
    }
    
    public CCGDepList getNext(){
        return next;
    }
    
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(headId);sb.append("--");
        sb.append(argId);sb.append("--");
        sb.append(headCat);sb.append("--");
        sb.append(slot);
        return sb.toString();
    }
}
