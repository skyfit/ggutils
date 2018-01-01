package org.ggraham.ggutils.test;

	/*
	 * 
	 * Apache License 2.0 
	 * 
	 * Copyright (c) [2017] [Gregory Graham]
	 * 
	 * See LICENSE.txt for details.
	 * 
	 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.ggraham.ggutils.PackageService;
import org.ggraham.ggutils.logging.LogLevel;
import org.ggraham.ggutils.message.IHandleMessage;
import org.ggraham.ggutils.message.PacketDecoder;
import org.ggraham.ggutils.message.PacketDecoder.FieldType;
import org.ggraham.ggutils.network.UDPReceiver;

public class TestUDPReceiver {
	
	public static void usage() {
		System.out.println("Receives and decodes UDP packet data");
		System.out.println("");
		System.out.println("-c N  (optional) assumes first field is a sequence and checks for N packets");
		System.out.println("-p    (optional) print packet data");
		System.out.println("-r p  (optional) specifies port number p, default 5555");
		System.out.println("-t t  (required if -p, ignored if ! -p) adds a field of type t to the packet");
		System.out.println("-u    (optional) print usage");
		System.out.println("-w w  (optional) stop listening after w seconds");
		System.out.println("");
		System.out.println("Examples");
		System.out.println("");
		System.out.println("-c 50000 -t STRING -t INTEGER -t STRING");
		System.out.println("Expects 50000 packets with sequence number, prints a report of missing ");
		System.out.println("saves in test.csv, and sends on localhost:5555");
		System.out.println("");
		System.out.println("-f test.csv -i 192.168.1.2 -p 6666");
		System.out.println("Reads packets from test.csv, prepends a sequence number and sends to 192.168.1.2:6666");
		System.out.println("");
	}

	static int numRecv = 0;
	static int[] vals = null;
	static boolean print = false;

	public static void main(String[] args) throws InterruptedException, IOException {

	    ArrayList<PacketDecoder.PacketFieldConfig> fields = new ArrayList<PacketDecoder.PacketFieldConfig>();
	    PacketDecoder decoder = new PacketDecoder();

	    int port = 5555;
	    int wait = 0;
	    
	    int i = 0;
	    try {
	    while ( i < args.length ) {
	    	String currentArg = args[i++];
	    	if (currentArg.equals("-c")) {
	    		numRecv = Integer.parseInt(args[i++]);
        	} else if (currentArg.equals("-t")) {
        		fields.add(new PacketDecoder.PacketFieldConfig(FieldType.valueOf(args[i++])));
        	} else if (currentArg.equals("-p")) {
	    		print = true;
        	} else if (currentArg.equals("-u")) {
	    		usage();
	    	} else if (currentArg.equals("-r")) {
	    		port = Integer.parseInt(args[i++]);
	    	} else if (currentArg.equals("-w")) {
	    		wait = 1000 * Integer.parseInt(args[i++]);
	    	}
	    }
	    }
	    catch (Exception e) {
	    	usage();
	    	System.exit(0);
	    }

	    if ( numRecv > 0 ) {
	    	fields.add(0, new PacketDecoder.PacketFieldConfig(FieldType.INTEGER));
	    }
	    
	    for ( int m=0; m < fields.size(); m++ ) {
	    	decoder.addField(fields.get(m));
	    }
		vals = new int[numRecv];

		PackageService.getLog().setLogLevel(LogLevel.BASIC);
		
		UDPReceiver recv = new UDPReceiver("localhost", port, true, new IHandleMessage<ByteBuffer>() {
			@Override
			public boolean handleMessage(ByteBuffer buffer) {		
				boolean doStop = false;
				Object[] obj = new Object[fields.size()];
				decoder.DecodePacket(buffer, obj);
				if ( numRecv > 0 ) {
    				int seq = Integer.parseInt(obj[0].toString());
	    			vals[seq] = 1;
				}
				if ( print ) {
					StringBuilder builder = new StringBuilder();
					for ( int jj=0; jj<obj.length; jj++ ) {
						builder.append(obj[jj].toString()).append("  ");
					}
					System.out.println(builder.toString());
				}
				return true;
			}
		});
		recv.start();
		Thread.currentThread().sleep(wait);
		recv.stop();

		if ( numRecv > 0 ) {
    		boolean missing = false;
	    	for (int ii=0; ii<vals.length; ii++) {
		    	if ( vals[ii] == 0 ) {
			    	System.out.println("Missing " + ii);
				    missing = true;
    			}
	    	}
		    if ( !missing ) {
			    System.out.println("Got everything");			
    		}
		}
	}

}