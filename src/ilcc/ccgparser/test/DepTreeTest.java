/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ilcc.ccgparser.test;

import edinburgh.ccg.deps.CCGcat;
import ilcc.ccgparser.incderivation.PStateItem;
import ilcc.ccgparser.utils.ArcJAction;
import ilcc.ccgparser.utils.CCGJTreeNode;
import ilcc.ccgparser.utils.DataTypes.CCGCategory;
import ilcc.ccgparser.utils.DepTree;
import ilcc.ccgparser.utils.SCoNLLNode;
import ilcc.ccgparser.utils.Utils;
import ilcc.ccgparser.utils.ccgCombinators;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author ambati
 */
public class DepTreeTest {
    
    public static void main(String[] args) throws Exception {
        
        DepTreeTest test = new DepTreeTest();
        test.functions2();
        test.depfunctions();
        
    }
    
    public void depfunctions(){
        
        DepTree node, node1, node2, node3, node4, node5, node6, node7, node8, node9;
        node1 = new DepTree(1, CCGCategory.make("N"));
        node2 = new DepTree(2, CCGCategory.make("(S\\NP)/NP"));
        node3 = new DepTree(3, CCGCategory.make("NP"));
        node4 = new DepTree(4, CCGCategory.make("(NP\\NP)/NP"));
        node5 = new DepTree(5, CCGCategory.make("NP"));
        
        
        node6= node2.copy();
        node6.add(node1.copy());
        node6.add(node3.copy());        
            node7 = node4.copy();
            node7.add(node5.copy());
        node6.add(node7);
        
        
        node8 = node3.copy();
        node8.add(node4.copy());
        node8.add(node5.copy());
        
        node9 = node2.copy();
        node9.add(node3.copy());
        node9.add(node1.copy());
        
        //System.out.println(((DepTree)node4.getChildAt(0)).getId());
        //System.out.println(((DepTree)node5.getChildAt(0)).getId());
        System.out.println(node6.breadthFirstEnumeration().toString());
        
        System.out.println(node9.breadthFirstEnumeration().toString());
        //node4 = new DepTree(node1.getId(), CCGCategory.make("NP"));
        //node5 = new DepTree(node4.getId(), CCGCategory.make("S/(S\\NP)"));        
        //node6 = new DepTree(node4.getId(), CCGCategory.make("S/NP"));
        //node6.add(node5.copy());
        
    }
    
    
    public void functions2() throws Exception{
        CCGJTreeNode node1 = CCGJTreeNode.makeLeaf(CCGcat.lexCat("John", "N", 1), new SCoNLLNode(1, "John", "NN", "N"));
        CCGJTreeNode node2 = CCGJTreeNode.makeLeaf(CCGcat.lexCat("likes", "(S\\NP)/NP", 2), new SCoNLLNode(2, "likes", "VB", "(S\\NP)/NP"));
        CCGJTreeNode node3 = CCGJTreeNode.makeLeaf(CCGcat.lexCat("Mary", "N", 3), new SCoNLLNode(3, "Mary", "NN", "N"));
        List<CCGJTreeNode> input = Arrays.asList(node1, node2, node3);
        PStateItem item = new PStateItem();
        item = item.applyShift(ArcJAction.make(Utils.SRAction.SHIFT, -1, "N", ccgCombinators.RuleType.lexicon), input, 0, false);
        PStateItem item1 = item.copy();
        PStateItem item2 = item.copy();
        item1 = item1.applyReduce(ArcJAction.make(Utils.SRAction.RU, -1, "NP", ccgCombinators.RuleType.lex), 0, false);
        item1 = item1.applyShift(ArcJAction.make(Utils.SRAction.SHIFT, -1, "(S\\NP)/NP", ccgCombinators.RuleType.lexicon), input, 0, false);
        item1 = item1.applyShift(ArcJAction.make(Utils.SRAction.SHIFT, -1, "N", ccgCombinators.RuleType.lex), input, 0, false);
        item1 = item1.applyReduce(ArcJAction.make(Utils.SRAction.RU, -1, "NP", ccgCombinators.RuleType.lex), 0, false);
        item1 = item1.applyReduce(ArcJAction.make(Utils.SRAction.RR, -1, "S\\NP", ccgCombinators.RuleType.fa), 0, false);
        
        item2 = item2.applyReduce(ArcJAction.make(Utils.SRAction.RU, 1, "NP", ccgCombinators.RuleType.lex), 0, false);
        item2 = item2.applyReduce(ArcJAction.make(Utils.SRAction.RU, 1, "S/(S\\NP)", ccgCombinators.RuleType.lex), 0, false);
        item2 = item2.applyShift(ArcJAction.make(Utils.SRAction.SHIFT, -1, "(S\\NP)/NP", ccgCombinators.RuleType.lexicon), input, 0, false);
        item2 = item2.applyReduce(ArcJAction.make(Utils.SRAction.RL, -1, "S/NP", ccgCombinators.RuleType.gfc), 0, false);
        System.out.println(item1.getNode().getCCGcat());
        System.out.println(item2.getNode().getCCGcat());
        int a =10;
    }

}
