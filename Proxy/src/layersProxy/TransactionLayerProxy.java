package layersProxy;

import fsm.ClientFSM;
import fsm.ServerFSM;
import layers.*;
import mensajesSIP.*;
import proxy.Proxy;

public class TransactionLayerProxy extends TransactionLayer{


	public TransactionLayerProxy() {
		super();
		this.currentTransaction = Transaction.NO_TRANSACTION;
		this.callId = null;
		this.destination = null;
		this.myVias = Proxy.getMyVias();
		this.client = new ClientFSM(this);
		this.server = new ServerFSM(this) {
			
			@Override
			public void onInvite(InviteMessage invite) {
				TryingMessage tryingMessage = (TryingMessage) SIPMessage.createResponse(SIPMessage._100_TRYING, invite);
				transactionLayer.sendResponse(tryingMessage);
				transactionLayer.sendToUser(invite);
				
			}
		};
	}
	
	@Override
	public void resetLayer() {
		if(client.isTerminated() && server.isTerminated() && (currentTransaction == Transaction.INVITE_TRANSACTION)) {
			currentTransaction = Transaction.NO_TRANSACTION;
			callId = null;
			destination = null;
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
				
				if(!message.getCallId().equals(callId)) {
					ServiceUnavailableMessage serviceUnavailable = (ServiceUnavailableMessage) SIPMessage.createResponse(
							SIPMessage._503_SERVICE_UNABAILABLE, message);
					sendResponse(serviceUnavailable);
				}else if(message instanceof ACKMessage) {
					server.processMessage(message);
				}else{
					client.processMessage(message);
				}
				
				break;
				
			case NO_TRANSACTION:
				
				if(message instanceof InviteMessage) {
					currentTransaction = Transaction.INVITE_TRANSACTION;
					callId = message.getCallId();
					destination = ((InviteMessage)message).getDestination();
					server.processMessage(message);
				}else{
					ul.recvFromTransaction(message);
				}
				
				break;
				
			default:
				break;
				
		}
		
	}
	
	@Override
	public void recvRequestFromUser(SIPMessage request) {
		
		switch (currentTransaction) {
		
			case INVITE_TRANSACTION:
				client.processMessage(request);
				break;
				
			case NO_TRANSACTION:
				sendRequest(request);
				break;
				
			default:
				break;
		}
		
	}

	
}
