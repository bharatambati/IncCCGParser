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
public class CCGDepInfo {
    
    private final int headId;
    private final int argId;
    private final int slot;
    private final String headCat;
    private final boolean extracted;
    private double waitTime;
    
    public CCGDepInfo(int hid, int aid, int slt, String cat, boolean ext, double time){
        headId = hid;
        argId = aid;
        slot = slt;
        headCat = cat;
        extracted = ext;
        waitTime = time;
    }
    
    public CCGDepInfo copy(){
        return new CCGDepInfo(this.headId, this.argId, this.slot, this.headCat, this.extracted, this.waitTime);
    }
    
    public void setWaitTime(double time){
        waitTime = time;
    }
    
    public void increaseWaitTime(double time){
        waitTime += time;
    }
    
    public double getWaitTime(){
        return waitTime;
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
    
    public String ccgDepStr(){
        StringBuilder sb = new StringBuilder();
        sb.append(argId-1);sb.append("\t");
        sb.append(headId-1);sb.append("\t");
        sb.append(headCat);sb.append("\t");
        sb.append(slot);sb.append("\t");
        return sb.toString();
                
    }
    
    @Override
    public String toString(){
        return headId+"--"+argId+"--"+headCat+"--"+slot;
    }
}
