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
	
	ClientStateProxy client;
	ServerStateProxy server;
	Transaction currentTransaction;
	private Timer timerServer;
	private TimerTask taskServer;
	private Timer timerClient;
	private TimerTask taskClient;
	private String currentCallId;
	private String destination;

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
	 	
	 	sendRequest(ack);
   	 	
   	 	if(taskClient == null) {
   	 		
   	 		ul.recvFromTransaction(error);
   	 		
   	 		taskClient = new TimerTask() {
			
				@Override
				public void run() {
					client = ClientStateProxy.TERMINATED;
					if((server == ServerStateProxy.TERMINATED) && (client == ClientStateProxy.TERMINATED)){
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
						sendResponse(error);
						numTimes++;
					}else {
						server = ServerStateProxy.TERMINATED;
						if((server == ServerStateProxy.TERMINATED) && (client == ClientStateProxy.TERMINATED)){
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
			sendResponse(response);
			return;
		}
		
		switch (currentTransaction) {
		
			case INVITE_TRANSACTION:
				
				if(!message.getCallId().equals(currentCallId)) {
					ServiceUnavailableMessage serviceUnavailable = (ServiceUnavailableMessage) SIPMessage.createResponse(
							SIPMessage._503_SERVICE_UNABAILABLE, message);
					sendResponse(serviceUnavailable);
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
					server = server.processMessage(message, this);
				}else{
					ul.recvFromTransaction(message);
				}
				
				break;
				
			default:
				break;
				
		}
		
	}
	
	public void recvRequestFromUser(SIPMessage request, InetAddress requestAddress, int requestPort) {
		
		this.requestAddress = requestAddress;
		this.requestPort = requestPort;
		
		switch (currentTransaction) {
		
			case INVITE_TRANSACTION:
				client = client.processMessage(request, this);
				break;
				
			case NO_TRANSACTION:
				sendRequest(request);
				break;
				
			default:
				break;
		}
	}
	
	public void recvResponseFromUser(SIPMessage response) {
		
		switch (currentTransaction) {
		
			case INVITE_TRANSACTION:
				server = server.processMessage(response, this);
				break;
				
			case NO_TRANSACTION:
				sendResponse(response);
				break;
				
			default:
				break;
		}
	}
		

}
