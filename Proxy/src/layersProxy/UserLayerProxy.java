package layersProxy;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import layers.*;
import mensajesSIP.*;
import proxy.Proxy;

public class UserLayerProxy extends UserLayer{
	
	private Map<String, String> locationService;
	private Map<String, String> allowedUsers;
	private InviteMessage inboundInvite;
	
	public UserLayerProxy() {
		super();
		this.locationService = new HashMap<String, String>();
		this.allowedUsers = new HashMap<String, String>();
		this.allowedUsers.put("sip:asdf1@dominio.es", "qwerty1");
		this.allowedUsers.put("sip:asdf2@dominio.es", "qwerty2");
		this.inboundInvite = null;
	}
	
	public InviteMessage getInboundInvite() {
		return inboundInvite;
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
		
		if(message instanceof InviteMessage) {
			String key = message.getToUri();
			String[] s = locationService.get(key).split(":");
			InetAddress address;
			try {
				address = InetAddress.getByName(s[0]);
				int port = Integer.valueOf(s[1]);
				((TransactionLayerProxy)transactionLayer).setAddressClient(address);
				((TransactionLayerProxy)transactionLayer).setPortClient(port);
				((TransactionLayerProxy)transactionLayer).recvFromUser(message);
				inboundInvite = (InviteMessage) message;
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else {
			((TransactionLayerProxy)transactionLayer).recvFromUser(message);
		}
		
	}

}