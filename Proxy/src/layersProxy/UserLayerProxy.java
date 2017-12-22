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

public class UserLayerProxy extends UserLayer{
	
	private Map<String, String> locationService;
	private Map<String, String> allowedUsers;
	private String currentCallId;
	private boolean callInProgress;
	private boolean looseRouting;
	private Transaction currentTransaction;
	String myRoute;
	
	public UserLayerProxy(boolean looseRouting) {
		super();
		this.locationService = new HashMap<String, String>();
		this.allowedUsers = new HashMap<String, String>();
		this.allowedUsers.put("sip:asdf1@dominio.es", "qwerty1");
		this.allowedUsers.put("sip:asdf2@dominio.es", "qwerty2");
		this.looseRouting = looseRouting;
		this.callInProgress = false;
		this.myRoute = "<sip:proxy.dominio.com>";
	}
	

	public SIPMessage registerUser(RegisterMessage message) {
		if(allowedUsers.containsKey(message.getToUri())) {
			locationService.put(message.getToUri(), message.getContact().split("@")[1]);
			OKMessage ok = (OKMessage) SIPMessage.createResponse(SIPMessage._200_OK, message);
			return ok;
		}else {
			NotFoundMessage error = (NotFoundMessage) SIPMessage.createResponse(SIPMessage._404_NOT_FOUND, message);
			return error;
		}
	}


	@Override
	public void recvFromTransaction(SIPMessage message) {
		
		String key;
		String[] s;
		InetAddress address;
		int port;
		ArrayList<String> newVias;
		
		switch (currentTransaction) {
		
			case NO_TRANSACTION:
				
				if(callInProgress) {
					
					if(message.getCallId().equals(currentCallId)) {
						
						if(message instanceof ACKMessage) {
							
							newVias = message.getVias();
							newVias.add(0, Proxy.getMyStringVias());
							message.setVias(newVias);
							((TransactionLayerProxy)transactionLayer).recvFromUser(message);
							
						}else if (message instanceof ByeMessage){
							
							key = message.getToUri();
							s = locationService.get(key).split(":");
							
							try {
								
								address = InetAddress.getByName(s[0]);
								port = Integer.valueOf(s[1]);
								((TransactionLayerProxy)transactionLayer).setRequestAddress(address);
								((TransactionLayerProxy)transactionLayer).setRequestPort(port);
								newVias = message.getVias();
								newVias.add(0, Proxy.getMyStringVias());
								message.setVias(newVias);
								((TransactionLayerProxy)transactionLayer).recvFromUser(message);
								
							} catch (UnknownHostException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		
						}
						
					}else {
						ServiceUnavailableMessage serviceUnavailable = (ServiceUnavailableMessage) SIPMessage.createResponse(
								SIPMessage._503_SERVICE_UNABAILABLE, message);
						((TransactionLayerProxy)transactionLayer).recvFromUser(serviceUnavailable);
					}
					
				}else if(message instanceof InviteMessage) {
					
					key = message.getToUri();
					s = locationService.get(key).split(":");
					try {
						
						callInProgress = true;
						currentTransaction = Transaction.INVITE_TRANSACTION;
						
						address = InetAddress.getByName(s[0]);
						port = Integer.valueOf(s[1]);
						((TransactionLayerProxy)transactionLayer).setRequestAddress(address);
						((TransactionLayerProxy)transactionLayer).setRequestPort(port);
						newVias = message.getVias();
						newVias.add(0, Proxy.getMyStringVias());
						message.setVias(newVias);
						if(looseRouting) {
							((InviteMessage)message).setRecordRoute(myRoute);
						}
						
						((TransactionLayerProxy)transactionLayer).recvFromUser(message);
						
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				break;
				
			case INVITE_TRANSACTION:
				
				if(message instanceof OKMessage) {
					if(!looseRouting) {
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
				((TransactionLayerProxy)transactionLayer).recvFromUser(message);
				
				break;
				
			case BYE_TRANSACTION:
				
				if(message instanceof OKMessage) {
					currentTransaction = Transaction.NO_TRANSACTION;
					callInProgress = false;
					currentCallId = null;
					newVias = message.getVias();
					newVias.remove(0);
					((TransactionLayerProxy)transactionLayer).recvFromUser(message);
				}
				
				break;
				
			default:
				break;
		}
		
		
		
	}

}