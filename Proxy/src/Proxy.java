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

public class Proxy {
	
	private static DatagramSocket datagramSocket;
	private static int puertoEscuchaProxy;
	private static boolean debug;
	private static boolean lr;
	public static Map<String, String> locationService = new HashMap<String, String>();
	private static UserLayerProxy ul;
	private static TransactionLayerProxy transactionLayer;
	private static TransportLayer transportLayer;
	private static DatagramPacket p;
	
	private static void registerResponse(RegisterMessage message) {
		String[] s = message.getVias().get(0).split(":");
		InetAddress registerIP;
		int registerPort;
		try {
			registerIP = InetAddress.getByName(s[0]);
			registerPort = Integer.valueOf(s[1]);
			p.setAddress(registerIP);
			p.setPort(registerPort);
			OKMessage ok = (OKMessage) SIPMessage.createResponse(SIPMessage._200_OK, message);
			System.out.println(ok.toStringMessage());
			p.setData(ok.toStringMessage().getBytes());
			datagramSocket.send(p);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

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
		transportLayer = new TransportLayer();
		
		ul.setTransactionLayer(transactionLayer);
		
		transactionLayer.setTransportLayer(transportLayer);
		transactionLayer.setUl(ul);
		
		transportLayer.setTransactionLayer(transactionLayer);
		transportLayer.setDatagramSocket(datagramSocket);
		
		byte[] buf = new byte[1024];
		p = new DatagramPacket(buf, buf.length);
		
		SIPMessage message;
		
		while(true) {
			try {
				datagramSocket.receive(p);
				message = SIPMessage.parseMessage(new String(p.getData()));
				if (message instanceof RegisterMessage) {
					System.out.println(message.toStringMessage());
					locationService.put(message.getToUri(), ((RegisterMessage)message).getContact().split("@")[1]);
					registerResponse((RegisterMessage)message);
				}else{
					transportLayer.recvFromNetwork(p);
				}
				
			} catch (IOException | SIPException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Reception failed");	
			}
			p.setData(buf, 0, buf.length);
		}

	}
	

}
