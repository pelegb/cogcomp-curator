import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.Scanner;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import edu.illinois.cs.cogcomp.thrift.base.AnnotationFailedException;
import edu.illinois.cs.cogcomp.thrift.base.Node;
import edu.illinois.cs.cogcomp.thrift.base.ServiceUnavailableException;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.thrift.base.Tree;
import edu.illinois.cs.cogcomp.thrift.curator.Curator;
import edu.illinois.cs.cogcomp.thrift.curator.Record;

public class CuratorClient {
	
	private static String recordContents(Record record) {
		StringBuffer result = new StringBuffer();
		result.append("Annotations present in the record:\n");
		result.append("- rawText: ");
		result.append(record.isSetRawText() ? "Yes" : "No");
		result.append("\nThe following Label Views: ");
		for (String key : record.getLabelViews().keySet()) {
			result.append(key);
			result.append(" ");
		}
		result.append("\n");
		result.append("The following Cluster Views: ");
		for (String key : record.getClusterViews().keySet()) {
			result.append(key);
			result.append(" ");
		}
		result.append("\n");
		result.append("The following Parse Views: ");
		for (String key : record.getParseViews().keySet()) {
			result.append(key);
			result.append(" ");
		}
		result.append("\n");
		result.append("The following general Views: ");
		for (String key : record.getViews().keySet()) {
			result.append(key);
			result.append(" ");
		}
		result.append("\n");
		return result.toString();
	}
	
    public static void main(String[] args) throws AnnotationFailedException, FileNotFoundException {

	    if ( args.length != 3 ) 
		{
		    System.err.println( "Usage: CuratorClient curatorHost curatorPort textFile" );
		    System.exit( -1 );
		}

	    String host = args[0];
	    int port  = Integer.parseInt( args[1] );
	    String fileName = args[2];

	    StringBuilder textBldr = new StringBuilder();
	    String NL = System.getProperty("line.separator");
    
	    
	    Scanner scanner = new Scanner(new FileInputStream(fileName) );
	    try {
		while (scanner.hasNextLine()){
		    textBldr.append(scanner.nextLine() + NL);
		}
	    }
	    finally{
		scanner.close();
	    }


// 		String text = "With less than 11 weeks to go to the final round of climate talks in "
// 				+ "Copenhagen, the UN chief, Ban Ki-Moon did not bother to hide his frustration "
// 				+ "in his opening remarks. \"The world's glaciers are now melting faster than "
// 				+ "human progress to protect them -- or us,\" he said. Others shared his gloom. "
// 				+ "\"Today we are on a path to failure,\" said France's Nicolas Sarkozy.";


	    String text = textBldr.toString();

	    System.err.println( "## read in text: " + text );

		//First we need a transport
		TTransport transport = new TSocket(host, port );
		//we are going to use a non-blocking server so need framed transport
		transport = new TFramedTransport(transport);
		//Now define a protocol which will use the transport
		TProtocol protocol = new TBinaryProtocol(transport);
		//make the client
		Curator.Client client = new Curator.Client(protocol);
		
		System.out.println("We are going to be calling the Curator with the following text:\n");
		System.out.println(text);
		
		System.out.println("\n\nWe are going to inspect the Curator for the available annotations:\n");
		
		Map<String, String> avail = null;
		try {
			transport.open();
			avail = client.describeAnnotations();
			transport.close();
		} catch (TException e1) {
			e1.printStackTrace();
		}

		for (String key : avail.keySet()) {
			System.out.println("\t"+key + " provided by " + avail.get(key));
		}

		System.out.println();
		
        System.out.println("First we'll get the named entities in the text.");
        System.out.print("Calling curator.provide(\"ner\", text, false)... ");
        Record record = null;

	boolean forceUpdate = true;
         try {
             transport.open();
             //call Curator
             record = client.provide("ner", text, forceUpdate);
             transport.close();
         } catch (ServiceUnavailableException e) {
         	if (transport.isOpen())
         		transport.close();
             System.out.println("ner annotations are not available");
             System.out.println(e.getReason());
             
         } catch (TException e) {
         	if (transport.isOpen())
           		transport.close();
               e.printStackTrace();
           }
   		System.out.println("done.\n");
   		System.out.println();
           if (avail.containsKey("ner")) {
           	System.out.println(recordContents(record));
           	System.out.println();

             System.out.println("Named Entities\n---------\n");
             for (Span span : record.getLabelViews().get("ner").getLabels()) {
                 System.out.println(span.getLabel() + " : "
                 + record.getRawText().substring(span.getStart(), span.getEnding()));
             }
             System.out.println();
             System.out.println();
             System.out.println("The raw data structure containing the NEs looks like this:");
             System.out.println(record.getLabelViews().get("ner"));
         }
         System.out.println();


        System.out.println("Next we'll call the extended NER (more entity types)...");
        System.out.print("Calling curator.provide(\"ner-ext\", text, false)... ");
        try {
            transport.open();
            //call Curator
            record = client.provide("ner-ext", text, forceUpdate);
            transport.close();
        } catch (ServiceUnavailableException e) {
        	if (transport.isOpen())
        		transport.close();
            System.out.println("ner-ext annotations are not available");
            System.out.println(e.getReason());
             
        } catch (TException e) {
        	if (transport.isOpen())
        		transport.close();
            e.printStackTrace();
        }
		System.out.println("done.\n");
		System.out.println();
        if (avail.containsKey("ner-ext")) {
        	System.out.println(recordContents(record));
        	System.out.println();

            System.out.println("Extended Named Entities\n---------\n");
            for (Span span : record.getLabelViews().get("ner-ext").getLabels()) {
                System.out.println(span.getLabel() + " : "
                + record.getRawText().substring(span.getStart(), span.getEnding()));
            }
            System.out.println();
            System.out.println();
            System.out.println("The raw data structure containing the NEs looks like this:");
            System.out.println(record.getLabelViews().get("ner"));
        }
        System.out.println();


		System.out.println("Next we will get a chunking (shallow parse) of the text.");
		System.out.print("Calling curator.provide(\"chunk\", text, forceUpdate = '" + ( forceUpdate ? "TRUE" : "FALSE" ) + "')... ");
		try {
			transport.open();
			//call Curator
			record = client.provide("chunk", text, forceUpdate);
			transport.close();
		} catch (ServiceUnavailableException e) {
			e.printStackTrace();
		} catch (TException e) {
			e.printStackTrace();
		}
		System.out.println("done.");
		System.out.println();
		System.out.println(recordContents(record));
		System.out.println();
		System.out.println("Notice that the record now contains chunk and sentences, tokens and pos fields.\n" +
				"This is because pos tags are required for chunking.  And tokenization is required by the pos tagger");
		System.out.println("\nSentences\n--------\n");
		for (Span span : record.getLabelViews().get("sentences").getLabels()) {
			System.out.println("# " +record.getRawText().substring(span.getStart(), span.getEnding()));
		}
		System.out.println("\nPOS Tags\n------\n");
		StringBuffer result = new StringBuffer();
		for (Span span : record.getLabelViews().get("pos").getLabels()) {
			result.append(record.getRawText().substring(span.getStart(), span.getEnding()) + "/" + span.getLabel());
			result.append(" ");
		}
		System.out.println(result.toString());
		System.out.println();
		System.out.println("Chunking\n---------\n");
		result = new StringBuffer();
		for (Span span : record.getLabelViews().get("chunk").getLabels()) {
			result.append("["+span.getLabel()+ " ");
			result.append(record.getRawText().substring(span.getStart(), span.getEnding()));
			result.append("] ");
		}
		System.out.println(result.toString());
		
		System.out.println("\n");
		System.out.println("Next we will get the stanford dependency annotations of the text.\n");
		System.out.print("Calling curator.provide(\"stanfordDep\", text, forceUpdate = '" + ( forceUpdate ? "TRUE" : "FALSE" ) + "')... ");

		try {
			transport.open();
			//call Curator
			record = client.provide("stanfordDep", text, forceUpdate);
			transport.close();
		} catch (ServiceUnavailableException e) {
			e.printStackTrace();
		} catch (TException e) {
			e.printStackTrace();
		}
		
		System.out.println();
		System.out.println("Stanford Dependencies\n------------------\n\n");
		for (Tree depTree : record.getParseViews().get("stanfordDep").getTrees()) {
			int top = depTree.getTop(); //this tells us where in nodes the head node is
			Stack<Integer> stack = new Stack<Integer>();
			stack.push(top);
			result = new StringBuffer();
			while (!stack.isEmpty()) {
				int headIndex = stack.pop();
				Node head = depTree.getNodes().get(headIndex);
				if (!head.isSetChildren()) {
					continue;
				}
				for (Integer childIndex : head.getChildren().keySet()) {
					stack.push(childIndex);
					Node child = depTree.getNodes().get(childIndex);
					String relation = head.getChildren().get(childIndex);
					result.append(relation);
					result.append("(");
					result.append(record.getRawText().substring(head.getSpan().getStart(), head.getSpan().getEnding()));
					result.append(", ");
					result.append(record.getRawText().substring(child.getSpan().getStart(), child.getSpan().getEnding()));
					result.append(")\n");
				}
			}
			System.out.println("Dependency tree");
			System.out.println(result.toString());
		}
		
		System.out.println();


		System.out.println("\n");
// 		System.out.println("Next we will get the berkeley parser annotations of the text.\n");
// 		System.out.print("Calling curator.provide(\"berkeley\", text, forceUpdate = '" + ( forceUpdate ? "TRUE" : "FALSE" ) + "')... ");

// 		try {
// 			transport.open();
// 			//call Curator
// 			record = client.provide("berkeley", text, forceUpdate);
// 			transport.close();
// 		} catch (ServiceUnavailableException e) {
// 			e.printStackTrace();
// 		} catch (TException e) {
// 			e.printStackTrace();
// 		}
		
// 		System.out.println();
// 		System.out.println("Berkeley syntactic parse:\n------------------\n\n");
// 		for (Tree depTree : record.getParseViews().get("berkeley").getTrees()) {
// 			int top = depTree.getTop(); //this tells us where in nodes the head node is
// 			Stack<Integer> stack = new Stack<Integer>();
// 			stack.push(top);
// 			result = new StringBuffer();
// 			while (!stack.isEmpty()) {
// 				int headIndex = stack.pop();
// 				Node head = depTree.getNodes().get(headIndex);
// 				if (!head.isSetChildren()) {
// 					continue;
// 				}
// 				for (Integer childIndex : head.getChildren().keySet()) {
// 					stack.push(childIndex);
// 					Node child = depTree.getNodes().get(childIndex);
// 					String relation = head.getChildren().get(childIndex);
// 					result.append(relation);
// 					result.append("(");
// 					result.append(record.getRawText().substring(head.getSpan().getStart(), head.getSpan().getEnding()));
// 					result.append(", ");
// 					result.append(record.getRawText().substring(child.getSpan().getStart(), child.getSpan().getEnding()));
// 					result.append(")\n");
// 				}
// 			}
// 			System.out.println("Parse tree");
// 			System.out.println(result.toString());
// 		}
		
// 		System.out.println();


// 		System.out.println();
// 		System.out.println("Next we will get the Wikifier's view of the text.");
// 		System.out.print("Calling curator.provide(\"wikifier\", text, forceUpdate = '" + ( forceUpdate ? "TRUE" : "FALSE" ) + "')... ");
// 		try {
// 		    transport.open();
// 		    //call Curator
// 		    record = client.provide("wikifier", text, forceUpdate);
// 		    transport.close();
// 		} catch (ServiceUnavailableException e) {
// 		    e.printStackTrace();
// 		} catch (TException e) {
// 		    e.printStackTrace();
// 		}
// 		System.out.println("done.");
// 		System.out.println();
// 		System.out.println(recordContents(record));
// 		System.out.println();

// 		result = new StringBuffer();
// 		for (Span span : record.getLabelViews().get("wikifier").getLabels()) {
// 		    result.append("Term from text: '");
// 			result.append(record.getRawText().substring(span.getStart(), span.getEnding()));
// 			result.append( "'\nLabel: " + span.getLabel()+ "\nProperties: \n" );

// 			for ( Entry< String, String > e : span.getAttributes().entrySet() )
// 			    result.append( e.getKey() + ", " + e.getValue() + "; " + "\n" );
// 			result.append("----------------------\n");
// 		}
// 		System.out.println(result.toString());
		
// 		System.out.println("\n");

// 	      System.out.print("Calling curator.provide(\"charniak_k_best\", text, forceUpdate = '" + ( forceUpdate ? "TRUE" : "FALSE" ) + "')... ");
// 	        try {
// 	            transport.open();
// 	            //call Curator
// 	            record = client.provide("charniak_k_best", text, forceUpdate);
// 	            transport.close();
// 	        } catch (ServiceUnavailableException e) {
// 	            e.printStackTrace();
// 	        } catch (TException e) {
// 	            e.printStackTrace();
// 	        }
	        
// 	        System.out.println();



// 		System.out.println();

		
		/*
		System.out.println("Charniak k-best parses:\n------------------\n\n");

    for (Tree depTree : record.getParseViews().get("charniak_k_best").getTrees()) {
			int top = depTree.getTop(); //this tells us where in nodes the head node is
			Stack<Integer> stack = new Stack<Integer>();
			stack.push(top);
			result = new StringBuffer();
			while (!stack.isEmpty()) {
				int headIndex = stack.pop();
				Node head = depTree.getNodes().get(headIndex);
				if (!head.isSetChildren()) {
					continue;
				}
				for (Integer childIndex : head.getChildren().keySet()) {
					stack.push(childIndex);
					Node child = depTree.getNodes().get(childIndex);
					String relation = head.getChildren().get(childIndex);
					result.append(relation);
					result.append("(");
					result.append(record.getRawText().substring(head.getSpan().getStart(), head.getSpan().getEnding()));
					result.append(", ");
					result.append(record.getRawText().substring(child.getSpan().getStart(), child.getSpan().getEnding()));
					result.append(")\n");
				}
			}
			System.out.println("Parse tree");
			System.out.println(result.toString());
		}
		
		System.out.println();

		*/
		

		System.out.println();
		System.out.println("Next we will get the verb Semantic Role structures...");
		System.out.print("Calling curator.provide(\"srl\", text, forceUpdate)... ");
		try {
		    transport.open();
		    //call Curator
		    record = client.provide("srl", text, forceUpdate);
		    transport.close();
		} catch (ServiceUnavailableException e) {
		    e.printStackTrace();
		} catch (TException e) {
		    e.printStackTrace();
		}
		System.out.println("done.");


		System.out.println();
		System.out.println("Semantic role labels (verbs):\n------------------\n\n");
		for (Tree depTree : record.getParseViews().get("srl").getTrees()) {
			int top = depTree.getTop(); //this tells us where in nodes the head node is
			Stack<Integer> stack = new Stack<Integer>();
			stack.push(top);
			result = new StringBuffer();
			while (!stack.isEmpty()) {
				int headIndex = stack.pop();
				Node head = depTree.getNodes().get(headIndex);
				if (!head.isSetChildren()) {
					continue;
				}
				for (Integer childIndex : head.getChildren().keySet()) {
					stack.push(childIndex);
					Node child = depTree.getNodes().get(childIndex);
					String relation = head.getChildren().get(childIndex);
					result.append(relation);
					result.append("(");
					result.append(record.getRawText().substring(head.getSpan().getStart(), head.getSpan().getEnding()));
					result.append(", ");
					result.append(record.getRawText().substring(child.getSpan().getStart(), child.getSpan().getEnding()));
					result.append(")\n");
				}
			}
			System.out.println("Verb SRL predicate-argument structure:");
			System.out.println(result.toString());
		}
		
		System.out.println();

		System.out.println();
		
		System.out.println();
		System.out.println("Next we will get the noun Semantic Role structures...");
		System.out.print("Calling curator.provide(\"nom\", text, forceUpdate)... ");
		try {
		    transport.open();
		    //call Curator
		    record = client.provide("nom", text, forceUpdate);
		    transport.close();
		} catch (ServiceUnavailableException e) {
		    e.printStackTrace();
		} catch (TException e) {
		    e.printStackTrace();
		}
		System.out.println("done.");


		System.out.println("Semantic role labels (de-verbal nouns):\n------------------\n\n");
		for (Tree depTree : record.getParseViews().get("nom").getTrees()) {
			int top = depTree.getTop(); //this tells us where in nodes the head node is
			Stack<Integer> stack = new Stack<Integer>();
			stack.push(top);
			result = new StringBuffer();
			while (!stack.isEmpty()) {
				int headIndex = stack.pop();
				Node head = depTree.getNodes().get(headIndex);
				if (!head.isSetChildren()) {
					continue;
				}
				for (Integer childIndex : head.getChildren().keySet()) {
					stack.push(childIndex);
					Node child = depTree.getNodes().get(childIndex);
					String relation = head.getChildren().get(childIndex);
					result.append(relation);
					result.append("(");
					result.append(record.getRawText().substring(head.getSpan().getStart(), head.getSpan().getEnding()));
					result.append(", ");
					result.append(record.getRawText().substring(child.getSpan().getStart(), child.getSpan().getEnding()));
					result.append(")\n");
				}
			}
			System.out.println("Noun SRL predicate-argument structure:");
			System.out.println(result.toString());
		}
		
		System.out.println();

		
// 		System.out.println("We could continue calling the Curator for other annotations but we'll stop here.");


		System.out.println();
// 		System.out.println("Next we call the MentionDetector..." );
// 		System.out.print("Calling curator.provide(\"mention\", text, forceUpdate = '" + ( forceUpdate ? "TRUE" : "FALSE" ) + "')... ");
// 		try {
// 		    transport.open();
// 		    //call Curator
// 		    record = client.provide("mention", text, forceUpdate);
// 		    transport.close();
// 		} catch (ServiceUnavailableException e) {
// 		    e.printStackTrace();
// 		} catch (TException e) {
// 		    e.printStackTrace();
// 		}
// 		System.out.println("done.");
// 		System.out.println();
// 		System.out.println(recordContents(record));
// 		System.out.println();

// 		result = new StringBuffer();
// 		for (Span span : record.getLabelViews().get("mention").getLabels()) {
// 		    result.append("Term from text: '");
// 			result.append(record.getRawText().substring(span.getStart(), span.getEnding()));
// 			result.append( "'\nLabel: " + span.getLabel()+ "\nProperties: \n" );

// 			for ( Entry< String, String > e : span.getAttributes().entrySet() )
// 			    result.append( e.getKey() + ", " + e.getValue() + "; " + "\n" );
// 			result.append("----------------------\n");
// 		}
// 		System.out.println(result.toString());
		
// 		System.out.println("\n");

	}
	
}
