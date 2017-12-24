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
	
	InetAddress requestAddress;
	int requestPort;
	ClientStateProxy client;
	ServerStateProxy server;
	Transaction currentTransaction;
	private Timer timerServer;
	private TimerTask taskServer;
	private Timer timerClient;
	private TimerTask taskClient;
	private String currentCallId;
	private String destination;

	public void setRequestAddress(InetAddress requestAddress) {
		this.requestAddress = requestAddress;
	}

	public void setRequestPort(int requestPort) {
		this.requestPort = requestPort;
	}


	public TransactionLayerProxy() {
		super();
		this.client = ClientStateProxy.TERMINATED;
		this.server = ServerStateProxy.TERMINATED;
		this.currentTransaction = Transaction.NO_TRANSACTION;
		this.timerServer = new Timer();
		this.timerClient = new Timer();
		this.taskServer = null;
		this.taskClient = null;
		this.currentCallId = null;
		this.destination = null;
	}
	

	public void sendACK(SIPMessage error) {
		
		ACKMessage ack = new ACKMessage();
		
		ArrayList<String> vias = new ArrayList<String>();
		vias.add(Proxy.getMyStringVias());
   	 	
   	 	ack.setDestination(destination);
   	 	ack.setVias(vias);
   	 	ack.setCallId(currentCallId);
	 	ack.setToUri(error.getToUri());
	 	ack.setFromUri(error.getFromUri());
	 	ack.setcSeqStr("ACK");
	 	ack.setcSeqNumber("1");
	 	
	 	sendToTransportRequest(ack);
   	 	
   	 	if(taskClient == null) {
   	 		
   	 		ul.recvFromTransaction(error);
   	 		
   	 		taskClient = new TimerTask() {
			
				@Override
				public void run() {
					client = ClientStateProxy.TERMINATED;
					if(server == ServerStateProxy.TERMINATED && client == ClientStateProxy.TERMINATED){
						currentTransaction = Transaction.NO_TRANSACTION;
						currentCallId = null;
						destination = null;
					}
					System.out.println("CLIENT: COMPLETED -> TERMINATED");
					taskClient.cancel();
					taskClient = null;
				}
   	 		};
		
			timerClient.schedule(taskClient, 1000);
   	 	}
   	 	
		
	}
	
	
	public void sendError(SIPMessage error) {
			
		if(taskServer == null) {
			
   	 		taskServer = new TimerTask() {
   	 			
   	 			int numTimes = 0;
			
				@Override
				public void run() {
					if(numTimes < 4) {
						sendToTransportResponse(error);
						numTimes++;
					}else {
						server = ServerStateProxy.TERMINATED;
						if(server == ServerStateProxy.TERMINATED && client == ClientStateProxy.TERMINATED){
							currentTransaction = Transaction.NO_TRANSACTION;
							currentCallId = null;
							destination = null;
						}
						taskServer.cancel();
						taskServer = null;
					}
				}
   	 		};
		
			timerServer.schedule(taskServer, 200);
   	 	}
			
	}
	
	public void cancelTimer() {
		if(taskServer != null) {
			taskServer.cancel();
			taskServer = null;
		}
	}
	
	

	@Override
	public void recvFromTransport(SIPMessage message) {
		
		if(message instanceof RegisterMessage) {
			SIPMessage response = ((UserLayerProxy)ul).registerUser((RegisterMessage)message);
			sendToTransportResponse(response);
			return;
		}
		
		switch (currentTransaction) {
		
			case INVITE_TRANSACTION:
				
				if(!message.getCallId().equals(currentCallId)) {
					ServiceUnavailableMessage serviceUnavailable = (ServiceUnavailableMessage) SIPMessage.createResponse(
							SIPMessage._503_SERVICE_UNABAILABLE, message);
					sendToTransportResponse(serviceUnavailable);
				}else if(message instanceof ACKMessage) {
					server = server.processMessage(message, this);
				}else{
					client = client.processMessage(message, this);
				}
				
				if((server == ServerStateProxy.TERMINATED) && (client == ClientStateProxy.TERMINATED)){
					currentTransaction = Transaction.NO_TRANSACTION;
					currentCallId = null;
					destination = null;
				}
				
				break;
				
			case NO_TRANSACTION:
				
				if(message instanceof InviteMessage) {
					currentTransaction = Transaction.INVITE_TRANSACTION;
					currentCallId = message.getCallId();
					destination = ((InviteMessage)message).getDestination();
					server = ServerStateProxy.PROCEEDING;
					server = server.processMessage(message, this);
				}else if(message instanceof ACKMessage) {
					ul.recvFromTransaction(message);
				}else if(message instanceof ByeMessage) {
					currentTransaction = Transaction.BYE_TRANSACTION;
					currentCallId = message.getCallId();
					ul.recvFromTransaction(message);
				}
				
				break;
				
				
			case BYE_TRANSACTION:
				if(message.getCallId().equals(currentCallId) && (message instanceof OKMessage)) {
					ul.recvFromTransaction(message);
				}else {
					ServiceUnavailableMessage serviceUnavailable = (ServiceUnavailableMessage) SIPMessage.createResponse(
							SIPMessage._503_SERVICE_UNABAILABLE, message);
					sendToTransportResponse(serviceUnavailable);
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
				}
				
				break;
				
			case BYE_TRANSACTION:
				if(message instanceof ByeMessage) {
					sendToTransportRequest(message);
				}else if(message instanceof OKMessage) {
					currentTransaction = Transaction.NO_TRANSACTION;
					currentCallId = null;
					sendToTransportResponse(message);
				}
				break;
			
			case NO_TRANSACTION:
				if(message instanceof ACKMessage) {
					sendToTransportRequest(message);
				}
				break;	
				
			default:
				break;
		}
	}
		
	
	
	public void sendToTransportRequest(SIPMessage message){
		try {
			transportLayer.sendToNetwork(requestAddress, requestPort, message);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void sendToTransportResponse(SIPMessage message){
		String s[] = message.getVias().get(0).split(":");
		try {
			transportLayer.sendToNetwork(InetAddress.getByName(s[0]), Integer.valueOf(s[1]), message);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
