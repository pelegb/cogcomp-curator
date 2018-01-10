package edu.illinois.cs.cogcomp.archive;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.thrift.TBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

import edu.illinois.cs.cogcomp.thrift.curator.MultiRecord;
import edu.illinois.cs.cogcomp.thrift.curator.Record;


/*
 * Database Adapter of Curator using MongoDB
 */
public class DatabaseArchive extends AbstractArchive{
	
	protected final Logger logger = LoggerFactory
			.getLogger(edu.illinois.cs.cogcomp.archive.DatabaseArchive.class);
	
	private final String dburl;
	private final String user;
	private final String password;
	
	//mongoDB connections
	private Mongo mongo = null;
	private DB db = null;
	private String DB_NAME = "curator";
	
	private final Map<Class<?>, DatabaseStore> stores = new HashMap<Class<?>, DatabaseStore>();
	
	/**
	 * Constructors
	 */
	public DatabaseArchive() throws ConfigurationException{
		this(new PropertiesConfiguration("configs/database.properties"));
	}

	public DatabaseArchive(Configuration config) throws ConfigurationException {
		super(config);
		
		//dburl = String.format("jdbc:h2:%s;DB_CLOSE_ON_EXIT=FALSE", 
						//config.getString("database.url", "mem:db1"));
		
		dburl = config.getString("database.url", "mem:db1");
		
		user = config.getString("database.user", "sa");
		password = config.getString("database.password", "");
		
		init();
		
		long maintenanceTime = config.getLong("database.maintenancetime", 300) * 60 * 1000;
		long expireTime =  config.getLong("database.expiretime", 30);
		long expireAge = -1;
		if ( expireTime > 0 ) 
		    expireAge = expireTime * 24 * 60 * 60 * 1000;
		int updateCount = config.getInt("database.updatecount", 1000);
		final long reportTime = config.getLong("database.reporttime", 5) * 60 * 1000;
		
		// add an observer for access logs
		for (DatabaseStore store : stores.values())
			store.addObserver(new DatabaseAccessListener(updateCount));
		
		if ( expireTime > 0 )
		    {
			// start the maintenance thread
			Thread maintenance = new Thread(new DatabaseMaintenanceWorker(
										      maintenanceTime, expireAge, this), "Database Maintainer");
			maintenance.start();
		    }
		else 
		    {
			logger.warn( "Not starting record-expiry maintenance worker, as expiretime was non-positive." );
		    }

		// start the reporter
		Thread reporter = new Thread("Database Report") {
			public void run() {
				for (;;) {
					for (DatabaseStore store : stores.values())
						logger.info(store.getStatusReport());
						try {
							Thread.sleep(reportTime);
						} catch (InterruptedException e) {
						}
					}
				}
			};
		reporter.start();
		
		// add shutdown manager
		Runtime.getRuntime().addShutdownHook(
				new Thread(new ShutdownListener(this),
								"Database Shutdown Listener"));
		
	}
	
	
	/**
	 * Initialize the database.
	 */
	private synchronized void init() {
		boolean auth = false;
		try {
			mongo  = new Mongo(dburl);
			db = mongo.getDB(DB_NAME);
			auth = db.authenticate(user, password.toCharArray());
			
		} catch (UnknownHostException e) {
		    logger.error("Cannot Find Database Host: "+dburl + ": " + e.getMessage() );
			System.exit(1);
		} catch (MongoException e) {
		    logger.error("Error Connecting to Database: " + e.getMessage());
			System.exit(1);
		}
		
		if(auth){
			// record store
			stores.put(Record.class, new DatabaseStore("records", db));
			// multirecord store
			stores.put(MultiRecord.class, new DatabaseStore("multirecords", db));
			
			logger.info("Database Initialized.");
		}
		else{
			logger.error("Database Authentification Failed.");
			System.exit(1);
		}
	}
	
	
	
	
	@Override
	public boolean close() throws ArchiveException {
		mongo.close();	//Exceptions?
		logger.info("Database Connection Closed.");
		return true;
	}
	

	@Override
	public synchronized <T extends TBase> T getById(String identifier,
			Class<T> clazz) throws ArchiveException {
		DatabaseStore store = stores.get(clazz);

		logger.debug( "Calling DatabaseStore.getById() for identifier '{}'", identifier );
		T datum = store.getById(identifier, clazz);
		logger.debug( "Got response from DatabaseStore; returning." );
		return datum;
	}

	@Override
	public synchronized <T extends TBase> T get(String text, boolean ws,
			Class<T> clazz) throws ArchiveException {
		return getById(Identifier.getId(text, ws), clazz);
	}

	@Override
	public synchronized <T extends TBase> T get(List<String> text,
			Class<T> clazz) throws ArchiveException {
		return getById(Identifier.getId(text), clazz);
	}

	@Override
	public synchronized <T extends TBase> boolean store(T datum, Class<T> clazz)
			throws ArchiveException {
		DatabaseStore store = stores.get(clazz);
		return store.store(datum, clazz);
	}

	
	public void updateRecordAccess() throws ArchiveException {
		logger.info("Updating access times on data");
		for (DatabaseStore store : stores.values())
			store.updateAccess();
		logger.info("Finished updating access times on data");
	}

	public void expireRecords(long expireAge) throws ArchiveException {
	
	    if ( expireAge > 0 )
	    {
		logger.info("Expiring records of sufficient age...");
		for (DatabaseStore store : stores.values())
			store.expire(expireAge);
		logger.info("Finished expiring records.");
	    }
	}
	
	
	/**
	 * Shutdown Listener
	 */
	private class ShutdownListener implements Runnable {

		private final DatabaseArchive dba;

		public ShutdownListener(DatabaseArchive dba) {
			this.dba = dba;
		}

		public void run() {
			try {
				dba.close();
			} catch (ArchiveException e) {
				dba.logger.error("Exception closing database.", e);
			}
		}
	}

}


/**
 * Database Access Listener
 * 
 * An Observer that gets notified whenever an access is made to the database.
 * Responsible for updating access times of records once the cache reaches a
 * certain size.
 */
class DatabaseAccessListener implements Observer, Runnable {
	private DatabaseStore dbs;
	private int updatecount;
	private volatile boolean isRunning;

	public DatabaseAccessListener() {
		this(1000);
	}

	public DatabaseAccessListener(int updatecount) {
		this.updatecount = updatecount;
		this.isRunning = false;
	}

	public void update(Observable o, Object arg) {
		if (isRunning)
			return;
		int size = (Integer) arg;
		if (size > updatecount) {
			this.dbs = (DatabaseStore) o;
			Thread t = new Thread(this, "Database Update Listener");
			isRunning = true;
			t.start();
		}
	}

	public void run() {
		try {
			dbs.updateAccess();
		} catch (ArchiveException e) {
			dbs.logger.error("Error updating record access.", e);
		}
		isRunning = false;
	}
}

/**
 * Database Maintenance Worker
 * 
 * Responsible for keeping the database in shipshape. Performs maintenance every
 * sleepTime milliseconds. Its duties consist of expiring old records and
 * updating record access times.
 * 
 */
class DatabaseMaintenanceWorker implements Runnable {
	private static Logger logger = LoggerFactory
			.getLogger(DatabaseMaintenanceWorker.class);
	private long expireAge;
	private long sleepTime;
	private DatabaseArchive dba;

	public DatabaseMaintenanceWorker(long delay, long expireAge,
			DatabaseArchive dba) {
		this.sleepTime = delay;
		this.expireAge = expireAge;
		this.dba = dba;
	}

	public void run() {
		try {
			for (;;) {
			    if ( expireAge > 0 )
			    {
				logger.info("Performing routine maintenance");
				try {
					dba.updateRecordAccess();
					dba.expireRecords(expireAge);
				} catch (ArchiveException e) {
					logger.error("Error performing maintenance.", e);
				}
				logger.info("Maintenance complete.");
				Thread.sleep(sleepTime);
			    }
			}
		} catch (InterruptedException e) {

		}
	}
}
