package ilcc.ccgparser.test;

import edinburgh.ccg.deps.CCGcat;
import edinburgh.ccg.deps.DepList;
import ilcc.ccgparser.incderivation.PStateItem;
import ilcc.ccgparser.utils.ArcJAction;
import ilcc.ccgparser.utils.CCGJNode;
import ilcc.ccgparser.utils.CCGJRuleInfo;
import ilcc.ccgparser.utils.CCGJTreeNode;
import ilcc.ccgparser.utils.SCoNLLNode;
import ilcc.ccgparser.utils.Utils.SRAction;
import ilcc.ccgparser.utils.ccgCombinators;
import ilcc.ccgparser.utils.ccgCombinators.RuleType;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ambati
 */
public class CatTest {
  
    public void printCategoryDeps(CCGcat cat1, CCGcat cat2, CCGcat res){
        DepList deps = new DepList(null, null, null, -1, -1);
        deps.append(cat1.filledDependencies);
        deps.append(cat2.filledDependencies);
        deps.append(res.filledDependencies);
        while(deps!=null){
            if(deps.headCat != null)
                System.out.println(cat1.catStringRecIndexed()+" "+cat2.catStringRecIndexed()+" "+res.catStringRecIndexed()+" "+deps.headIndex+" "+deps.argIndex+" "+deps.headCat+" "+deps.argPos+" ");
            deps = deps.next;
        }
        System.out.println("\n");
    }
    
    public void functions(){
        
        CCGJNode node1 = new CCGJNode();        
        CCGJNode node2 = new CCGJNode();
        CCGJNode node3 = new CCGJNode();
        CCGJNode node4 = new CCGJNode();
        CCGJNode node5 = new CCGJNode();
        CCGJNode node6 = new CCGJNode();
        CCGJNode node7 = new CCGJNode();
        
        node1.setCCGcat(CCGcat.lexCat("1", "N/N"));
        node2.setCCGcat(CCGcat.lexCat("2", "N/N"));
        node3.setCCGcat(CCGcat.lexCat("3", "N"));
        
        node4.setLeftNode(node1);
        node4.setRightNode(node2);
        node5.setLeftNode(node3);
        node5.setRightNode(node4);
        
        node6.setLeftNode(node2);
        node6.setRightNode(node3);
        node7.setLeftNode(node1);
        node7.setRightNode(node6);
        
        System.out.println(node7.getRightNode().getRightNode().getCCGcat().toString());
        System.out.println(node5.getLeftNode().getCCGcat().toString());
        
        CCGcat cat1, cat2, cat3, cat4, cat5;
        CCGcat tcat1, tcat2, tcat3, tcat4, tcat5, resCat;
        
        cat1 = CCGcat.lexCat("", ",", 1);
        cat2 = CCGcat.lexCat("", "NP[conj]", 2);
        CCGcat.combine(cat1, cat2, "NP[conj]");
        
        cat1 = CCGcat.lexCat("John", "Y/Z", 1);
        cat2 = CCGcat.lexCat("read", "X\\Y", 2);
        tcat1 = CCGcat.backwardCrossedComposition(cat1, cat2);
        
        cat1 = CCGcat.lexCat("John", "S/(S\\NP)", 1);
        cat2 = CCGcat.lexCat("read", "S[dcl]\\NP", 2);
        tcat1 = CCGcat.forwardApplication(cat1, cat2);
        
        cat1 = CCGcat.lexCat("John", "NP", 1);
        cat2 = CCGcat.lexCat("read", "(S\\NP)/NP", 2);
        cat3 = CCGcat.lexCat("the", "NP/N", 3);
        cat4 = CCGcat.lexCat("book", "N", 4);
        
        tcat1 = CCGcat.typeRaiseForwardComposition(cat1, cat2, "S", CCGcat.FW);
        System.out.println(cat1.filledDependencies);System.out.println(cat2.filledDependencies);System.out.println(tcat1.filledDependencies);
        tcat2 = CCGcat.forwardComposition(tcat1, cat3);
        System.out.println(tcat1.filledDependencies);System.out.println(cat3.filledDependencies);System.out.println(tcat2.filledDependencies);
        tcat3 = CCGcat.forwardApplication(tcat2, cat4);
        System.out.println(tcat2.filledDependencies);System.out.println(cat4.filledDependencies);System.out.println(tcat3.filledDependencies);
        
        cat1 = CCGcat.lexCat("John", "NP", 1);
        cat2 = CCGcat.lexCat("has", "(S[dcl]\\NP)/(S[pt]\\NP)", 2);
        cat3 = CCGcat.lexCat("been", "(S[pt]\\NP)/(S[ng]\\NP)", 3);
        cat4 = CCGcat.lexCat("sleeping", "S[ng]\\NP", 4);
        cat5 = CCGcat.lexCat("today", "(S\\NP)\\(S\\NP)", 5);
        tcat5 = CCGcat.lexCat("today", "S\\S", 5);
        
        ccgCombinators combs = new ccgCombinators();
        CCGJRuleInfo info = combs.checkCCGRules(cat4, tcat5);
        tcat1 = CCGcat.backwardApplication(cat4, tcat5);
        printCategoryDeps(cat4, cat5, tcat1);        
        tcat2 = CCGcat.forwardApplication(cat3, tcat1);
        printCategoryDeps(cat3, tcat1, tcat2);
        tcat3 = CCGcat.forwardApplication(cat2, tcat2);
        printCategoryDeps(cat2, tcat2, tcat3);
        tcat4 = CCGcat.backwardApplication(cat1, tcat3);
        printCategoryDeps(cat1, tcat3, tcat4);
        
        tcat1 = CCGcat.typeRaiseForwardComposition(cat1, cat2, "S", CCGcat.FW);
        printCategoryDeps(cat1, cat2, tcat1);        
        tcat2 = CCGcat.forwardComposition(tcat1, cat3);
        printCategoryDeps(tcat1, cat3, tcat2);
        tcat3 = CCGcat.forwardApplication(tcat2, cat4);
        tcat3.setHeads(cat4.heads(), cat4.getHeadId());
        printCategoryDeps(tcat2, cat4, tcat3);
        tcat4 = CCGcat.backwardApplication(tcat3, tcat5);
        printCategoryDeps(tcat3, tcat5, tcat4);
        
        
        /*
        cat1 = CCGcat.lexCat("John", "NP", 13);
        cat2 = CCGcat.lexCat("loves", "(S[dcl]\\NP)/NP", 14);
        
        cat1 = CCGcat.lexCat("John", "NP", 13);
        cat2 = CCGcat.lexCat("loves", "(S[dcl]\\NP)/(S[b]\\NP)", 14);
        tcat1 = cat1.typeRaise("S", CCGcat.FW);
        resCat = CCGcat.forwardComposition(tcat1, cat2);
        System.out.println(resCat.getIndCatDeps());
        System.out.println(resCat.catStringIndexed());
        //System.out.println(resCat.filledDependencies.toString());
        cat1 = CCGcat.lexCat("", "S\\NP");
        resCat = CCGcat.typeChangingRule(cat1, "NP\\NP");
        System.out.println(); System.out.println(cat1.getIndCatDeps()); System.out.println(resCat.getIndCatDeps());
        
        cat1 = CCGcat.lexCat("", "(S\\NP)\\(S\\NP)");
        resCat = CCGcat.typeChangingRule(cat1, "S\\S");
        System.out.println(); System.out.println(cat1.getIndCatDeps()); System.out.println(resCat.getIndCatDeps());
        
        cat1 = CCGcat.lexCat("", "(S\\NP)\\(S\\NP)");
        resCat = CCGcat.typeChangingRule(cat1, "S\\NP");
        System.out.println(); System.out.println(cat1.getIndCatDeps()); System.out.println(resCat.getIndCatDeps());
        
        cat1 = CCGcat.lexCat("", "(S[dcl]\\NP)/(S[b]\\NP)");
        
        cat1 = CCGcat.lexCat("", "S/(NP\\NP)");
        cat2 = CCGcat.lexCat("", "(NP\\NP)/NP");
        resCat = CCGcat.combine(cat1, cat2, "S/NP");
        cat1 = CCGcat.lexCat("John", "NP", 13);
        String resCatStr1 = "S[dcl]/NP";
        CCGcat trcat1 = CCGcat.typeRaiseTo(cat1, "S/(S\\NP)");
        CCGcat resCat1 = CCGcat.combine(trcat1, cat2, resCatStr1);
        //printDeps(resCat1, resCatStr1);
               
        cat1 = CCGcat.lexCat("Nov.", "((S\\NP)\\(S\\NP))/N[num]", 15);
        cat2 = CCGcat.lexCat("Nov.", "N[num]", 16);
        resCatStr1 = "(S\\NP)\\(S\\NP)";
        //System.out.println(cat1.indexedCatString());
        //System.out.println(cat1.getIndCatDeps());
        resCat1 = CCGcat.combine(cat1, cat2, resCatStr1);
        //printDeps(resCat1, resCatStr1);
        
        //CCGcat ccgCat = CCGcat.typeRaiseTo(resCat1, resCat1.catString);
        //if (ccgCat == null)
        //    ccgCat = CCGcat.typeChangingRule(resCat1, resCat1.catString);
                */
    }
    
    public static void main(String[] args) throws IOException, Exception {
        CatTest test = new CatTest();
        String str = "X\\X";
        if(str.equals("X\\X"))
            System.out.println("True");
        test.functions2();
        test.functions();
    }
    
    public void functions2() throws Exception{
        CCGJTreeNode node1 = CCGJTreeNode.makeLeaf(CCGcat.lexCat("John", "N", 1), new SCoNLLNode(1, "John", "NN", "N"));
        CCGJTreeNode node2 = CCGJTreeNode.makeLeaf(CCGcat.lexCat("likes", "(S\\NP)/NP", 2), new SCoNLLNode(2, "likes", "VB", "(S\\NP)/NP"));
        CCGJTreeNode node3 = CCGJTreeNode.makeLeaf(CCGcat.lexCat("Mary", "N", 3), new SCoNLLNode(3, "Mary", "NN", "N"));
        List<CCGJTreeNode> input = Arrays.asList(node1, node2, node3);
        PStateItem item = new PStateItem();
        item = item.applyShift(ArcJAction.make(SRAction.SHIFT, -1, "N", RuleType.lexicon), input, 0, false);
        PStateItem item1 = item.copy();
        PStateItem item2 = item.copy();        
        item1 = item1.applyReduce(ArcJAction.make(SRAction.RU, -1, "NP", RuleType.lex), 0, false);
        item1 = item1.applyShift(ArcJAction.make(SRAction.SHIFT, -1, "(S\\NP)/NP", RuleType.lexicon), input, 0, false);
        item1 = item1.applyShift(ArcJAction.make(SRAction.SHIFT, -1, "N", RuleType.lex), input, 0, false);
        item1 = item1.applyReduce(ArcJAction.make(SRAction.RU, -1, "NP", RuleType.lex), 0, false);
        item1 = item1.applyReduce(ArcJAction.make(SRAction.RR, -1, "S\\NP", RuleType.fa), 0, false);
        
        item2 = item2.applyReduce(ArcJAction.make(SRAction.RU, 1, "NP", RuleType.lex), 0, false);
        item2 = item2.applyReduce(ArcJAction.make(SRAction.RU, 1, "S/(S\\NP)", RuleType.lex), 0, false);
        item2 = item2.applyShift(ArcJAction.make(SRAction.SHIFT, -1, "(S\\NP)/NP", RuleType.lexicon), input, 0, false);
        item2 = item2.applyReduce(ArcJAction.make(SRAction.RL, -1, "S/NP", RuleType.gfc), 0, false);
        System.out.println(item1.getNode().getCCGcat());
        System.out.println(item2.getNode().getCCGcat());
        int a =10;
    }
        
        public static void printDeps(CCGcat resCat, String resCatStr) {
            if(resCat.catString.equals(resCatStr)){
                DepList dep = resCat.filledDependencies;
                while(dep!=null){
                    System.out.println(dep.argIndex+"\t"+dep.headIndex+"\t"+dep.headCat+"\t"+dep.argPos+"\t"+dep.extracted+"\t"+dep.bounded);
                    dep = dep.next();
                }                
            }
            else{
                System.out.println("Can't Combine");
            }
        }
}