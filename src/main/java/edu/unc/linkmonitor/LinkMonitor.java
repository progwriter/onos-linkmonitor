package edu.unc.linkmonitor;

import com.google.common.base.Strings;
import org.apache.felix.scr.annotations.*;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.link.LinkService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Dictionary;
import java.util.Timer;
import java.util.TimerTask;

@Component(immediate = true)
public class LinkMonitor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private LinkService linkService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceService deviceService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ComponentConfigService cfgService;

    private static final String DEFAULT_CSV_FILE_NAME = "port_stats";
    @Property(name="filename", value=DEFAULT_CSV_FILE_NAME, label="CSV file name")
    private String csvFileName = DEFAULT_CSV_FILE_NAME;

    private static final long DEFAULT_POLLING_INTERVAL = 5000;
    // 5 sec is the OpenFlow default polling period
    @Property(name = "pollingInterval", longValue = DEFAULT_POLLING_INTERVAL,
            label = "The Polling Interval")
    private long pollingInterval = DEFAULT_POLLING_INTERVAL;

    private Writer portStatsWriter;
    private int timerCounter = 1;
    private Timer timerTask;


    private class LinkStatsTask extends TimerTask {

        @Override
        public void run() {
            log.debug("Timer Task has been run " + timerCounter + " time(s)!");
            timerCounter++;
            if (linkService == null) {
                log.error("linkService is NULL");
            }
            if (deviceService == null) {
                log.error("linkService is NULL");
            }
            if (cfgService == null) {
                log.error("cfgService is NULL");
            }
            if (portStatsWriter != null) {
                // For each link get the src port and get stats on the port
                for (Link l : linkService.getActiveLinks()) {
                    PortStatistics stats = deviceService.getDeltaStatisticsForPort(l.src().deviceId(), l.src().port());
                    if (stats == null) {
                        log.error("No port stats for link " + l.src().deviceId().uri().toString() + " <-->"
                                + l.dst().deviceId().uri().toString());
                    } else {
                        PortStatsBean bean = new PortStatsBean(l, stats);
                        try {
                            bean.write(portStatsWriter);
                        } catch (IOException e) {
                            log.warn("Failed port stats write");
                        }
                    }
                }
            }

        }
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        if (properties == null) {
            log.error("Properties is NULL");
            return;
        }

        String s = Tools.get(properties, "pollingInterval");

        if (Strings.isNullOrEmpty(s)) {
            log.debug("Invalid polling interval");
            pollingInterval = DEFAULT_POLLING_INTERVAL;
        } else {
            log.debug("We Parsed " + Integer.parseInt(s.trim()));
            pollingInterval = Integer.parseInt(s.trim());
            log.debug("Set the polling interval to " + pollingInterval);
        }

        s = Tools.get(properties, "csvFile");
        if (!Strings.isNullOrEmpty(s)) {
            if (!s.equals(csvFileName)) {
                try {
                    portStatsWriter.close();
                } catch (IOException e) {
                    log.error("Could not close CSV file properly", e);
                }
                csvFileName = s;
                openFile();
            }
        }

    }

    @Activate
    void activate() {
        log.info("Activated");
    }


    private void openFile() {
        try {
            File portStatsFile = new File(csvFileName + ".csv");
            File oldPortStatsFile = new File(csvFileName + "-old.csv");
            if (portStatsFile.exists()) {
                log.debug("Old portStats data file exists, renaming file to $stats-old");
                portStatsFile.renameTo(oldPortStatsFile);
            }
            portStatsFile.createNewFile();

            portStatsWriter = new FileWriter(portStatsFile);
            portStatsWriter.write(PortStatsBean.getHeaders());

        } catch (IOException e) {
            log.error("Unable to open Port Stats File");
        }
    }

    @Activate
    protected void activate(ComponentContext context) {
        log.info("Going to Activate");
        if (linkService == null) {
            log.error("linkService is NULL. Aborting.");
            return;
        }
        if (deviceService == null) {
            log.error("linkService is NULL. Aborting.");
            return;
        }
        if (cfgService == null) {
            log.error("cfgService is NULL. Aborting.");
            return;
        }
        cfgService.registerProperties(getClass());
        modified(context);
        log.debug("Loaded the Configurable Properties");

        openFile();

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
        if (linkService == null) log.error("linkService is NULL");
        if (deviceService == null) log.error("deviceService is NULL");
        if (cfgService == null) {
            log.error("cfgService is NULL");
        } else {
            cfgService.unregisterProperties(getClass(), false);
        }
        if (timerTask == null) {
            log.error("Timer Task is NULL");
        } else {
            log.debug("Ending Timer Task");
            timerTask.purge();
            timerTask.cancel();
        }
        if (portStatsWriter == null) {
            log.warn("File Writer 'portStatsWriter' is NULL");
        } else {
            try {
                log.debug("Closing Port Stats File");
                portStatsWriter.close();
            } catch (IOException e) {
                log.error("IOException while trying to close Port Stats File");
            }
        }
        log.info("Stopped LinkLoadMonitor");
    }

}
