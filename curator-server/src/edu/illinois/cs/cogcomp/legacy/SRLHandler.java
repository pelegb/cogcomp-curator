package edu.illinois.cs.cogcomp.legacy;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.thrift.TException;

import edu.illinois.cs.cogcomp.annotation.handler.IllinoisTokenizerHandler;
import edu.illinois.cs.cogcomp.thrift.curator.Record;
import edu.illinois.cs.cogcomp.thrift.base.AnnotationFailedException;
import edu.illinois.cs.cogcomp.thrift.base.Forest;
import edu.illinois.cs.cogcomp.thrift.base.Labeling;
import edu.illinois.cs.cogcomp.thrift.base.Node;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.thrift.base.Tree;
import edu.illinois.cs.cogcomp.thrift.parser.Parser;
import edu.illinois.cs.cogcomp.util.StringUtil;
import edu.illinois.cs.cogcomp.util.WhiteSpaceTokenizer;

/**
 * This code calls the SRL server creates the desired views.
 * 
 * The code is mainly due to Vivek Srikumar with modifications by James Clarke.
 */
public class SRLHandler implements Parser.Iface {

    private String host;
    private int port;
    private boolean debug = true;

    public SRLHandler(String host, int port) {
	this.host = host;
	this.port = port;
    }

    /**
     * Calls the SRL server and produces an SRL parse as Forest for the record.
     * 
     * @param record
     *            - the record to parse
     * @return SRL Parse
     * @throws UnknownHostException
     * @throws IOException
     * @throws TException
     * @throws AnnotationFailedException
     */
    private synchronized Forest performSRL(Record record)
	    throws UnknownHostException, IOException, TException,
	    AnnotationFailedException {

	CogcompClient client = new CogcompClient(host, port, true);
	String rawText = record.getRawText();
	List<Tree> trees = new ArrayList<Tree>();
	for (Span sentence : record.getLabelViews().get("sentences")
		.getLabels()) {
	    client.startSession();
	    client.getOutput("1 0\n", true);
	    String sentenceStr = StringUtil.spanToString(sentence, rawText)
		    .replaceAll("\\s+", " ");
	    List<String> columnFormat = client.getOutput(sentenceStr + "\n\n");
	    client.closeSession();
	    if (debug) {
		for (String line : columnFormat)
		    System.out.println(line);
	    }
	    trees.addAll(treesFromColumns(columnFormat, rawText, sentence));
	}
	Forest forest = new Forest();
	forest.setSource(getSourceIdentifier());
	forest.setTrees(trees);
	return forest;
    }

    /**
     * Creates a List<Tree> from SRL column format output.
     * 
     * @param columnFormat
     * @param sentence
     * @param source
     * @return
     * @throws IOException
     * @throws AnnotationFailedException
     */
    private List<Tree> treesFromColumns(List<String> columnFormat,
	    String rawText, Span sentence) throws IOException,
	    AnnotationFailedException {
	int numTokens = columnFormat.size();

	if (numTokens == 0) {
	    throw new AnnotationFailedException(
		    "Server returned an empty response for: " + rawText);
	}
	int location = 0;

	String[][] data = new String[numTokens][];
	int i = 0;

	List<Tree> trees = new ArrayList<Tree>();
	String sentenceText = StringUtil.spanToString(sentence, rawText);
	for (String line : columnFormat) {
	    String[] items = WhiteSpaceTokenizer.tokenizeToArray(line);
	    data[i++] = items;

	    String predicate = items[1];

	    if (!predicate.equals("-")) {
		Tree tree = new Tree();

		tree.setNodes(new ArrayList<Node>());

		Span span = new Span();
		int startend[] = StringUtil.findSpan(location, sentenceText,
			items[0]);
		span.setStart(startend[0] + sentence.getStart());
		span.setEnding(startend[1] + sentence.getStart());
		span.setAttributes(new HashMap<String, String>());
		span.getAttributes().put("predicate", predicate);
		location = startend[1];

		Node pred = new Node();

		pred.setSpan(span);
		pred.setLabel("Predicate");
		pred.setChildren(new HashMap<Integer, String>());

		tree.getNodes().add(pred);
		tree.setTop(tree.getNodes().size() - 1);
		trees.add(tree);
	    } else {
		int startend[] = StringUtil.findSpan(location, sentenceText,
			items[0]);
		if (startend[0] != -1)
		    location = startend[1];
	    }
	}

	Map<Integer, Boolean> dblquoteLocs = findDoubleQuotationLocations(sentenceText);

	Map<Integer, Integer> dblquoteLens = findDoubleQuotationLengths(
		sentenceText, dblquoteLocs);

	for (int columnId = 2; columnId < trees.size() + 2; columnId++) {
	    location = 0;
	    int start = -1;
	    int end = -1;
	    int[] startend = new int[] { 0, 0 };
	    String label = "";
	    int predNum = columnId - 2;
	    Tree tree = trees.get(predNum);
	    List<Node> nodes = tree.getNodes();
	    Node pred = nodes.get(tree.getTop());

	    for (int rowId = 0; rowId < numTokens; rowId++) {
		String word = data[rowId][0];
		startend = findStartEndForSpan(sentenceText, startend[1], word,
			dblquoteLocs, dblquoteLens);

		if (data[rowId][columnId].equals("*"))
		    continue;
		if (data[rowId][columnId].startsWith("(")) {
		    label = data[rowId][columnId].substring(1,
			    data[rowId][columnId].indexOf("*"));
		    start = startend[0];
		}

		if (data[rowId][columnId].endsWith(")")) {

		    String tmp = data[rowId][columnId]
			    .substring(data[rowId][columnId].indexOf("*") + 1);
		    tmp = tmp.substring(0, tmp.indexOf(")"));
		    if (!tmp.equals(label)) {
			throw new IOException("Invalid data format");
		    }
		    end = startend[1];

		    if (!label.equals("V")) {
			Span span = new Span();
			span.setStart(start + sentence.getStart());
			span.setEnding(end + sentence.getStart());

			Node arg = new Node();
			arg.setSpan(span);
			arg.setLabel("Argument");

			nodes.add(arg);
			pred.getChildren().put(nodes.size() - 1, label);
		    }
		    start = -1;
		    end = -1;
		    label = "";

		}
	    }
	}
	return trees;
    }

    public Forest parseRecord(Record record) throws TException,
	    AnnotationFailedException {
	try {
	    return performSRL(record);
	} catch (IOException e) {
	    throw new TException("IOException", e);
	}
    }

    public boolean ping() throws TException {
	return true;
    }

    public String getName() throws TException {
	return "Semantic Role Labeler (Legacy)";
    }

    public String getVersion() throws TException {
	return "0.1";
    }

    private Map<Integer, Integer> findDoubleQuotationLengths(
	    String sentenceText, Map<Integer, Boolean> dblquoteLocs) {

	Map<Integer, Integer> dblquoteLens = new HashMap<Integer, Integer>();
	for (int position : dblquoteLocs.keySet()) {
	    if (sentenceText.charAt(position) == '"')
		dblquoteLens.put(position, 1);
	    else
		dblquoteLens.put(position, 2);
	}

	return dblquoteLens;
    }

    private static Map<Integer, Boolean> findDoubleQuotationLocations(
	    String input) {
	Map<Integer, Boolean> quoteLocs = new TreeMap<Integer, Boolean>();
	if (input.contains("\"")) {
	    int from = 0;
	    int index;
	    int counter = 0;
	    boolean first = true;
	    while ((index = input.indexOf("\"", from)) != -1) {
		quoteLocs.put(index, first);
		counter++;
		from = index + 1;
		first = !first;
	    }
	}
	findPTBDoubleQuoteLocations(input, quoteLocs);

	return quoteLocs;
    }

    private static void findPTBDoubleQuoteLocations(String input,
	    Map<Integer, Boolean> quoteLocs) {
	if (input.contains("``") || input.contains("''")) {
	    int from = 0;
	    int index;
	    boolean first = true;

	    boolean done = false;
	    do {

		int indexOpen = input.indexOf("``", from);
		int indexClose = input.indexOf("''", from);

		if (indexOpen < 0 && indexClose < 0) {
		    done = true;
		} else {
		    if (indexOpen < 0)
			index = indexClose;
		    else if (indexClose < 0)
			index = indexOpen;
		    else
			index = Math.min(indexOpen, indexClose);

		    quoteLocs.put(index, first);
		    first = !first;
		    from = index + 1;
		}
	    } while (!done);

	}
    }

    private static int[] findStartEndForSpan(String input, int location,
	    String word, Map<Integer, Boolean> dblquoteLocs,
	    Map<Integer, Integer> dblquoteLens) {
	int[] startend = null;
	if (word.equals("``") || word.equals("''")) {
	    // double tick is a special case because it could have been a double
	    // quote before
	    // inputAsSrl is how SRL viewed the input (we replicate the
	    // important transforms
	    // srl makes, this is very fragile!)
	    StringBuffer inputAsSrl = new StringBuffer();
	    int translations = 0;
	    int previous = 0;
	    // subLocs will contian the locations in the srl string where double
	    // ticks should be double quote
	    List<Integer> subLocs = new ArrayList<Integer>();
	    for (Integer key : dblquoteLocs.keySet()) {
		inputAsSrl.append(input.substring(previous, key));
		String mark = dblquoteLocs.get(key) ? "``" : "''";
		inputAsSrl.append(mark);

		// Vivek: A hack on top of another one: This is to deal with the
		// cases when double quotes are written as `` or '' instead of "
		// in the raw text.
		previous = key + dblquoteLens.get(key);

		subLocs.add(key + translations);
		if (location > key) {
		    translations++;
		}
	    }
	    inputAsSrl.append(input.substring(previous));
	    startend = StringUtil.findSpan(location, inputAsSrl.toString(),
		    word);
	    if (subLocs.contains(startend[0])) {
		startend[1] = startend[1] - 1;
	    }
	    startend[0] = startend[0] - translations;
	    startend[1] = startend[1] - translations;
	} else {
	    startend = StringUtil.findSpan(location, input, word);
	}
	return startend;
    }

    public String getSourceIdentifier() throws TException {
	return "ccgsrl-" + getVersion();
    }

    public static void main(String[] args) throws Exception {
	String text = "I went home and partied.  Dan has a book which is blue.  The dog ate the cat quickly.";
	text = "He said, \"Listen to my story\". The message read \"you are in danger\".  He responded, ``I see no danger -- only flying pigs!''.";
	text = "Searle will give pharmacists brochures on the use of prescription drugs for distribution in their stores.";
	text = "\"Madonna laid the law down to me before we went out. [She said] I am not going to Disneyland, OK? That's out,\" Jackson said. \"I said, 'I didn't ask to go to Disneyland.' She said, 'We are going to the restaurant. And afterwards, we are going to a strip bar.'";
	text = "The cost analysis is in: the Senate Finance Committee's health care legislation, as amended during seven days of debate, would cost a projected $829 billion over 10 years, up from $774 billion, according to the Congressional Budget Office.";
	text = "The first thing that needs to happen is that Bettman and Goodenow (the players' association rep) need to be fired, deported, flogged, tarred and feathered and finally forced to pay for four tickets plus a program, four dogs, four beers, and two cokes and parking to a game at an NHL arena for every day of the next season so they understand what the fans go thru to see a hockey game";
	text = "Bob kicked the ball to Jack and he kicked it back.";
	text = "The game was a failure.";
	text = "`` This decision adds to the already widespread perception that in politically sensitive cases , the independence of the judiciary can no longer be guaranteed , \" the London-based group said in a statement .";

	text = "And we 'll raise it through existing shareholders '' as well as through junk bonds , said Anthony Simonds-Gooding , the private consortium 's chief executive . ";

	text = "The issue is one of a judge 's experience , his competence and his physical ability to serve on the bench , '' Justice Rubin said . ";
	Record record = new Record();
	record.setRawText(text);

	IllinoisTokenizerHandler tokenizer = new IllinoisTokenizerHandler();

	List<Labeling> labelRecord = tokenizer.labelRecord(record);

	System.out.println(labelRecord);

	record.setLabelViews(new HashMap<String, Labeling>());
	record.setParseViews(new HashMap<String, Forest>());

	record.getLabelViews().put("sentences", labelRecord.get(0));
	record.getLabelViews().put("tokens", labelRecord.get(1));

	System.out.println(record.getLabelViews().get("sentences"));

	Parser.Iface srl = new SRLHandler("smeagol.cs.uiuc.edu", 13710); // nom

	record.getParseViews().put("srl", srl.parseRecord(record));

	for (Tree tree : record.getParseViews().get("srl").getTrees()) {

	    List<Node> nodes = tree.getNodes();
	    Node predicate = nodes.get(tree.getTop());
	    System.out.println("(" + predicate.getSpan().getAttributes() + ": "
		    + StringUtil.spanToString(predicate.getSpan(), text));
	    for (Integer i : predicate.getChildren().keySet()) {
		Node arg = nodes.get(i);
//		System.out.println(arg.getSpan());
		System.out.println("  (" + predicate.getChildren().get(i)
			+ ": " + StringUtil.spanToString(arg.getSpan(), text)
			+ ")");
	    }
	    System.out.println(")");

	}
    }

}
