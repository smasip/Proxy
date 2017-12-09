package layersProxy;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import fsmProxy.*;
import layers.*;
import mensajesSIP.*;

public class TransactionLayerProxy extends TransactionLayer{
	
	InetAddress addressClient;
	InetAddress addressServer;
	int portClient;
	int portServer;
	ClientStateProxy client;
	ServerStateProxy server;
	String currentCallID;
	Transaction currentTransaction;
	
	public InetAddress getAddressClient() {
		return addressClient;
	}

	public void setAddressClient(InetAddress addressClient) {
		this.addressClient = addressClient;
	}

	public int getPortClient() {
		return portClient;
	}

	public void setPortClient(int portClient) {
		this.portClient = portClient;
	}

	public TransactionLayerProxy() {
		super();
		this.client = ClientStateProxy.TERMINATED;
		this.server = ServerStateProxy.TERMINATED;
		this.currentTransaction = Transaction.NO_TRANSACTION;
	}

	@Override
	public void recvFromTransport(SIPMessage message) {
		String[] s;
		
		if(message instanceof RegisterMessage) {
			System.out.println(message.toStringMessage());
			s = message.getVias().get(0).split(":");
			InetAddress registerIP;
			int registerPort;
			try {
				registerIP = InetAddress.getByName(s[0]);
				registerPort = Integer.valueOf(s[1]);
				SIPMessage response = ((UserLayerProxy)ul).registerUser((RegisterMessage)message);
				transportLayer.sendToNetwork(registerIP, registerPort, response);
				return;
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		switch (currentTransaction) {
		
			case INVITE_TRANSACTION:
				client = client.processMessage(message, this);
				break;
				
			case NO_TRANSACTION:
				
				if(message instanceof InviteMessage) {
					s = message.getVias().get(0).split(":");
					try {
						addressServer = InetAddress.getByName(s[0]);
						portServer = Integer.valueOf(s[1]);
						currentTransaction = Transaction.INVITE_TRANSACTION;
						//currentCallID =
						server = ServerStateProxy.PROCEEDING;
						server = server.processMessage(message, this);
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				break;
				
			default:
				break;
				
		}
		
	}

	public void recvFromUser(SIPMessage message) {
		
		switch (currentTransaction) {
		
			case INVITE_TRANSACTION:
				
				if(message instanceof InviteMessage) {
					client = ClientStateProxy.CALLING;
					client = client.processMessage(message, this);
				}else {
					server = server.processMessage(message, this);
					if(server == ServerStateProxy.TERMINATED && client == ClientStateProxy.TERMINATED){
						currentTransaction = Transaction.NO_TRANSACTION;
					}
				}
				
				break;
				
			default:
				break;
		}
	}
		
	
	
	public void sendToTransportClient(SIPMessage message) throws IOException {
		transportLayer.sendToNetwork(addressClient, portClient, message);
	}
	
	public void sendToTransportServer(SIPMessage message) throws IOException {
		transportLayer.sendToNetwork(addressServer, portServer, message);
	}

}
