package edu.unc;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.felix.scr.annotations.*;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.link.LinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

@Component(immediate = true)
public class AppComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private LinkService linkService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceService deviceService;
    protected Connection rabbitConn = null;
    protected Channel rchannel = null;

    // 5 sec is the OpenFlow default polling period
    private long pollingInterval = 5000;
    private static boolean running = true;
    private static final String QUEUE_NAME = "link_stats";

    private class LinkStatsTask extends TimerTask {

        @Override
        public void run() {
            // Assuming that we will have somewhere to send these stats
            if (rabbitConn != null && rchannel != null) {
                // For each link get the src port and get stats on the port
                for (Link l : linkService.getActiveLinks()) {
                    PortStatistics stats = deviceService.getDeltaStatisticsForPort(l.src().deviceId(), l.src().port());
                    // TODO: send the stats to the rabbitMQ broker
                    // see https://www.rabbitmq.com/api-guide.html for examples


                    // Message contents:
                    // Essentially think of a message as the key-value pair where key is the link (identified by two deviceIds)
                    // and the value is the stats (potentially as a mapping from string to double, e.g. "bytes_sent" -> value)
                    // TODO: include a timestamp in the message
                }
            } else {
                log.warn("No connection to RabbitMQ broker established, skipping");
            }
        }
    }


    private void connect() {
        // TODO: make connection parameters to the broker configurable. Currently assumes localhost instance with default params
        ConnectionFactory factory = new ConnectionFactory();
        try {
            rabbitConn = factory.newConnection();
            rchannel = rabbitConn.createChannel();
        } catch (IOException e) {
            log.error("Failed to connect to the RabbitMQ broker, is the broker up?", e);
        } catch (TimeoutException e) {
            log.error("Timed out when connecting to the RabbitMQ broker, is the broker up?", e);
        }
    }

    @Activate
    protected void activate() {
        connect();

        // TODO: make polling pollingInterval configurable as well
        new Timer().scheduleAtFixedRate(new LinkStatsTask(), 0, pollingInterval);
        log.info("Started LinkLoadMonitor");
    }

    @Deactivate
    protected void deactivate() {
        /**
         * Stop the link monitor
         */
        running = false;
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
        log.info("Stopped LinkLoadMonitor");
    }

}
