package layersProxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import fsmProxy.*;
import layers.*;
import mensajesSIP.*;
import proxy.Proxy;

public class TransactionLayerProxy extends TransactionLayer{
	
	InetAddress addressClient;
	InetAddress addressServer;
	int portClient;
	int portServer;
	ClientStateProxy client;
	ServerStateProxy server;
	Transaction currentTransaction;
	private String myStringVias;
	private Timer timer;
	private TimerTask task;
	
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
		this.myStringVias = "localhost:" + Integer.toString(Proxy.puertoEscuchaProxy);
		this.timer = new Timer();
	}
	
	public String getMyStringVias() {
		return myStringVias;
	}

	public void sendACK(SIPMessage error) {
		ACKMessage ack = new ACKMessage();
		InviteMessage invite = ((UserLayerProxy)ul).getInboundInvite();
		
		ArrayList<String> vias = new ArrayList<String>();
		vias.add(myStringVias);
		String destination = invite.getDestination();
    	String toUri = invite.getToUri();
   	 	String fromUri = invite.getFromUri();
   	 	String callId = invite.getCallId();
   	 	String cSeqNumber = "1";
   	 	String cSeqStr = "ACK";
   	 	
   	 	ack.setDestination(destination);
   	 	ack.setCallId(callId);
	 	ack.setToUri(toUri);
	 	ack.setFromUri(fromUri);
	 	ack.setcSeqStr(cSeqStr);
	 	ack.setcSeqNumber(cSeqNumber);
	 	ack.setVias(vias);
   	 	
   	 	if(task == null) {
   	 		
   	 		task = new TimerTask() {
			
				@Override
				public void run() {
					client = ClientStateProxy.TERMINATED;
					task = null;
					ul.recvFromTransaction(error);
				}
   	 		};
		
			timer.schedule(task, 1000);
   	 	}
   	 	
   	 	try {
			sendToTransportClient(ack);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	public void sendError(SIPMessage error) {
			
		if(task == null) {
			
   	 		task = new TimerTask() {
   	 			
   	 			int numTimes = 0;
			
				@Override
				public void run() {
					if(numTimes <= 4) {
						try {
							transportLayer.sendToNetwork(addressServer, portServer, error);
							numTimes++;
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}else {
						client = ClientStateProxy.TERMINATED;
						task.cancel();
						task = null;
					}
				}
   	 		};
		
			timer.schedule(task, 200);
   	 	}
			
	}
	
	public void cancelTimer() {
		if(task != null) {
			task.cancel();
			task = null;
		}
	}
	
	

	@Override
	public void recvFromTransport(SIPMessage message) {
		String[] s;
		
		if(message instanceof RegisterMessage) {
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
