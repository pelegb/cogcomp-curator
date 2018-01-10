package Tests;




import CAFToCurator.CAFtoCurator;
import CuratorToCAF.CuratorToCAF;

public class runTests {


	public static void main(String[] args) {

	    if ( args.length != 1 ) 
	    {
		System.err.println( "USAGE: runTests <file>.DocAnnotations.json" );
		System.exit( -1 );
	    }

	    String fileName = args[0];

	    runTestFromCAF( fileName );

	}


	/**
	 * Create record object from original CAF file path, then create CAF file from this record.
	 */


	public static void runTestFromCAF(String CAFFilePath){
		CAFtoCurator fromCAF = new CAFtoCurator(CAFFilePath);
		CuratorToCAF fromCurator = new CuratorToCAF(fromCAF.getRecord());
		System.out.println( "Running test on file '" + CAFFilePath+ "'..." );
		System.out.println("----ORIGINAL CAF JSON----\n");
		System.out.println(fromCAF.getOriginalCAFJson());
		System.out.println("\n----NEW CAF JSON AFTER CONVERSION/UNCONVERSION----\n");
		System.out.println(fromCurator.getDocAnnotationsJson());
		
	}
	

}
