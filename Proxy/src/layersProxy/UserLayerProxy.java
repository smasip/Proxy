package layersProxy;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import layers.*;
import mensajesSIP.*;
import proxy.Proxy;
import servlets.impl.SipServletRequest;
import servlets.interfaces.SipServletRequestInterface;
import servlets.personalServlets.GenericSIPServlet;

public class UserLayerProxy extends UserLayer{
	
	private String currentCallId;
	private boolean callInProgress;
	private Transaction currentTransaction;
	
	public void setParameters(String currentCallId, boolean callInProgress, Transaction currentTransaction) {
		this.currentCallId = currentCallId;
		this.callInProgress = callInProgress;
		this.currentTransaction = currentTransaction;
	}
	
	public UserLayerProxy() {
		super();
		this.currentTransaction = Transaction.NO_TRANSACTION;
		this.callInProgress = false;
	}
	

	public SIPMessage registerUser(RegisterMessage message) {
		if(Proxy.allowedUsers.containsKey(message.getToUri())) {
			Proxy.locationService.put(message.getToUri(), message.getContact().split("@")[1]);
			OKMessage ok = (OKMessage) SIPMessage.createResponse(SIPMessage._200_OK, message);
			return ok;
		}else {
			NotFoundMessage error = (NotFoundMessage) SIPMessage.createResponse(SIPMessage._404_NOT_FOUND, message);
			return error;
		}
	}


	@Override
	public void recvFromTransaction(SIPMessage message) {
		
		String[] s;
		String key;
		InetAddress requestAddress;
		int resquestPort;
		ArrayList<String> newVias;
		
		switch (currentTransaction) {
		
			case NO_TRANSACTION:
				
				if(callInProgress) {
					
					if(message.getCallId().equals(currentCallId)) {
						
						if(message instanceof ACKMessage) {
							
							newVias = message.getVias();
							newVias.add(0, Proxy.getMyStringVias());
							message.setVias(newVias);
							((TransactionLayerProxy)transactionLayer).recvResponseFromUser(message);
							
						}else if (message instanceof ByeMessage){
							
							key = message.getToUri();
							s = Proxy.locationService.get(key).split(":");
							
							try {

								currentTransaction = Transaction.BYE_TRANSACTION;
								requestAddress = InetAddress.getByName(s[0]);
								resquestPort = Integer.valueOf(s[1]);
								newVias = message.getVias();
								newVias.add(0, Proxy.getMyStringVias());
								message.setVias(newVias);
								((TransactionLayerProxy)transactionLayer).recvRequestFromUser(message, requestAddress, resquestPort);
								
							} catch (UnknownHostException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		
						}
						
					}else {
						ServiceUnavailableMessage serviceUnavailable = (ServiceUnavailableMessage) SIPMessage.createResponse(
								SIPMessage._503_SERVICE_UNABAILABLE, message);
						((TransactionLayerProxy)transactionLayer).recvResponseFromUser(serviceUnavailable);
					}
					
				}else if(message instanceof InviteMessage) {
					
					SipServletRequestInterface request = new SipServletRequest(
							(TransactionLayerProxy)transactionLayer, 
							(InviteMessage) message);
					String caller = request.getCallerURI();
					
					GenericSIPServlet servlet;
						try {
							if(caller.equals("sip:asdf1@dominio.es")) {
								servlet = (GenericSIPServlet) Class.forName("servlets.personalServlets.SIPServlet1").newInstance();
							}else {
								servlet = (GenericSIPServlet) Class.forName("servlets.personalServlets.SIPServlet2").newInstance();
							}
							servlet.doInvite(request);
						} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
				}
				
				break;
				
			case INVITE_TRANSACTION:
				
				if(message instanceof OKMessage) {
					if(!Proxy.lr) {
						callInProgress = false;
						currentCallId = null;
					}
					currentTransaction = Transaction.NO_TRANSACTION;
				}else if((message instanceof BusyHereMessage) || (message instanceof RequestTimeoutMessage)) {
					currentTransaction = Transaction.NO_TRANSACTION;
					callInProgress = false;
					currentCallId = null;
				}
				
				newVias = message.getVias();
				newVias.remove(0);
				((TransactionLayerProxy)transactionLayer).recvResponseFromUser(message);
				
				break;
				
			case BYE_TRANSACTION:
				
				if(message instanceof OKMessage) {
					currentTransaction = Transaction.NO_TRANSACTION;
					callInProgress = false;
					currentCallId = null;
					newVias = message.getVias();
					newVias.remove(0);
					((TransactionLayerProxy)transactionLayer).recvResponseFromUser(message);
				}
				
				break;
				
			default:
				break;
		}
		
		
		
	}

}