/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.test;

import ilcc.ccgparser.incderivation.NonInc;
import ilcc.ccgparser.incderivation.SRParser;
import ilcc.ccgparser.utils.CCGDepInfo;
import ilcc.ccgparser.utils.CCGJSentence;
import ilcc.ccgparser.utils.CCGJTreeNode;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author ambati
 */
public class Auto2Parg {
    
    public static void main(String[] args) throws IOException, Exception {
        
        String autofile, pargfile, stagfile, gpargfile;
        
        if(args.length == 4){
            autofile = args[0];
            pargfile = args[1];
            stagfile = args[2];
            gpargfile = args[3];
        }
        else {
            autofile = "/home/ambati/ilcc/projects/parsing/experiments/english/ccg/data/orig/test.gccg.auto";
            pargfile = "/home/ambati/ilcc/projects/parsing/experiments/english/ccg/data/orig/test.gccg.jparg";
            stagfile = "/home/ambati/ilcc/projects/parsing/experiments/english/ccg/data/orig/test.gccg.stagged";
            gpargfile = "/home/ambati/ilcc/projects/parsing/experiments/english/ccg/data/orig/test.gccg.parg";
        }
        
        Auto2Parg a2p = new Auto2Parg();
        a2p.parse(autofile, pargfile, stagfile);
        a2p.evaluate(gpargfile, pargfile);
    }
    
    public void parse(String autoFile, String pargFile, String stagFile) throws IOException, Exception{
        
        SRParser srparser = new NonInc();
        
        BufferedReader derivReader = new BufferedReader(new FileReader(new File(autoFile)));
        BufferedWriter out = new BufferedWriter(new FileWriter(new File(pargFile)));
        BufferedWriter stagout = new BufferedWriter(new FileWriter(new File(stagFile)));
        stagout.write("# Generated using \n# Auto2Parg.java\n\n");
        
        String dLine;
        
        System.out.println("Processing: ");
        srparser.init();
        int count = 0;
        while (derivReader.readLine() != null) {
            count++;
            CCGJSentence sent = new CCGJSentence();
            dLine = srparser.getccgDeriv(derivReader);
            CCGJTreeNode root = srparser.parseDrivString(dLine, sent);
            //srparser.getDerivDeps(root);
            printDeps(root, out, stagout, count, sent);
        }
        out.flush();
        out.close();
        stagout.flush();
        stagout.close();
    }
    
    public void printDeps(CCGJTreeNode root, BufferedWriter out, BufferedWriter stagout, int sentcount, CCGJSentence sent) throws IOException{
        HashMap<String, CCGDepInfo> goldccgDeps = root.getDeps();
        ArrayList<CCGJTreeNode> leaves = getLeaves(root);
        String superStr = getSuperStr(leaves);
        out.write("<s id="+sentcount+"> "+goldccgDeps.size()+" <c>"+superStr+"</c>\n");
        stagout.write(superStr+"\n");
        for(String key : goldccgDeps.keySet()){
            CCGDepInfo depinfo = goldccgDeps.get(key);
            out.write(depinfo.ccgDepStr()+"\t"+sent.getNode(depinfo.getArgId()-1).getWrdStr()+"\t"+sent.getNode(depinfo.getHeadId()-1).getWrdStr()+"\n");
        }
        out.write("<\\s>\n");
    }
    
    private String getSuperStr(ArrayList<CCGJTreeNode> leaves){
        StringBuilder sb = new StringBuilder();
        for(CCGJTreeNode leaf : leaves){
            sb.append(leaf.getHeadWrd().toString());sb.append("|");
            sb.append(leaf.getPOS().toString());sb.append("|");
            sb.append(leaf.getCCGcat().toString());sb.append(" ");
        }
        return sb.toString().trim();
    }
    
    public ArrayList<CCGJTreeNode> getLeaves(CCGJTreeNode root){
        ArrayList<CCGJTreeNode> leaves = new ArrayList<>();
        postOrder(root, leaves);
        return leaves;
    }
    
    public void postOrder(CCGJTreeNode root, ArrayList list){
        if(root == null)
            return;
        postOrder(root.getLeftChild(), list);
        postOrder(root.getRightChild(), list);
        if(root.isLeaf())
            list.add(root);
    }

    
    public void evaluate(String gpargfile, String opargfile) throws FileNotFoundException, IOException{
        BufferedReader gpReader = new BufferedReader(new FileReader(new File(gpargfile)));
        BufferedReader opReader = new BufferedReader(new FileReader(new File(opargfile)));
        
        HashMap<Integer, HashMap<String, CCGDepInfo>> goldccgDeps, sysccgDeps;
        
        goldccgDeps = getccgDepMap(gpReader);
        sysccgDeps = getccgDepMap(opReader);
        evaluateParseDependenciesJulia(goldccgDeps, sysccgDeps);
    }
    
    public void evaluateParseDependenciesJulia(HashMap<Integer, HashMap<String, CCGDepInfo>> gcDeps, HashMap<Integer, HashMap<String, CCGDepInfo>> scDeps){
        int sGoldDeps, sSysDeps, sCorrDeps = 0, lsCorrDeps = 0;
        int uGold, uSys, uCorr, lCorr;
        uGold = uSys = uCorr = lCorr = 0;
        HashMap<String, CCGDepInfo> goldccgDeps, sysccgDeps;
        for(int count = 1; count <= gcDeps.size(); count++){
            goldccgDeps = gcDeps.get(count);
            sysccgDeps = scDeps.get(count);
            sGoldDeps = goldccgDeps.size();
            sSysDeps = sysccgDeps.size();
            sCorrDeps = lsCorrDeps = 0;
            for(String key : sysccgDeps.keySet()){
                CCGDepInfo sdinfo = sysccgDeps.get(key);
                String[] split = key.split("--");
                String key2 = split[1]+"--"+split[0];
                if(goldccgDeps.containsKey(key)){
                    sCorrDeps++;
                    CCGDepInfo gdinfo = goldccgDeps.get(key);
                    if(sdinfo.getCat().equals(gdinfo.getCat()) && sdinfo.getSlot()==gdinfo.getSlot()){
                        lsCorrDeps++;
                        goldccgDeps.remove(key);
                    }
                }
                else if(goldccgDeps.containsKey(key2))
                    sCorrDeps++;
            }
            
            uGold += sGoldDeps;
            uSys += sSysDeps;
            uCorr += sCorrDeps;
            lCorr += lsCorrDeps;
        }
        DecimalFormat df = new DecimalFormat(".00");
        System.out.println();
        System.out.println("goldccgDeps, sysDeps, corrDeps : "+uGold+" "+uSys+" "+uCorr);
        double UP = 100.00*uCorr/uSys;
        double UR = 100.00*uCorr/uGold;
        double UF = (2.00*UP*UR)/(UP+UR);
        
        double LP = 100.00*lCorr/uSys;
        double LR = 100.00*lCorr/uGold;
        double LF = (2.00*LP*LR)/(LP+LR);
        
        System.out.println(" Unlabelled Prec : "+df.format(UP)+" Rec : "+df.format(UR)+" F-score : "+df.format(UF));
        System.out.println(" Labelled Prec : "+df.format(LP)+"  Rec : "+df.format(LR)+" F-score : "+df.format(LF));
        System.out.println();
    }
    
    private HashMap<Integer, HashMap<String, CCGDepInfo>> getccgDepMap(BufferedReader iReader) throws IOException{
        String line;
        int sentcount = 0;
        HashMap<Integer, HashMap<String, CCGDepInfo>> ccgDeps = new HashMap<>();
        HashMap<String, CCGDepInfo> tmpmap = new HashMap<>();
        while ( (line = iReader.readLine()) != null) {
            if(line.startsWith("# ") || line.trim().isEmpty())
                continue;
            if(line.startsWith("<s id")){
                tmpmap = new HashMap<>();                
                sentcount++;
            }
            else if(line.startsWith("<\\s"))
                ccgDeps.put(sentcount, tmpmap);
            
            else{
                String parts[] = line.trim().replaceAll("[ \t]+","\t").split("\t");
                String key = parts[1]+"--"+parts[0];
                CCGDepInfo info = new CCGDepInfo(Integer.parseInt(parts[1]), Integer.parseInt(parts[0]), Integer.parseInt(parts[3]), parts[2], false, 0.0);
                tmpmap.put(key, info);
            }
        }
        return ccgDeps;
    }
}
