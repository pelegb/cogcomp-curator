package CAFToCurator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;

import com.sri.faust.model.DocAnnotations;
import com.sri.faust.model.DocumentExcerpt;
import com.sri.faust.model.FaustEntityMention;
import com.sri.faust.model.FaustRelationMention;

import edu.illinois.cs.cogcomp.thrift.base.Forest;
import edu.illinois.cs.cogcomp.thrift.base.Node;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.thrift.base.Tree;
import edu.illinois.cs.cogcomp.thrift.curator.Record;

public class CAFtoCurator {
	private Record record = null;
	private DocAnnotations dAnn = null;
	Forest relationForest = null;
	boolean setSourceId = false;
	
	/**
	 * Transforms a CAF file to a Curator record. This Constructor takes a String (file path).
	 * @param fileLocation path to the file
	 */
	public CAFtoCurator(String fileLocation){
		
			try {
				dAnn = DocAnnotations.fromJson(new File(fileLocation));
			} catch (JsonParseException e) {
				System.out.println("Json parse problem");
				e.printStackTrace();
				return;
			} catch (JsonMappingException e) {
				System.out.println("Json mapping problem");
				e.printStackTrace();
				return;
			} catch (IOException e) {
				System.out.println("IO problem");
				e.printStackTrace();
				return;
			}
		
		makeRecord();
		
	}
	/**
	 * Transforms a CAF file to a Curator record. This Constructor takes a File.
	 * @param cafFile 
	 */
	public CAFtoCurator(File cafFile){

		try {
			dAnn = DocAnnotations.fromJson(cafFile);
		} catch (Exception e) {
			System.out.println("Error converting from File. Aborting");
			e.printStackTrace();
			return;
		}
		makeRecord();
	}
	/**
	 * Transforms a CAF file to a Curator record. This Constructor takes an InputStream.
	 * @param cafInputStream
	 */
	public CAFtoCurator(InputStream cafInputStream){
		try {
			dAnn = DocAnnotations.fromJson(cafInputStream);
		} catch (Exception e) {
			System.out.println("Error converting from InputStream. Aborting");
			e.printStackTrace();
			return;
		}
		makeRecord();
	}
	/**
	 * Transforms a CAF file to a Curator record. This Constructor takes a Reader.
	 * @param cafReader
	 */
	public CAFtoCurator(Reader cafReader){
		try {
			dAnn = DocAnnotations.fromJson(cafReader);
		} catch (Exception e) {
			System.out.println("Error converting from Reader. Aborting");
			e.printStackTrace();
			return;
		}
		makeRecord();
	}

	/**
	 * The function that begins the process of converting CAF to a record. All Constructors call this.
	 * It does the global initialization and calls the work functions.
	 */
	private void makeRecord(){
		record = new Record();
		String text = dAnn.getText();
		record.setRawText(text);

		record.setIdentifier(dAnn.getDocId() +","+ dAnn.getDocId() +","+ hashString(text)); //Use a hash of the text so we cann allow changes.
		relationForest = new Forest();
		
		//add information we'd lose otherwise to the source of the forest. We'll add sourceId to this on the first argument
		//Ignore task and processor for now, because I don't have any experience seeing those
		//relationForest.setSource(dAnn.getDocId()+","+dAnn.getRunId()+","+dAnn.getTask()+","+dAnn.getProcessor().getProcessorName());
		relationForest.setSource(dAnn.getDocId()+","+dAnn.getRunId());
		getEntities();
		getRelations();
		
		


		HashMap<String, Forest> forestSet = record.getParseViews();
		if ( null = forestSet )
		    forestSet = new HashMap<String, Forest>();
		forestSet.put("CAF", relationForest);
		record.setParseViews(forestSet); 		
	}
	
	/**
	 * Fills the forest with trees from all the relations from the CAF file.
	 */
	private void getRelations(){
		List<FaustRelationMention> r = dAnn.getRelationMentions();
		if(r == null){
			//System.out.println("There were no relations!");
			return;
		}
		HashMap<String, Tree> refMap = new HashMap<String, Tree>();
		int size = r.size();
		
		FaustEntityMention subject;
		FaustEntityMention object;
		String refId;
		Tree currentTree;
		//Make a tree for each relation
		//The entire span of the relation will be the root node
		//Each child node will be an argument (including PrimaryTrigger)
		
		for(int i = 0; i < size; i++){
			subject = r.get(i).getSubjectEntity();
			refId = subject.getReferenceId(); 
			
			
			if(!refMap.containsKey(refId)){ //We've never seen this subject before
				Tree tree = new Tree();				
				String pred = "Unknown";
				Iterator<String> iter = subject.getTypes().iterator();
				if(iter.hasNext()){
					pred = iter.next();
					//Clean out the #
					if(pred.contains("#")){
						pred = pred.substring(pred.indexOf("#") + 1, pred.length());
					}
				
				}
				//Make a node to be the root for the tree
				Node root = new Node();
				//Set label text to the name of the Event
				root.setLabel(pred);
				
				Span rootSpan = new Span();
				DocumentExcerpt temp = r.get(i).getProvenance();
				if(temp == null){
					//System.out.println("There was no provenance!");
					rootSpan.setStart(-1);
					rootSpan.setEnding(-1);
					rootSpan.setLabel("");
					
				}
				else{
					rootSpan.setStart(temp.getBeginningOffset());
					rootSpan.setEnding(temp.getEndingOffset());
					rootSpan.setLabel(temp.getExcerptString());

				}
				root.setSpan(rootSpan);
				//now that the event category is set up, add the PrimaryTrigger
				Node subjectNode = new Node();
				subjectNode.setLabel("PrimaryTrigger");
				
				Span subjectSpan = new Span();
				subjectSpan.setLabel(subject.getProvenance().getExcerptString());
				subjectSpan.setStart(subject.getProvenance().getBeginningOffset());
				subjectSpan.setEnding(subject.getProvenance().getEndingOffset());
				subjectNode.setSpan(subjectSpan);
				
				
				
				//Add these two nodes to the tree
					//We'll wait to associate the root node with the children nodes until their all added
				tree.addToNodes(root);
				tree.addToNodes(subjectNode);
				tree.top = 0;
				
				//Since it is new, add it to the hashmap
				refMap.put(refId, tree);
				currentTree = tree;
				
				
			}
			else currentTree = refMap.get(refId);
			object = r.get(i).getObjectEntity();
			//make a new argument and add it to the tree
			if(object != null)
				addArgument(currentTree, object, r.get(i).getPredicate(), false);
			
		}
		// Now that we've filled the trees, we need to associate the root of the trees with the child nodes
		//Since every other kid in the tree is under the root, add them all
		for(Tree temp : refMap.values()){
			
			Map<Integer, String> kids = new HashMap<Integer, String>();
			for(int i = 1; i < temp.nodes.size(); i++){
				kids.put(i, "");
			}
			temp.nodes.get(0).setChildren(kids);
			//Then we'll fill the relationForest with the trees
			relationForest.addToTrees(temp);
		}
		
		
	}
	/**
	 * Fills the forest with trees from all the Entities from the CAF file.
	 */
	private void getEntities(){
		List<FaustEntityMention> e = dAnn.getEntityMentions();
		HashMap<String, Tree> refMap = new HashMap<String, Tree>();
		int size = e.size();

		String refId;
		Tree currentTree;
		for(int i = 0; i < size; i++){
			refId = e.get(i).getReferenceId(); 
			if(!refMap.containsKey(refId)){ //We've never seen this entity before
				Tree tree = new Tree();
				//Using source incorrectly as a place to distinguish between entities and events
				tree.setSource("Entity");
				
				//make a new tree and associate it with this refId
				refMap.put(refId, tree);
				currentTree = tree;
			}
			else{
				//get the tree associated with your refId
				currentTree = refMap.get(refId);
				
			}
			//make a new argument and add it to category(location)
			addArgument(currentTree, e.get(i), "", true);
			
		}
		// Now that we've filled the trees, we need to associate the root of the trees with the child nodes
		//Since every other kid in the tree is under the root, add them all
		for(Tree temp : refMap.values()){
			
			Map<Integer, String> kids = new HashMap<Integer, String>();
			for(int i = 1; i < temp.nodes.size(); i++){
				kids.put(i, "");
			}
			temp.nodes.get(0).setChildren(kids);
			//Then we'll fill the relationForest with the trees
			relationForest.addToTrees(temp);
		}
	
	}
	/**
	 * 
	 * @param currentTree the tree to add to
	 * @param arg the FaustEntityMention to convert to our Record format and add
	 * @param argName The name of the argument
	 * @param entity Is it any entity? (or part of a relation)
	 */
	private void addArgument(Tree currentTree, FaustEntityMention arg, String argName, boolean entity){
		Node objectNode = new Node();
		Span objectSpan = new Span();
		
		//set the sourceId if we havne't already
		if(!setSourceId){
			relationForest.setSource(relationForest.getSource() + "," + arg.getSourceId());
			setSourceId = true;
		}
		
		if(!entity){
			
			if(argName.contains("#")){
				argName = argName.substring(argName.indexOf("#") + 1, argName.length());

			}
			objectNode.setLabel(argName);
		}
		else{
			//This (theoretically)adds the name to be "Name - Span Text" as it was in EAT+
			Iterator<String> iter = arg.getTypes().iterator();
			String type = "";
			String temp;
			while(iter.hasNext()){
				temp = iter.next();
				if(temp.contains("#")){
					temp = temp.substring(temp.indexOf("#") + 1, temp.length());

				}
				type += temp + ", ";
			}
			type = type.substring(0, type.length() -2); //Remove last comma and space

			
			objectNode.setLabel(type);
		}
		
		objectSpan.setStart(arg.getProvenance().getBeginningOffset());
		objectSpan.setEnding(arg.getProvenance().getEndingOffset());
		objectSpan.setLabel(arg.getProvenance().getExcerptString());
		
		objectNode.setSpan(objectSpan);
		currentTree.addToNodes(objectNode);
	}
	
	/**
	 * Takes a string, hashes it with MD5, then returns the new string
	 * @param toHash The string to hash
	 * @return the hashed string
	 */
	private String hashString(String toHash){
		//http://www.spiration.co.uk/post/1199/Java-md5-example-with-MessageDigest
		//There will be some problems with leading zeros, but for this implementation it doesn't matter
		byte[] defaultBytes = toHash.getBytes();
		try{
			MessageDigest algorithm = MessageDigest.getInstance("MD5");
			algorithm.reset();
			algorithm.update(defaultBytes);
			byte messageDigest[] = algorithm.digest();
		            
			StringBuffer hexString = new StringBuffer();
			for (int i=0;i<messageDigest.length;i++) {
				hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
			}

			return hexString.toString();
		}catch(NoSuchAlgorithmException nsae){
		    return "Error";        
		}
	}
	
	/**
	 * Returns the Curator record that was created in the constructor.
	 * @return the Curator record that was created in the constructor.
	 */
	public Record getRecord(){
		
		return record;
		
	}
	/**
	 * Returns the json text of the CAF file you passed to the constructor.
	 * @return the json text of the CAF file you passed to the constructor.
	 */
	public String getOriginalCAFJson(){
		if(dAnn != null){
			try{
				return dAnn.toJson();
			}catch(Exception e){
				
				return "Failed to make into Json";
			}
		}
		else{
			return "CAF was null";
		}
	}
	
	
}
