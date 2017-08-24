package edu.unc.linkmonitor;

import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.PortNumber;
import org.onosproject.net.DeviceId;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.net.link.LinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.ComponentContext;
import org.onlab.util.Tools;
import org.onlab.osgi.DefaultServiceDirectory;
import java.io.IOException;
import java.io.Writer;
import java.io.FileWriter;
import java.io.File;
import java.util.Timer;
import java.util.Dictionary;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;
import java.util.List;
import java.util.ArrayList;
import com.google.common.base.Strings;

@Component(immediate = true)
public class LinkMonitor {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private LinkService linkService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceService deviceService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ComponentConfigService cfgService;

    // CSV will be written to $karaf/logs/
    private static final String CSV_FILE_NAME = "logs/port_stats";
    
    private static final long DEFAULT_POLLING_INTERVAL = 5000;    
    // 5 sec is the OpenFlow default polling period
    @Property(name = "pollingInterval", longValue = DEFAULT_POLLING_INTERVAL, label = "The Polling Interval")
    private long pollingInterval = DEFAULT_POLLING_INTERVAL;
    private static boolean running = true;
    private static final String QUEUE_NAME = "link_stats";

    private Writer portStats;
    
    private int proof = 1;

    private Timer timerTask;
    
    
    private class LinkStatsTask extends TimerTask {

	@Override
	public void run() {
	    log.info("Timer Task has been run " + proof + " time(s)!");
	    proof++;
	    if (linkService == null) {
		log.error("linkService is NULL");
	    }
	    if (deviceService == null) {
		log.error("linkService is NULL");
	    }
	    if (cfgService == null) {
		log.error("cfgService is NULL");
	    }
	    if (portStats != null) {
		// For each link get the src port and get stats on the port
		for (Link l : linkService.getActiveLinks()) {
		    PortStatistics stats = deviceService.getStatisticsForPort(l.src().deviceId(), l.src().port());
		    // TODO: send the stats to the rabbitMQ broker
		    // see https://www.rabbitmq.com/api-guide.html for examples
		    if (stats == null) {
			log.error("Port Stats for Link " + l.src().deviceId().uri().toString() + " <-------->" + l.dst().deviceId().uri().toString());
		    }
		    else {
			PortStatsBean bean = new PortStatsBean(l,stats);
			bean.write(portStats);
		    }
		    // Message contents:
		    // Essentially think of a message as the key-value pair where key is the link (identified by two deviceIds)
		    // and the value is the stats (potentially as a mapping from string to double, e.g. "bytes_sent" -> value)
		    // TODO: include a timestamp in the message
		}	    
	    }
	    
	}
    }
    
    @Modified
    public void modified(ComponentContext context) {
	Dictionary<?, ?> properties = context.getProperties();
	if (properties == null) {
	    log.info("Properties is NULL!!!");
	}
	
	String s = Tools.get(properties, "pollingInterval");

	if (Strings.isNullOrEmpty(s)) {
	    log.info("The String S is empty/NULL!!!!");
	}
	else {
	    log.info("We Parsed " + Integer.parseInt(s.trim()));
	}
	
	pollingInterval = Strings.isNullOrEmpty(s) ? DEFAULT_POLLING_INTERVAL : Integer.parseInt(s.trim());
	log.info("Set the polling interval to " + pollingInterval);
    }
    
    @Activate void activate () {
	log.info("Calling Activate!");
	log.info("Activated");
    }
    
    @Activate
    protected void activate(ComponentContext context) {
	log.info("Going to Activate");
	if (linkService == null) {
	    log.error("linkService is NULL");
	}
	if (deviceService == null) {
	    log.error("linkService is NULL");
	}
	if (cfgService == null) {
	    log.error("cfgService is NULL");
	}
	cfgService.registerProperties(getClass());
	modified(context);
	log.info("Loaded the Configurable Properties");

	try {
	    File portStatsFile = new File(CSV_FILE_NAME + ".csv");
	    File oldPortStatsFile = new File(CSV_FILE_NAME + "-old.csv");
	    if (portStatsFile.exists()) {
		log.info("Old portStats data file exists, renaming file to $stats-old");
		portStatsFile.renameTo(oldPortStatsFile);
	    }
	    portStatsFile.createNewFile();

	    portStats = new FileWriter(portStatsFile);
	    portStats.write(PortStatsBean.getHeaders());
	    
	} catch (IOException e) {
	    log.error("Unable to open Port Stats File");
	    portStats = null;
	}
    
	log.info("Starting the timer!");
	// TODO: make polling pollingInterval configurable as well
	
	timerTask = new Timer();
	timerTask.scheduleAtFixedRate(new LinkStatsTask(), 0, pollingInterval);
	log.info("Started LinkLoadMonitor");
    }
    
    @Deactivate
    protected void deactivate() {
	/**
	 * Stop the link monitor
         */
	log.info("Calling Dectivate");
	running = false;
	if (linkService == null) log.error("linkService is NULL");
	if (deviceService == null) log.error("deviceService is NULL");
	if (cfgService == null) log.error("cfgService is NULL");
	cfgService.unregisterProperties(getClass(), false);
	if (timerTask == null) log.error("Timer Task is NULL");
	else {
	    log.info("Ending Timer Task..");
	    timerTask.purge();
	    timerTask.cancel();
	}
	if (portStats == null) log.error("File Writer 'portStats' is NULL");
	else {
	    try {
		log.info("Closing Port Stats File..");
		portStats.close();
	    } catch (IOException e) {
		log.error("IOException while trying to close Port Stats File");
	    }
	}
	
	/*
	//TODO: close anything else if necessary
	if (rchannel != null) {
	    try {
		rchannel.close();
	    } catch (IOException e) {
		log.error("Error closing a RabbitMQ channel.", e);
	    } catch (TimeoutException e) {
		log.error("Timed out when closing a RabbitMQ channel", e);
		e.printStackTrace();
	    }
	}
	
	if (rabbitConn != null) {
	    try {
		rabbitConn.close();
	    } catch (IOException e) {
		log.error("Error closing connection to RabbitMQ broker. Deactivating anyway.", e);
	    }
	}
	*/
	log.info("Stopped LinkLoadMonitor");
    }
    
}
