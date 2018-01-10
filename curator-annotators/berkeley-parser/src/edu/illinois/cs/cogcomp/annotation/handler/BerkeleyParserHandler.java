package edu.illinois.cs.cogcomp.annotation.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.thrift.TException;
import org.ietf.jgss.Oid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.berkeley.nlp.PCFGLA.CoarseToFineMaxRuleParser;
import edu.berkeley.nlp.PCFGLA.CoarseToFineNBestParser;
import edu.berkeley.nlp.PCFGLA.Grammar;
import edu.berkeley.nlp.PCFGLA.Lexicon;
import edu.berkeley.nlp.PCFGLA.MultiThreadedParserWrapper;
import edu.berkeley.nlp.PCFGLA.ParserData;
import edu.berkeley.nlp.util.Numberer;
import edu.berkeley.nlp.util.Pair;
import edu.illinois.cs.cogcomp.thrift.base.AnnotationFailedException;
import edu.illinois.cs.cogcomp.thrift.base.Forest;
import edu.illinois.cs.cogcomp.thrift.base.Labeling;
import edu.illinois.cs.cogcomp.thrift.base.Node;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.thrift.base.Tree;
import edu.illinois.cs.cogcomp.thrift.curator.Record;
import edu.illinois.cs.cogcomp.thrift.parser.Parser;

public class BerkeleyParserHandler implements Parser.Iface{
	
	private static Logger logger = LoggerFactory.getLogger(BerkeleyParserHandler.class);
	
	private final String tokensfield = "tokens";
	private final String sentencesfield = "sentences";
	private final String posfield = "pos";
	
	//REMOVE!!
	public String rawText;
	
	//configurations
	private Configuration config;
	private final String grammarFile;
	private final int nThreads;
	private final int kBest;
	private final boolean goldPOS;
	private final boolean DEBUG;
	private final boolean removeAt;
	
	private boolean isCuratorComponent = false;
	
	//parser
	private CoarseToFineMaxRuleParser parser = null;
	private MultiThreadedParserWrapper m_parser = null;
	
	//goldPOS, which is useless for this version, and seems useless as well in the berkeley parser; need further check
	private ArrayList<String> posTags = null;
	
	
	public BerkeleyParserHandler(String configFile){
		System.err.println( "## BerkeleyParserHandler( configfile ) -- constructor for BerkeleyParser as curator component..." );
        
		try {
			this.config = new PropertiesConfiguration(configFile);
			
		} catch (ConfigurationException e) {
			logger.error("Error reading configuration file. {}", configFile);
			System.exit(1);
		}
		
		grammarFile = config.getString("gr","grammars/eng_sm6.gr");
		nThreads = Integer.parseInt(config.getString("nthreads","1"));
		kBest = Integer.parseInt(config.getString("kbest","1"));
		goldPOS = Boolean.parseBoolean(config.getString("goldPOS", "false"));
		DEBUG = Boolean.parseBoolean(config.getString("DEBUG", "false"));
		removeAt = Boolean.parseBoolean(config.getString("remove@", "true"));
		
		isCuratorComponent = true;
		
		initializeParser();
	}
	
	
	private void initializeParser(){
		String inFileName = grammarFile;
		ParserData pData = ParserData.Load(inFileName);
		
		//unary penalty. what is this?
		double threshold = 1.0;
		
		if (pData==null) {
			logger.error("Failed to load grammar from file"+inFileName+".");
			System.exit(1);
		}
		
		Grammar grammar = pData.getGrammar();
		Lexicon lexicon = pData.getLexicon();
		Numberer.setNumberers(pData.getNumbs());
		if (kBest==1) parser = new CoarseToFineMaxRuleParser(grammar, lexicon, threshold,-1,false,false,false,false,false,true,true);
		else parser = new CoarseToFineNBestParser(grammar, lexicon, kBest,threshold,-1,false,false,false,false,false,false,true);
		
		parser.binarization = pData.getBinarization();
		
		if (nThreads > 1){
			logger.info("Parsing with " + nThreads + " threads in parallel.");
			System.out.println("Parsing with " + nThreads);
			m_parser = new MultiThreadedParserWrapper(parser, nThreads);
		}
		
		
	}
	
	
	
	private List<edu.berkeley.nlp.syntax.Tree<String>> singleThreadParse(List<String> sentence){
		List<edu.berkeley.nlp.syntax.Tree<String>> parsedTrees = null;
		
		if (kBest > 1){
			parsedTrees = parser.getKBestConstrainedParses(sentence, posTags, kBest);
			if (parsedTrees.size()==0) {
				parsedTrees.add(new edu.berkeley.nlp.syntax.Tree<String>("ROOT"));
			}
		} else {
			parsedTrees = new ArrayList<edu.berkeley.nlp.syntax.Tree<String>>();
			edu.berkeley.nlp.syntax.Tree<String> parsedTree = parser.getBestConstrainedParse(sentence,posTags,null);
			if (goldPOS && parsedTree.getChildren().isEmpty()){ // parse error when using goldPOS, try without
				if(DEBUG) System.err.println("ERROR using goldPOS, try without");
				parsedTree = parser.getBestConstrainedParse(sentence,null,null);
			}
			parsedTrees.add(parsedTree);

		}
		
		return parsedTrees;
	}
	
	//note that the multi-thread version does not support goldPOS
	private List<edu.berkeley.nlp.syntax.Tree<String>> multiThreadParse(List<String> sentence){
		ArrayList<edu.berkeley.nlp.syntax.Tree<String>> parsedTrees = new ArrayList<edu.berkeley.nlp.syntax.Tree<String>>();
		
		m_parser.parseThisSentence(sentence);
		while(!m_parser.isDone()) {
			while (m_parser.hasNext()){
				parsedTrees.addAll(m_parser.getNext());
			}
		}
		
		return parsedTrees;
	}
	
	
	
	
	//This function is written in a k-best way; however, k-best feature is not well supported by the server
	@Override
	public Forest parseRecord(Record record) throws AnnotationFailedException, TException {
		String rawText = record.getRawText();
		this.rawText = new String(rawText);
		

		List[] tokenized_result = getTokenizedInput(record);
		
		List<String> tokenText = tokenized_result[0];
		List<ArrayList<Span>> offsets = tokenized_result[1];
		
		/*
		for(String token : tokenText) System.out.println(token);
		for(ArrayList<Span> spans : offsets){
			for(Span span : spans){
				System.out.println(span.start + ":" + span.ending + ":" + span.label);
			}
		}
		*/		
		//read in POS tags
		if(goldPOS) getPOS(record);
		
//		List<edu.berkeley.nlp.syntax.Tree<String>> parsedTrees = null;
//		
//		for(String line : tokenText){
//			if(nThreads <= 1) {
//				if(parsedTrees == null) parsedTrees = singleThreadParse(Arrays.asList(line.split("\\s+")));
//				else parsedTrees.add(singleThreadParse(Arrays.asList(line.split("\\s+"))).get(0));
//			}
//			else{
//				if(parsedTrees == null) parsedTrees = multiThreadParse(Arrays.asList(line.split("\\s+")));
//				else parsedTrees.add(multiThreadParse(Arrays.asList(line.split("\\s+"))).get(0));
//			}
//		}
		
		ArrayList<List<edu.berkeley.nlp.syntax.Tree<String>>> parsedForests = new ArrayList<List<edu.berkeley.nlp.syntax.Tree<String>>>();
		for(int i=0; i<kBest; i++) parsedForests.add(new ArrayList<edu.berkeley.nlp.syntax.Tree<String>>());
		
		List<edu.berkeley.nlp.syntax.Tree<String>> parsedTrees = null;
		
		
		for(String line : tokenText){
			
			if(nThreads <= 1)
				parsedTrees = singleThreadParse(Arrays.asList(line.split("\\s+")));
			else
				parsedTrees = multiThreadParse(Arrays.asList(line.split("\\s+")));
				
			for(int i=0; i<kBest; i++){
				if(parsedTrees.size() <= i) parsedForests.get(i).add(null);
				else parsedForests.get(i).add(parsedTrees.get(i));
			}
		}
		
		ArrayList<Forest> forests = new ArrayList<Forest>();
		for(int i=0; i<kBest; i++){
			forests.add(ConvertTreeToForest(parsedForests.get(i), offsets, tokenText));
		}
		
		return forests.get(0);
	}
	
	
	
	private void getPOS(Record record){
		if(!record.getLabelViews().containsKey(posfield)){
			System.err.println("Processing without POS tags");
			return;
		}
		List<Span> tags = record.getLabelViews().get(posfield).getLabels();
		for(Span tag : tags){
// 			System.err.println("POS labeling : " + tag.label);
			if(posTags == null) posTags = new ArrayList<String>();
			posTags.add(tag.label);
		}
	}
	

	private Forest ConvertTreeToForest(List<edu.berkeley.nlp.syntax.Tree<String>> their_trees, 
			List<ArrayList<Span>> offsets, List<String> tokenText) throws TException{
		
		Forest forest = new Forest();
		forest.setSource(getSourceIdentifier());
		
		System.out.println("their_tree_size: " + their_trees.size());
		for(int i=0; i<their_trees.size(); i++){
			ArrayList<Span> offset = offsets.get(i);
			
			Tree tree = new Tree();
			Node root = generateNode(their_trees.get(i), tree, tokenText.get(i), offset.iterator());
			tree.getNodes().add(root);
			tree.setTop(tree.getNodes().size() - 1);
			if(!forest.isSetTrees()){
				forest.setTrees(new ArrayList<Tree>());
			}
			//we don't have rawtext here; printTree for manual testing
			//printTree(root, tree, "@you I   love   you!");
			forest.getTrees().add(tree);
		}
		
		return forest;
	}
	
	
	
	private Node generateNode(edu.berkeley.nlp.syntax.Tree<String> their_tree, 
			Tree tree, String token_text, Iterator<Span> offset_iter){
		
		if(!tree.isSetNodes()) tree.setNodes(new ArrayList<Node>());
		
		List<Node> nodes = tree.getNodes();
		Node node = new Node();
		
		int start  = Integer.MAX_VALUE;
		int end = Integer.MIN_VALUE;

		if (their_tree.getLabel() != null){
			node.setLabel(their_tree.getLabel());
		}
		if (!their_tree.isLeaf()) {
			for(edu.berkeley.nlp.syntax.Tree<String> their_child : their_tree.getChildren()){
				if(!node.isSetChildren()) node.setChildren(new TreeMap<Integer, String>());
				
				Node child = generateNode(their_child, tree, token_text, offset_iter);
				
				
				//remove non-leaf children with label containing "@"
				if(removeAt && child.label.charAt(0) == 64 && child.children!=null){
					if(DEBUG) System.err.println("Removing " + child.label);
					for(int i : child.children.keySet()){
						node.getChildren().put(i, child.children.get(i));
					}
				} //only add child if it is not a leaf: remove duplicated labels of leaves
				else if(child.children != null){
					nodes.add(child);
					node.getChildren().put(nodes.size()-1, "");
				}
				
				
				
				int child_start = child.getSpan().start;
				int child_end = child.getSpan().ending;
				
				if(child_start < start) start = child_start;
				if(child_end > end) end = child_end;
				
			}
		}
		else{
			if(their_tree.getSpanMap().size() > 1) System.err.println("2 on leaf");
			Pair<Integer, Integer> pair = their_tree.getSpanMap().keySet().iterator().next();
			
			String tree_string = token_text.substring(pair.getFirst(), pair.getSecond());
			Span tree_span = offset_iter.next();
			start = tree_span.start;
			end = tree_span.ending;
		}
		
		//print the node
		if(DEBUG){
			System.out.println("Printing Node:");
			System.out.println(their_tree.getLabel());
			System.out.println(rawText.substring(start, end));
			System.out.println(start + ":" + end);
		}
		
		
		Span span = new Span();
		span.setStart(start);
		span.setEnding(end);
		node.setSpan(span);
		
		return node;
	}
	
	
	public void printTree(Node node, Tree tree, String rawText){
		
		List<Node> nodes = tree.getNodes();
		System.out.println(node.label + ":" + rawText.substring(node.getSpan().start, node.getSpan().ending));
		if(node.children != null){
			for(int i : node.children.keySet()) printTree(nodes.get(i), tree, rawText);
		}
	}
	
	
	private List[] getTokenizedInput(Record record){
		
		ArrayList[] results = new ArrayList[2];	// 0 for string, 1 for offsets
		
		ArrayList<String> tokenText = new ArrayList<String>();
		ArrayList<ArrayList<Span>> offsets = new ArrayList<ArrayList<Span>>();
		
		results[0] = tokenText;
		results[1] = offsets;
		
		String rawText = record.getRawText();
		
		for (Span sentence : record.getLabelViews().get(sentencesfield).getLabels()) {

			//now we must create the input to the parser
			String tokenSent = "";
			ArrayList<Span> offset = new ArrayList<Span>();
			offsets.add(offset);
			
			for (Span t : record.getLabelViews().get(tokensfield).getLabels()) {
				//find tokens that fall within the current sentence.
				int start = t.getStart();
				int end = t.getEnding();
				
				if (start >= sentence.getStart() && end <= sentence.getEnding()) {
					tokenSent += (rawText.substring(start, end) + " ");
					offset.add(t);
				}
			}
			if(tokenSent.length() >= 1){
				tokenSent = tokenSent.substring(0, tokenSent.length() -1);
				tokenText.add(new String(tokenSent));
			}
			
		}
		
		return results;
	}
	
	
	@Override
	public boolean ping() throws TException {
		return true;
	}

	@Override
	public String getName() throws TException {
		return "berkeley";
	}

	@Override
	public String getVersion() throws TException {
		return "0.1"; 
	}

	@Override
	public String getSourceIdentifier() throws TException {
		return getName() + "-" + getVersion();
	}


}
