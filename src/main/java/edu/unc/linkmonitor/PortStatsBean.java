package edu.unc.linkmonitor;

import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.PortStatistics;
import java.net.URI;
import java.io.Writer;
import java.io.IOException;
import org.onosproject.net.Link;
import java.time.LocalTime;
import java.lang.Long;

/** The object model for the data we are sending through endpoints */
public class PortStatsBean {

    String DeviceId;
    String SrcPort;
    String DstDeviceId;
    String DstPort;
    String Age;
    String PacketsReceived;
    String RxPacketsDropped;
    String RxPacketErrors;
    String PacketsSent;
    String TxPacketsDropped;
    String TxPacketErrors;

    //TODO: write constructor

    public PortStatsBean(Link link, PortStatistics stats) {
	this.DeviceId = link.src().deviceId().uri().toString();
	this.SrcPort = String.valueOf(link.src().port().toLong());
	this.DstDeviceId = link.dst().deviceId().uri().toString();
	this.DstPort = String.valueOf(link.dst().port().toLong());
	this.Age = Long.toString(stats.durationSec());
	this.PacketsReceived = Long.toString(stats.packetsReceived());
	this.RxPacketsDropped = Long.toString(stats.packetsRxDropped());
	this.RxPacketErrors = Long.toString(stats.packetsRxErrors());
	this.PacketsSent = Long.toString(stats.packetsSent());
	this.TxPacketsDropped = Long.toString(stats.packetsTxDropped());
	this.TxPacketErrors = Long.toString(stats.packetsTxErrors());
    }

    public String getDeviceId() {
	return this.DeviceId;
    }
    public void setDeviceId(DeviceId deviceId) {
	this.DeviceId = deviceId.toString();
    }

    public String getSrcPort() {
	return this.SrcPort;
    }
    public void setSrcPort(PortNumber port) {
	this.SrcPort = port.toString();
    }

    public String getDstDeviceId() {
	return this.DstDeviceId;
    }
    
    public void setDstDeviceId(DeviceId deviceId) {
	this.DstDeviceId = deviceId.toString();
    }    

    public String getDstPort() {
	return this.DstPort;
    }
    public void setDstPort(PortNumber port) {
	this.DstPort = port.toString();
    }
    
    public String getAge() {
	return this.Age;
    }
    public void setAge(double age) {
	this.Age = String.valueOf(age);
    }

    public String getPacketsReceived() {
	return this.PacketsReceived;
    }
    public void setPacketsReceived(long packetsReceived) {
	this.PacketsReceived = String.valueOf(packetsReceived);
    }
    public String getPacketsSent() {
	return this.PacketsSent;
    }
    public void setPacketsSent(long packetsSent) {
	this.PacketsSent = String.valueOf(packetsSent);
    }
    
 
    public String getRxPacketsDropped() {
	return this.RxPacketsDropped;
    }
    public void setRxPacketsDropped(long packetsDropped) {
	this.RxPacketsDropped = String.valueOf(packetsDropped);
    }
    public String getRxPacketErrors() {
	return this.RxPacketErrors;
    }
    public void setRxPacketErrors(long packetErrors) {
	this.RxPacketErrors = String.valueOf(packetErrors);
    }


    public String getTxPacketsDropped() {
	return this.TxPacketsDropped;
    }
    public void setTxPacketsDropped(long packetsDropped) {
	this.TxPacketsDropped = String.valueOf(packetsDropped);
    }
    public String getTxPacketErrors() {
	return this.TxPacketErrors;
    }
    public void setTxPacketErrors(long packetErrors) {
	this.TxPacketErrors = String.valueOf(packetErrors);
    }

    public static String getHeaders() {
	String entry = "Time,DeviceId,SrcPort,DstDeviceId,DstPort,Age,PktsRecvd,PktsSent,RxPktDrop,TxPktDrop,RxPktErr,TxPktErn\n";
	return entry;
    }

    public boolean write(Writer file) {
	try {
	    String entry = LocalTime.now().toString() + "," + getDeviceId() + "," + getSrcPort() + "," + getDstDeviceId() + "," + getDstPort() + "," + getAge() + "," + getPacketsReceived() + "," + getPacketsSent() + ","  + getRxPacketsDropped() + "," + getTxPacketsDropped() + "," + getRxPacketErrors() + "," + getTxPacketErrors() + "\n";
	    file.write(entry);
	    file.flush();
	    return true;
	} catch (IOException e) {
	    return false;
	}
    }

}
