package com.dft.boetools.programs;

import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.businessobjects.sdk.plugin.desktop.common.IConfiguredContainer;
import com.businessobjects.sdk.plugin.desktop.common.IConfiguredServices;
import com.businessobjects.sdk.plugin.desktop.common.IExecProps;
import com.businessobjects.sdk.plugin.desktop.enterprisenode.IEnterpriseNode;
import com.businessobjects.sdk.plugin.desktop.service.IService;
import com.businessobjects.sdk.plugin.desktop.servicecontainer.IServiceContainer;
import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.plugin.desktop.server.ExpectedRunState;
import com.crystaldecisions.sdk.plugin.desktop.server.IServer;
import com.dft.boetools.BOEHelper;
import com.dft.boetools.QueryHelper;
import com.dft.boetools.StringHelper;

/**
 * Manually configuring APS Servers post install is a time consuming and repetitive process.
 * Generally we end up configuring APS servers the same way in many environments.  Being
 * able to define the configuration once and then apply the configuration to multiple environments
 * would improve the time it takes to manage a deployment.
 * 
 * Thus the creation of this program.  Its design is to accept a properties file where we will
 * define the APS Configuration we want to apply and this program will automate the deployment of that
 * configuration.
 * 
 * This Class is implemented using the AbstractProgram base class.  However it's not really
 * intended as a deployable BusinessObjects program job, but there are a number of convieniences
 * built into the AbstractProgram that we can use even though it will be executed from the command line
 * 
 * There are many services that can be deployed to an Adaptive Processing Server, and they change
 * all the time.  Therefore we will not attempt to code for specific services and leave all configuration 
 * to the properties file that controls the program.  It will be up to the user to specify valid
 * Service Names when setting up the configuration.  The list of valid service names can be 
 * obtained from the server AdminTools UI by executing the following Query.  Note this will
 * return all services not just those that can be deployed to an Adaptive Processing Server.  You
 * will have to cross reference the returned list with the CMC GUI and its list of services which 
 * can be deployed to an Adaptive Processing Server Host. 
 * 
 * SELECT SI_ID, SI_NAME, SI_DESCRIPTION FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'Service'
 * 
 * For Reference the set of services that can be deployed in a SP5 version of Business Objects
 * Consists of the following.  The official name of each service is given followed by it's descrption
 * Which is how the serivce is displayed in the CMC GUI.
 * 
 * 1 TranslationService="Translation Service"
 * 2 MonitoringExtensionServant="Web Intelligence Monitoring Service"
 * 3 ExcelDataAccessService="Excel Data Access Service"
 * 4 JavaConnectivity_Administration="Adaptive Connectivity Service"
 * 5 LCMServiceOCCA2="Lifecycle Management Service"
 * 6 PublishingService="Publishing Service"
 * 7 LCMClearCaseServiceOCCA2="Lifecycle Management ClearCase Service"
 * 8 MON.MonitoringService="Monitoring Service"
 * 9 RebeanService="Rebean Service"
 * 10 ExtendedBlockServerServant="Visualization Service"
 * 11 SecurityTokenService="Security Token Service"
 * 12 PlatformSearchService="Platform Search Service"
 * 13 PS="BEx Web Applications Service"
 * 14 CustomDataAccessService="Custom Data Access Service"
 * 15 VisualDiffService="Visual Difference Service"
 * 16 AuditProxyService="Client Auditing Proxy Service"
 * 17 ActionFrameworkService="Insight to Action Service"
 * 18 DataFederatorService="Data Federation Service"
 * 19 DSLBridge="DSL Bridge Service"
 * 20 RecoveryService="Document Recovery Service"
 * 21 PublishingPostProcessingService="Publishing Post Processing Service"
 * 22 MDAS="Multi Dimensional Analysis Service"
 * 23 AnalysisApplicationDesignService="Analysis Application Service"
 * 
 * Parameters:
 * CONFIG_FILE: This parameter defines the location where the necessary configuration file will be
 * 				located.  The value should be a valid file path that can be accessed in the the current
 * 				operating environment, and should point to file that is in java properties format.  
 * 				The parameter is optional and has a default value of aps.properties, which will cause
 * 				the program to look for a file in the current working directory called aps.properties
 * 
 * @author roy.wells
 *
 */
public class APSBuilder extends AbstractProgram {

	private static final String CONFIG_FILE = "CONFIG_FILE";
	
	// Instance Variables to cut down on parameter passing.
	QueryHelper q;
	Configuration c;
	
	private static class Configuration {
		
		private static final String APS_NAMES = "APS_NAMES";
		private static final String DROP = "DROP";
		private static final String SERVICE_LIST = "SERVICE_LIST";
		private static final String DESCRIPTION = "DESCRIPTION";
		private static final String NODE = "NODE";
		private static final String XMX = "XMX";
		private static final String OTHER_COMMANDS = "OTHER_COMMANDS";
		
		private static final String STOPPED_APS_NAMES = "STOPPED_APS_NAMES";
		
		private static final String[] EMPTY = new String[] {};
		private Properties config = new Properties();
		
		private Logger logger = Logger.getLogger(this.getClass());
		Configuration (String configPath) throws Exception{
			
			File f = new File(configPath);
			try {
				logger.info("Loading APS Configuration from file path : " + f.getAbsolutePath());
				FileReader fr = new FileReader(f);
				
				config.load(fr);
				fr.close();
			} catch (Exception e) {
				logger.error("Could not load Configuration File from file path \"" + f.getAbsolutePath()+ "\"");
				logger.error("	Reason for failure to load is " + e);
				throw e;
			}
			logger.info("APS Configuration loaded successfully");
		}
		
		String[] getStoppedAPSNames() {
			String stoppedNames = config.getProperty(STOPPED_APS_NAMES);
			return (stoppedNames != null) ? stoppedNames.split(StringHelper.COMMA_SEPARATED_VALUES) : EMPTY;
		}
		
		String[] getAPSNames() {
			String apsNames = config.getProperty(APS_NAMES);
			return (apsNames != null) ? apsNames.split(StringHelper.COMMA_SEPARATED_VALUES) : EMPTY;
		}
		
		String[] getServicesForAPS(String apsName) {
			String serviceList = config.getProperty(toKey(apsName,SERVICE_LIST));
			return (serviceList != null) ?  serviceList.split(StringHelper.COMMA_SEPARATED_VALUES) : EMPTY;
		}		
		

		boolean isDrop(String name) {
			return Boolean.valueOf(config.getProperty(toKey(name, DROP)));
		}
		
		String getDescriptionForAPS(String apsName) {
			String desc = config.getProperty(toKey(apsName, DESCRIPTION));
			return (desc != null) ? desc : "";
		}
		
		String getXMXForAPS(String apsName) {
			return config.getProperty(toKey(apsName, XMX));
		}
		
		String getOtherArgsForAPS(String apsName) {
			return config.getProperty(toKey(apsName, OTHER_COMMANDS));
		}
		
		String getNodeName(String apsName) {
			return config.getProperty(toKey(apsName, NODE));  
		}
		
		String toKey(String name, String key) {
			return name+"."+key;
		}
		
		
	}
	
	@Override
	protected Properties getDefaultArguments() {
		Properties defaults = new Properties();
		defaults.put(CONFIG_FILE, "aps.properties");
		return defaults;
	}

	@Override
	protected void runInternal(BOEHelper boe) throws Exception {
		logger.info("Start APS Builder");
		q = new QueryHelper(boe);
		
		// Locate the APS Configuration Properties file that will control the rest of the program.
		c = new Configuration(getArgument(CONFIG_FILE));
				
		// Shutdown APS Servers flagged for shutdown.
		stopExistingAPS(q, c);
		
		// iterate through the list of APS to create
		for (String apsName : c.getAPSNames()) {
			
			logger.info("Starting Configuration of APS " + apsName);
			
			// Before we do anything that modifies an APS, make sure there is a valid set of services to deploy to an APS
			String [] servicesForAPS = c.getServicesForAPS(apsName);
			if (servicesForAPS.length < 1) {
				logger.error("No list of services found for APS " + apsName + ".  Moving on to next APS");
				continue;
			}
			
			// Get the Service List and make sure there is at least one service to deploy before proceeding.
			IInfoObjects serviceIds = getAPSServiceObjects(q, apsName,	servicesForAPS);
			if (serviceIds.size() < 1) {
				logger.error("No valid service objects foundfound for APS " + apsName + ". Moving on to next APS");
				continue;
			}
			
			
			// Get the combined Server Name that is a combination of Node Name and Server Name.
			IEnterpriseNode node = getEnterpriseNode(c.getNodeName(apsName));
			String serverName = node.getTitle() + "." + apsName;
			
			// Deal with Dropping Servers
			if (existingServerStillExists(serverName, c.isDrop(apsName))) continue; 
				
			// It's now safe to create the new Server
			IInfoObjects servers = q.newInfoObjectsCollection();
			IServer server = (IServer) servers.add(IServer.KIND);
			server.setTitle(serverName);
			server.setFriendlyName(serverName);
			server.setDescription(c.getDescriptionForAPS(apsName));			
			
			// Temporary set startup properties to prevent startup
			server.setExpectedRunState(ExpectedRunState.STOPPED);
			server.setDisabled(true);
			server.setAutoBoot(false);
			
			// Set the Servers Node
			server.setEnterpriseNode(node.getID());
			
			// Save the initial properties of the Server
			server.save();
			
			// Reload the server to pick up values set automatically by the system.
			server = (IServer) q.getObjectByID(server.getID());
			
			// Let the server know that it is an APS
			IServiceContainer serviceContainer = (IServiceContainer) q.getObjectByName("AdaptiveProcessingServiceContainer", IServiceContainer.KIND);
			server.setContainer(serviceContainer.getID());			
			
			IConfiguredServices configuredServices = server.getHostedServices();
			Iterator serviceIter = serviceIds.iterator();
			while(serviceIter.hasNext()) {
				IService service = (IService) serviceIter.next();
				logger.info("	Adding Service " + service.getTitle());
				configuredServices.add(service.getID());
			}
			
			// Re-save the Server
			server.save();
			server = (IServer) q.getObjectByID(server.getID());
			
			// Set Command Line Properties
			setXMX(server, c.getXMXForAPS(apsName));
			
			// Add Additional args if Any
			setOtherArgs(server, c.getOtherArgsForAPS(apsName));
			
			// Set Work Directory
			setWorkDirectory(server, serverName);	
			
			// Set startup parameters and do final save
			server.setExpectedRunState(ExpectedRunState.RUNNING);
			server.setDisabled(false);
			server.setAutoBoot(true);
			server.save();
			
		}
		
	}

	private IInfoObjects getAPSServiceObjects(QueryHelper q, String apsName, String[] services) throws SDKException {
		String serviceSQL = "SELECT SI_ID FROM CI_SYSTEMOBJECTS WHERE SI_NAME IN " + StringHelper.inClause(services);
		IInfoObjects serviceIds = q.executeRawQuery(serviceSQL);
		if (serviceIds.size() < services.length) {
			logger.error("	Some Service names specified in the configuration did not exist in this environment. Please check your configuration");
			logger.error("	Service names that were requested (" + services + ")");
			logger.error("	Service names that were found " + q.inClauseByName(serviceIds));			
		}
		return serviceIds;
	}

	private void stopExistingAPS(QueryHelper q, Configuration c) throws SDKException {
		 
		for (String stoppedName : c.getStoppedAPSNames()) {
			logger.info("Stopping Server " + stoppedName);
			IServer existingServer = (IServer) q.getObjectByName(stoppedName, IServer.KIND);
			existingServer.setExpectedRunState(ExpectedRunState.STOPPED);
			existingServer.setDisabled(true);
			existingServer.setAutoBoot(false);
			existingServer.save();
		}
	}
	
	/* Try to drop existing server if allowed and return the outcome */
	private boolean existingServerStillExists(String serverName, boolean dropAllowed) throws SDKException{
		// Deal with DROP Property
		IServer existingServer = (IServer) q.getObjectByName(serverName, IServer.KIND);
		if (existingServer != null) {
			// Check to see if we are dropping an exisiting APS>  If so and it exists drop it.
			// if it doesn't exist do nothing.
			if(dropAllowed){
				logger.info("	Removing existing Server");
				if (existingServer.isAlive()) {
					logger.info("	Server is currently running, attempting to shutdown before removal");
					existingServer.setExpectedRunState(ExpectedRunState.STOPPED);
					existingServer.save();
					
					// Try this 10 Times
					boolean deleted=false;
					for(int i=0; i<10; i++) {
						try { Thread.sleep(5000); } catch (InterruptedException e) {}
						existingServer = (IServer) q.getObjectByName(serverName, IServer.KIND);
						if (! existingServer.isAlive()) {
							existingServer.deleteNow();
							deleted=true;
							break;
						}
					}
					
					// Try to force it.
					if (! deleted) {
						existingServer.setExpectedRunState(ExpectedRunState.STOPNOW);
						existingServer.save();
						
						for(int i=0; i<10; i++) {
							try { Thread.sleep(5000); } catch (InterruptedException e) {}
							existingServer = (IServer) q.getObjectByName(serverName, IServer.KIND);
							if (! existingServer.isAlive()) {
								existingServer.deleteNow();
								deleted=true;
								break;
							}
						}
					}
					
					if(! deleted) {
						logger.error("	Could not stop server and therefore can not delete.  Stop this server manually before re-running processes");
						return true;
					}
					
				} else {
					existingServer.deleteNow();	
				}
				
			} else { 
				// If not dropping and the APS already exists don't make any further changes.
				logger.info("	Server already exists and DROP flag is false, no changes being made");
				return true;		
			}
		}
		
		return false;
	}
	
	private void setXMX(IServer server, String xmx) throws SDKException {		
		if (xmx != null) {
			logger.info("	Setting -Xmx property to " + xmx);
			IConfiguredContainer cfgContainer = server.getContainer();
			IExecProps execProps = cfgContainer.getExecProps();
			String args = execProps.getArgs().replace("-Xmx1g","-Xmx" + xmx);
			execProps.setArgs(args);
		}
	}
	
	private void setOtherArgs(IServer server, String otherArgs) throws SDKException {
		if (otherArgs != null) {
			// We want to add our args to the string just prior to the -jar args.
			logger.info("	Adding other arguments " + otherArgs);
			IConfiguredContainer cfgContainer = server.getContainer();
			IExecProps execProps = cfgContainer.getExecProps();
			String args = execProps.getArgs();
			StringBuilder newArgs = new StringBuilder(args);
			newArgs.insert(args.indexOf("-jar"), " " + otherArgs + " ");
			execProps.setArgs(newArgs.toString());				
		}
	}
	
	private void setWorkDirectory(IServer server, String serverName) throws SDKException {
		logger.info("	Appending Server Name to Work Directory");
		IConfiguredContainer cfgContainer = server.getContainer();
		IExecProps execProps = cfgContainer.getExecProps();
		String argsWithWorkDir = execProps.getArgs().replace("%PJSContainerDir%work", "%PJSContainerDir%work/" + serverName);
		execProps.setArgs(argsWithWorkDir);	
	}
	
	private IEnterpriseNode getEnterpriseNode(String nodeName) throws SDKException {
		if (nodeName == null) {
			return (IEnterpriseNode) q.getFirstOfKind(IEnterpriseNode.KIND);
		} else {
			return (IEnterpriseNode) q.getObjectByName(nodeName, IEnterpriseNode.KIND);
		}
	}
	

	public static void main(String[] args) {
		APSBuilder builder = new APSBuilder();
		try {
			builder.test(args);
		} catch (IllegalArgumentException e) {
			// command line user error already logged and instructions given.			
		}		
	}

}
