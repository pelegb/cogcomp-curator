package edu.illinois.cs.cogcomp.archive;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;

/**
 * This class provides an abstract base class which Archive implementations
 * can subclass as a configurable starting point.
 * 
 * @author Michael Deleo, SRI International
 *
 */
public abstract class AbstractArchive implements Archive {

	protected final Configuration config;

	/**
	 * Construct an AbstractArchive with specified configuration.
	 * 
	 * @param config the configuration
	 * @throws ConfigurationException
	 */
	public AbstractArchive(final Configuration config) throws ConfigurationException {
		this.config = config;
	}

	/**
	 * Returns the configuration for the AbstractArchive
	 * @return the configuration
	 */
	public final Configuration getConfig() {
		return this.config;
	}
	
}
