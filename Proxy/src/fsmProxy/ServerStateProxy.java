package fsmProxy;

import java.io.IOException;

import mensajesSIP.*;
import layersProxy.*;
import layers.*;

public enum ServerStateProxy {
	
	
	PROCEEDING{
		@Override
		public ServerStateProxy processMessage(SIPMessage message, TransactionLayer tl) {
			System.out.println("PROCEEDING");
			System.out.println(message.toStringMessage());
			if (message instanceof InviteMessage) {
				try {
					TryingMessage tryingMessage = (TryingMessage) SIPMessage.createResponse(SIPMessage._100_TRYING, message);
					((TransactionLayerProxy) tl).sendToTransportServer(tryingMessage);
					tl.sendToUser(message);
					return this;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else if (message instanceof RingingMessage) {
				try {
					((TransactionLayerProxy) tl).sendToTransportServer(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return this;
			}else if (message instanceof NotFoundMessage || 
					  message instanceof ProxyAuthenticationMessage ||
					  message instanceof RequestTimeoutMessage ||
					  message instanceof BusyHereMessage ||
					  message instanceof ServiceUnavailableMessage) {
				// Falta send response
				return COMPLETED;
			}else if (message instanceof OKMessage) {
				try {
					((TransactionLayerProxy) tl).sendToTransportServer(message);
					return TERMINATED;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return this;
		}

	},
	COMPLETED{
		@Override
		public ServerStateProxy processMessage(SIPMessage message, TransactionLayer tl) {
			return TERMINATED;
		}
		
	},
	TERMINATED{
		@Override
		public ServerStateProxy processMessage(SIPMessage message, TransactionLayer tl) {
			System.out.println("TERMINATED");
			System.out.println(message.toStringMessage());
			return this;
		}
		
	};
	
	public abstract ServerStateProxy processMessage(SIPMessage message, TransactionLayer tl);


}
