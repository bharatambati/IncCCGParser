README

This package consists of transition based Incremental CCG parsers.

Parsing Algorithms:
-- Shift Reduce CCG parsing ("NonInc") [1]
-- Revealing based Incremental parser ("RevInc") [2]

Learning Algorithms:
-- Structured Perceptron [1]
-- Neural Networks [3,5]

Languages:
-- English [1,2,5]

Structured Perceptron:
======================
java -Xms12g -cp lib/*:IncCCGParser.jar ilcc.ccgparser.test.IncParserTest -trainCoNLL <training-conll-file> -trainAuto <training-auto-file> -trainParg <training-parg-file> -testCoNLL <testing-conll-file> -testAuto <testing-auto-file> -testParg <testing-parg-file> -outAuto <output-auto-file> -outParg <output-parg-file> -model <model-file> -beam <beam-size> -isTrain true -debug <0/1/2> -early <true/false> -iters <#iterations> -algo <"NonInc"/"RevInc"> -lookAhead <true/false>

During testing remove training file options "-trainCoNLL,-trainAuto,-trainParg"

Commands to Replicate:
======================
java -Xms12g -cp lib/*:IncCCGParser.jar ilcc.ccgparser.test.IncParserTest -testCoNLL $data/test.accg.conll -testAuto $data/test.gccg.auto -testParg $data/test.gccg.jparg -outAuto $outpath/test.per.ginc.auto -outParg $outpath/test.per.ginc.deps -model $models/per.ginc.model.txt.gz -algo "RevInc" -beam 1 -lookAhead true

java -Xms12g -cp lib/*:IncCCGParser.jar ilcc.ccgparser.test.IncParserTest -testCoNLL $data/test.accg.conll -testAuto $data/test.gccg.auto -testParg $data/test.gccg.jparg -outAuto $outpath/test.per.b16inc.auto -outParg $outpath/test.per.b16inc.deps -model $models/per.b16inc.model.txt.gz -algo "RevInc" -beam 16 -lookAhead true


java -Xms12g -cp lib/*:IncCCGParser.jar ilcc.ccgparser.test.IncParserTest -testCoNLL $data/test.innccg.conll -testAuto $data/test.gccg.auto -testParg $data/test.gccg.jparg -outAuto $outpath/test.per.gincin.auto -outParg $outpath/test.per.gincin.deps -model $models/per.gincin.model.txt.gz -algo "RevInc" -beam 1 -lookAhead false

java -Xms12g -cp lib/*:IncCCGParser.jar ilcc.ccgparser.test.IncParserTest -testCoNLL $data/test.innccg.conll -testAuto $data/test.gccg.auto -testParg $data/test.gccg.jparg -outAuto $outpath/test.per.b16incin.auto -outParg $outpath/test.per.b16incin.deps -model $models/per.b16incin.model.txt.gz -algo "RevInc" -beam 16 -lookAhead false


For Psycholinguistic Experiments:
=================================

Oracle:
=======
Command to Convert CCGbank derivations into Incremental derivations:
====================================================================
java -cp lib/*:IncCCGParser.jar ilcc.ccgparser.test.IncGetGoldDerivation -trainAuto $eeg/oracle/wsj.eeg.auto -trainCoNLL $eeg/oracle/wsj.eeg.conll -outAuto $eeg/oracle/wsj.eeg.out.auto

Command to extract trace of the parser with probabilities:
==========================================================
java -cp lib/*:IncCCGParser.jar ilcc.ccgparser.test.IncExtractProb -testAuto $eeg/oracle/wsj.eeg.out.auto -testCoNLL $eeg/oracle/wsj.eeg.inc.conll -outFile $eeg/oracle/wsj.eeg.out.txt -model $models/nn.gincin.model.txt.gz


Related Papers:
===============
[1] Bharat Ram Ambati, Tejaswini Deoskar, Mark Johnson, and Mark Steedman. 2015. An Incremental Algorithm for Transition-based CCG Parsing. In Proceedings of the 2015 Conference of the North American Chapter of the Association for Computational Linguistics: Human Language Technologies, pages 53–63, Denver, Colorado, May–June.

[2] Yue Zhang and Stephen Clark. 2011. Shift-Reduce CCG Parsing. In Proceedings of the 49th Annual Meeting of the Association for Computational Linguistics: Human Language Technologies, pages 683–692, Portland, Oregon, USA, June.

[3] Danqi Chen and Christopher Manning. 2014. A fast and accurate dependency parser using neural networks. In Proceedings of the 2014 Conference on Empirical
Methods in Natural Language Processing (EMNLP), pages 740–750, Doha, Qatar, October.

[4] David Weiss, Chris Alberti, Michael Collins, and Slav Petrov. 2015. Structured training for neural network transition-based parsing. In Proceedings of the 53rd Annual Meeting of the Association for Computational Linguistics and the 7th International Joint Conference on Natural Language Processing (Volume 1: Long Papers), pages 323–333, Beijing, China, July. Association for Computational Linguistics

[5] Bharat Ram Ambati, Tejaswini Deoskar and Mark Steedman. (2016). Shift Reduce CCG Parsing using Neural Network Models. In Proceedings of the 2015 Conference of the North American Chapter of the Association for Computational Linguistics: Human Language Technologies, San Diego, California, June.

[6] Bharat Ram Ambati. (2016). Transition-based Combinatory Categorial Grammar parsing for English and Hindi. PhD thesis, University of Edinburgh, UK.
