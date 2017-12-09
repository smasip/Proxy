package proxy;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import mensajesSIP.*;
import layers.*;
import layersProxy.TransactionLayerProxy;
import layersProxy.TransportLayerProxy;
import layersProxy.UserLayerProxy;

public class Proxy {
	
	private static DatagramSocket datagramSocket;
	private static int puertoEscuchaProxy;
	private static boolean debug;
	private static boolean lr;
	private static UserLayerProxy ul;
	private static TransactionLayerProxy transactionLayer;
	private static TransportLayer transportLayer;
	private static DatagramPacket p;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if(args.length != 3) {
			System.out.println("Incorrect number of arguments");
			System.exit(0);
		}
		
		puertoEscuchaProxy = Integer.valueOf(args[0]);
		lr = Boolean.valueOf(args[1]);
		debug = Boolean.getBoolean(args[2]);
		
		try {
			datagramSocket = new DatagramSocket(puertoEscuchaProxy);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Creation of DatagramSocket failed");
			System.exit(0);
		}
		
		ul = new UserLayerProxy();
		transactionLayer = new TransactionLayerProxy();
		transportLayer = new TransportLayerProxy();
		
		ul.setTransactionLayer(transactionLayer);
		
		transactionLayer.setTransportLayer(transportLayer);
		transactionLayer.setUl(ul);
		
		transportLayer.setTransactionLayer(transactionLayer);
		transportLayer.setDatagramSocket(datagramSocket);
		transportLayer.recvFromNetwork();
		
	}
	

}