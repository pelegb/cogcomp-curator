package CuratorToCAF;


import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


import com.sri.faust.model.DocAnnotations;
import com.sri.faust.model.DocumentExcerpt;
import com.sri.faust.model.FaustEntityMention;
import com.sri.faust.model.FaustRelationMention;

import edu.illinois.cs.cogcomp.thrift.base.Forest;
import edu.illinois.cs.cogcomp.thrift.base.Node;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.thrift.base.Tree;
import edu.illinois.cs.cogcomp.thrift.curator.Record;

public class CuratorToCAF {
	Forest forest = null;
	private DocAnnotations dAnn = null;
	private int refId = 0;
	private HashMap<Integer, String> pairing;
	private String sourceId = "";
	private String docId = "";
	
	/**
	 * Takes a Curator record and converts it to a CAF file.
	 * @param record a Curator CAF record
	 */
	public CuratorToCAF(Record record){
        
		
        forest = record.parseViews.get("CAF");
        if(forest == null){
        	System.err.println("ParseView didn't contain a forest with that name");
        	return;
        }
        //get additional values we stored in the forest "source." These are docId,runId,sourceId
        String[] addValues = forest.getSource().split(",");
        
        
        docId = addValues[0];
        sourceId = addValues[2];
        dAnn = new DocAnnotations(docId);
		dAnn.setText(record.getRawText());
		dAnn.setRunId(Integer.parseInt(addValues[1]));

        pairing = new HashMap<Integer, String>();
        fillDocAnnotation();
	}
	/**
	 * Sets up the task of filling all entities, then all relations
	 */
	private void fillDocAnnotation(){
		Iterator<Tree> it = forest.getTreesIterator();
		boolean secondPass = false;
		//we need to go through this twice, the first time getting all the Entities
		//This ensure that our refId's will line up
		Tree temp = null;
		for(int i = 0; i < 2; i++){
			while(it.hasNext()){
				temp = it.next();
				//If it's the first time and it's an entity
				if(!secondPass && temp.source != null && temp.source.equals("Entity")){
					addEntityTree(temp);
				}
				else if(temp.source == null || !temp.source.equals("Entity")){
					addRelationTree(temp);
				}
			}
			secondPass = true;
		}
	}
	/**
	 * Adds an entity tree (entity and all coreference) to the CAF file
	 * @param tree
	 */
	private void addEntityTree(Tree tree){
		Iterator<Node> it = tree.getNodesIterator();
		String referenceId = refId + "";
		//All these entities get the same referenceId
		while(it.hasNext()){
			Node entity = it.next();
			dAnn.addEntityMention(mentionFromNode(entity, referenceId));
			pairing.put(cantorPairing(entity.span.start, entity.span.ending), referenceId);
		}
		refId++;
	}
	/**
	 * Adds a relation tree (root span, primary trigger, and all arguments) to the CAF file
	 * @param tree
	 */
	private void addRelationTree(Tree tree){
		//The first node is the entire span
		//The second node is the primary trigger
		//The reset are arguments
		List<Node> nodes = tree.getNodes();
		int size = tree.getNodesSize();
		if(size < 2){
			System.out.println("Error: This relation tree didn't have a provenance or primary trigger");
			return;
		}
		//Get the entire span as a provenance
		Node entireSpanNode = nodes.get(0);
		DocumentExcerpt provenance = new DocumentExcerpt(docId, entireSpanNode.getSpan().getLabel(), entireSpanNode.getSpan().getStart(), entireSpanNode.getSpan().getEnding());
		//Get the primaryTrigger mention and override type of the primaryTrigger from "Primary Trigger" to the event name
		Node primaryTriggerNode = nodes.get(1);
		FaustEntityMention primaryTrigger = mentionFromNode(primaryTriggerNode, "", entireSpanNode.getLabel());
		
		
		
		FaustRelationMention relationMention = null;
		//FaustRelationMention(primaryTrigger, predicate, argument)
		for(int i = 2; i < size; i++){
			FaustEntityMention argument = mentionFromNode(nodes.get(i), "");
			relationMention = new FaustRelationMention(primaryTrigger, nodes.get(i).getLabel(), argument);
			relationMention.setProvenance(provenance);
			relationMention.setProbability(1.0);
			relationMention.setWeight(1.0);
			relationMention.setSourceId(sourceId);
			dAnn.addRelationMention(relationMention);
		}
	}
	
	/**
	 * Given two integers, maps to a unique single integer
	 * @param a
	 * @param b
	 * @return
	 */
	private static int cantorPairing(int a, int b){
		//returns a unique number for the pair
		int sum = a + b;
		return (sum * (sum+1))/2 + b;
	}
	/**
	 * Calculates a referenceId by checking to see if it should create a new one, or use a stored one based on the span start and stop
	 * @param node
	 * @return
	 */
	private String refWithCantor(Node node){
		//Cantor then check hash table
		String referenceId;
		int cantor = cantorPairing(node.getSpan().getStart(), node.getSpan().getEnding());
		if(pairing.containsKey(cantor)){
			referenceId = pairing.get(cantor);
		}
		else{
			referenceId = refId + "";
			pairing.put(cantor, referenceId);
			refId++;
		}
		return referenceId;
	}

	private FaustEntityMention mentionFromNode(Node node, String referenceId){
		return mentionFromNode(node, referenceId, "");
	}
	/**
	 * Converts a record node into a CAF mention
	 * @param node
	 * @param referenceId
	 * @param overrideTypeWith
	 * @return
	 */
	private FaustEntityMention mentionFromNode(Node node, String referenceId, String overrideTypeWith){
		if(referenceId.equals("")){
			referenceId = refWithCantor(node);
		}
		if(overrideTypeWith.equals("")){
			//by default, get the type we encoded into the label
			overrideTypeWith = node.getLabel();
		}
		Span span = node.getSpan();
		int start = span.getStart();
		int end = span.getEnding();
		DocumentExcerpt provenance = new DocumentExcerpt(docId, span.getLabel(), start, end);
		FaustEntityMention mention = new FaustEntityMention(overrideTypeWith, referenceId, provenance, sourceId);
		mention.setProbability(1.0);
		mention.setWeight(1.0);	
		return mention;
	}
	
	/**
	 * Returns the DocAnnotations that was created in the constructor
	 * @return
	 */
	public DocAnnotations getDocAnnotations(){
		return dAnn;
	}
	/**
	 * Returns the string representation of the json file for the created DocAnnotations
	 * @return
	 */
	public String getDocAnnotationsJson(){
		try {
			return dAnn.toJson();
		} catch (Exception e) {
			return "Error";
		}
	}

}
