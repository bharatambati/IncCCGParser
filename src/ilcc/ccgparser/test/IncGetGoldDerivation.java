/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.test;

import edu.stanford.nlp.util.StringUtils;
import ilcc.ccgparser.incderivation.IncParserGreedy;
import ilcc.ccgparser.incderivation.IncParser;
import ilcc.ccgparser.utils.CCGJTreeNode;
import ilcc.ccgparser.utils.ccgCombinators;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

/**
 *
 * @author ambati
 */
public class IncGetGoldDerivation {
    
    private static final Map<String, Integer> numArgs = new HashMap<>();
    static {
        numArgs.put("textFile", 1);
        numArgs.put("outFile", 1);
    }
    
    public static void main(String[] args) throws IOException, Exception {
        
        String trainAutoFile, trainConllFile, trainPargFile, testAutoFile, testPargFile, testConllFile, outAutoFile, outPargFile, modelFile, algo;
        
        String eeg = "/home/ambati/ilcc/projects/parsing/experiments/eeg/data/";
        trainAutoFile = eeg + "oracle/wsj.eeg.auto";
        trainConllFile = eeg + "oracle/wsj.eeg.conll";
        outAutoFile = eeg + "oracle/wsj.eeg.out.auto";
        
        if(args.length == 0){
            args = new String[] {
                "-trainCoNLL", trainConllFile, "-trainAuto", trainAutoFile, "-outAuto", outAutoFile,
            };
        }
        
        Properties props = StringUtils.argsToProperties(args, numArgs);
                
        IncParser parser;
        parser = new IncParserGreedy(props);
        BufferedWriter out = new BufferedWriter(new FileWriter(new File(props.getProperty("outAuto"))));
        if (props.getProperty("trainCoNLL") != null) {
            List<CCGJTreeNode> trees = parser.fillData();
            for(int i=0; i<trees.size(); i++){
                CCGJTreeNode tree = trees.get(i);
                if(tree != null)
                    writeDeriv(i+1, out, tree);
                else
                    out.write("ID=" + (i+1) + "\nError:No Derivation\n");
            }
        }
        out.close();
    }
    
    public static void postOrder(CCGJTreeNode root, ArrayList list) {
        if (root == null) {
            return;
        }
        postOrder(root.getLeftChild(), list);
        postOrder(root.getRightChild(), list);
        list.add(root);
    }

    public static void writeDeriv(int id, BufferedWriter odWriter, CCGJTreeNode root) throws IOException {
        ArrayList<CCGJTreeNode> list = new ArrayList<>();
        postOrder(root, list);
        Stack<String> sStack = new Stack<>();
        for (CCGJTreeNode node : list) {
            if (node.isLeaf()) {
                StringBuilder sb = new StringBuilder();
                String wrd, pos, cat;
                wrd = node.getWrdStr();
                pos = node.getPOS().toString();
                cat = node.getCCGcat().toString();
                sb.append("(<L ");
                sb.append(cat);
                sb.append(" ");
                sb.append(pos);
                sb.append(" ");
                sb.append(pos);
                sb.append(" ");
                sb.append(wrd);
                sb.append(" ");
                sb.append(cat);
                sb.append(">)");
                sStack.push(sb.toString());
            } else if (node.getChildCount() == 1) {

                StringBuilder sb = new StringBuilder();
                String cat;
                cat = node.getCCGcat().toString();
                sb.append("(<T ");
                sb.append(" ");
                sb.append(cat);
                sb.append(" lex ");
                sb.append(" 0 1> ");
                sb.append(sStack.pop());
                sb.append(" )");
                sStack.push(sb.toString());
            } else {
                String rstr = sStack.pop();
                String lstr = sStack.pop();
                StringBuilder sb = new StringBuilder();
                String cat = node.getCCGcat().toString();
                String dir = (node.getHeadDir() == 1) ? "0" : "1";
                sb.append("(<T ");
                sb.append(cat);
                sb.append(" ");
                sb.append(node.getArcAction().getRuleType());
                if (node.getArcAction().getRuleType().equals(ccgCombinators.RuleType.rreveal)) {
                    sb.append(node.getArcAction().getLevel());
                }
                sb.append(" ");
                sb.append(dir);
                sb.append(" 2> ");
                sb.append(lstr);
                sb.append(" ");
                sb.append(rstr);
                sb.append(" )");
                sStack.push(sb.toString());
            }
        }
        odWriter.write("ID=" + id + "\n");
        odWriter.write(sStack.pop().trim() + "\n");
        odWriter.flush();
    }
}