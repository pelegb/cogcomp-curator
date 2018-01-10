package edu.illinois.cs.cogcomp.archive;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;

/*
 * Database Adapter Managing Store, Update, Remove etc. of mongoDB 
 */
public class DatabaseStore extends Observable{

	protected final Logger logger = LoggerFactory
			.getLogger(edu.illinois.cs.cogcomp.archive.DatabaseStore.class);
	
	private final TSerializer serializer;
	private final TDeserializer deserializer;
	private final String name;
	
	private final AtomicInteger fetchCount = new AtomicInteger(0);
	private final AtomicInteger fetchTime = new AtomicInteger(0);
	private final AtomicInteger storeCount = new AtomicInteger(0);
	private final AtomicInteger storeTime = new AtomicInteger(0);

	private final Map<String, Long> accessLog = new ConcurrentHashMap<String, Long>();
	
	private DBCollection coll = null;
	
	public DatabaseStore(String collName, DB db){
		this(collName, db, new TSerializer(
				new TBinaryProtocol.Factory()), new TDeserializer(
				new TBinaryProtocol.Factory()));
	}
	
	public DatabaseStore(String collName, DB db,
			TSerializer s, TDeserializer d){
		
		this.serializer = s;
		this.deserializer = d;
		this.name = collName;
		
		coll = db.getCollection(collName);
		ensureIndex();
		
		logger.info("Database Store {} initialized.", collName);
	}
	
	private void ensureIndex(){
		coll.ensureIndex(new BasicDBObject("sha1_hash", 1), new BasicDBObject("unique", true));
	}
	
	
	public synchronized <T extends TBase> boolean store(T datum, Class<T> clazz) throws ArchiveException {
		
		long startTime = System.currentTimeMillis();
		String identifier = Identifier.getId(datum);
		long time = System.currentTimeMillis();
		
		//byte[] blob = serialize(datum);
		//InputStream is = new ByteArrayInputStream(blob);
		
		logger.debug("Storing datum in {} with digest [{}]", name, identifier);
		boolean result = false;
		WriteResult wr;
		
		byte[] blob = serialize(datum);
		
		BasicDBObject insert_query = new BasicDBObject();
		insert_query.put("sha1_hash", identifier);
		insert_query.put("datum", blob);
		insert_query.put("last_update", time);
		insert_query.put("last_access", time);
		
		if (getById(identifier, clazz) == null) {
			wr = coll.save(insert_query);
		}
		else{
			BasicDBObject update_query = new BasicDBObject();
			update_query.append("sha1_hash", identifier);
			
			wr = coll.update(update_query, insert_query);
		}
		
		result = wr.getError() == null;
		
		long endTime = System.currentTimeMillis();
		// we already updated the access time so lets remove it from the log
		if (accessLog.containsKey(identifier)) {
			accessLog.remove(identifier);
		}
		storeCount.incrementAndGet();
		storeTime.addAndGet((int) (endTime - startTime));
		
		if(!result) logger.error("Error Updating Database: " + wr.getError());
		
		return result;
	}

	
	public synchronized <T extends TBase> T getById(String identifier, Class<T> clazz) throws ArchiveException {
		long startTime = System.currentTimeMillis();
		T datum = null;
		
		BasicDBObject get_query = new BasicDBObject();
		get_query.put("sha1_hash", identifier);
		
		try{
		    logger.debug( "requesting db entry for identifier '{}'", identifier );

			DBCursor get_result = coll.find(get_query);
			if(get_result.hasNext()){
				DBObject doc = get_result.next();
				datum = deserialize((byte[])doc.get("datum"), clazz);
			}
			logger.debug( "db request completed." );

			accessLog.put(identifier, System.currentTimeMillis());
			// tell the observers we've modified access log
			setChanged();
			notifyObservers(accessLog.size());
			long endTime = System.currentTimeMillis();
			String msg = "Retrieved " + ( null == datum ? "empty " : "previously created " ) + 
			    "record";
			logger.debug( msg + " in {}ms", endTime - startTime);
			// store the details for logging
			fetchCount.incrementAndGet();
			fetchTime.addAndGet((int) (endTime - startTime));
			
		}catch(MongoException e){
		    logger.warn("Error creating prepared statements {}", e.getMessage() );
			logger.warn("Exception was: '" + e.getMessage() + "'");
			logger.error("Error getting datum for identifier: {}", identifier);
			throw new ArchiveException("Underlying database error" + e.getMessage(), e);
		}
		
		return datum; // will be null if 
	}
	
	
	
	
	private byte[] serialize(TBase datum) throws ArchiveException {
		try {
			return serializer.serialize(datum);
		} catch (TException e) {
			e.printStackTrace();
			throw new ArchiveException("Problem serializing data", e);
		}
	}

	private <T extends TBase> T deserialize(byte[] bytes, Class<T> clazz) throws ArchiveException {
		T datum = null;
		try {
			datum = clazz.newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			deserializer.deserialize(datum, bytes);
		} catch (TException e) {
			e.printStackTrace();
			throw new ArchiveException("Problem serializing data", e);
		}
		return datum;
	}
	
	/**
	 * Iterates over the cache of accessed identifiers and updates their access
	 * time.
	 * 
	 * @throws TException
	 */
	public synchronized void updateAccess() throws ArchiveException {
		long startTime = System.currentTimeMillis();
		Set<String> doneIdents = new HashSet<String>();
		for (String identifier : accessLog.keySet()) {
			long time = accessLog.get(identifier);

			BasicDBObject update_query = new BasicDBObject();
			BasicDBObject set_query = new BasicDBObject();
			update_query.append("sha1_hash", identifier);

			BasicDBObject accessObj = new BasicDBObject();
			accessObj.put( "last_access", time );

			set_query.append("$set", accessObj );
			
			WriteResult wr = coll.update(update_query, set_query);
			String wrError = wr.getError();
			if(wrError != null){
				logger.warn("Error Updating Access Time: {}", wrError);
			    logger.warn( "Error from WriteResult is '" + wr.getError() + "'.");
			    
			    String updateStr = update_query.toString();
			    String queryStr = set_query.toString();
			    logger.warn( "set query was'" + queryStr + "', update query was '" + updateStr + "'." );
			    throw new ArchiveException("Underlying database error (updateAccess())");	//no exception
			}

			doneIdents.add( identifier );
			
		}
		for (String ident : doneIdents) {
			accessLog.remove(ident);
		}
		long endTime = System.currentTimeMillis();
		logger.info("Finished updating access times in " + name
				+ " for {} items in {}ms", doneIdents.size(), endTime
				- startTime);
	}
	
	
	
	/**
	 * Expires (removes) data which is older than expireAge milliseconds.
	 * 
	 * @param expireAge
	 * @throws TException
	 * NEED ATOMIC????
	 */
	public void expire(long expireAge) throws ArchiveException {
		
		BasicDBObject expire_query = new BasicDBObject();
		expire_query.append("last_access", 
				new BasicDBObject().append("$lt", System.currentTimeMillis() - expireAge));
		WriteResult wr = coll.remove(expire_query);
		
		if(wr.getError() == null){
			logger.info("Deleted {} Old Items in {}", wr.getN(), name);
		}
		else{
			logger.warn("Error Removing Expired Records", wr.getError());
			logger.warn( "Error from WriteResult is '" + wr.getError() + "'.");
							
			throw new ArchiveException("Underlying database error");	//no exception
		}

	}
	
	
	/**
	 * Provide a status report of the database store.
	 * 
	 * @return the report
	 */
	public String getStatusReport() {
		int fc = fetchCount.getAndSet(0);
		int ft = fetchTime.getAndSet(0);
		int sc = storeCount.getAndSet(0);
		int st = storeTime.getAndSet(0);
		int fa = fc == 0 ? 0 : ft / fc;
		int sa = sc == 0 ? 0 : st / sc;

		long count = 0;
		
		count = coll.find().count();
		
		String result = String.format(
				"%s datastore | fetches: %d %dms | stores: %d %dms | items: %d",
				name, fc, fa, sc, sa, count);
		return result;
	}
	
}
