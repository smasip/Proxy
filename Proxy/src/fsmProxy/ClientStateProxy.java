package fsmProxy;

import java.io.IOException;

import mensajesSIP.*;
import layersProxy.*;
import layers.*;

public enum ClientStateProxy {
	CALLING {
		@Override
		public ClientStateProxy processMessage(SIPMessage message, TransactionLayer tl) {
			System.out.println("CALLING");
			System.out.println(message.toStringMessage());
			if(message instanceof InviteMessage) {
				try {
					((TransactionLayerProxy)tl).sendToTransportClient(message);
					return this;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else if (message instanceof RingingMessage) {
				tl.sendToUser(message);
				return PROCEEDING;
			}else if (message instanceof OKMessage) {
				tl.sendToUser(message);
				return TERMINATED;
			}else if (message instanceof NotFoundMessage || 
					  message instanceof ProxyAuthenticationMessage ||
					  message instanceof RequestTimeoutMessage ||
					  message instanceof BusyHereMessage ||
					  message instanceof ServiceUnavailableMessage) {
				return COMPLETED;
			}
			return this;
		}

	},
	PROCEEDING{
		@Override
		public ClientStateProxy processMessage(SIPMessage message, TransactionLayer tl) {
			System.out.println("PROCEEDING");
			System.out.println(message.toStringMessage());
			if (message instanceof TryingMessage || 
			    message instanceof RingingMessage) {
				tl.sendToUser(message);
				return this;
			}else if (message instanceof OKMessage) {
				tl.sendToUser(message);
				return TERMINATED;
			}else if (message instanceof NotFoundMessage || 
					  message instanceof ProxyAuthenticationMessage ||
					  message instanceof RequestTimeoutMessage ||
					  message instanceof BusyHereMessage ||
					  message instanceof ServiceUnavailableMessage) {
				// Falta send ACK y resp to TU
				return COMPLETED;
			}
			return this;
		}

	},
	COMPLETED{
		@Override
		public ClientStateProxy processMessage(SIPMessage message, TransactionLayer tl) {
			return TERMINATED;
		}
		
	},
	TERMINATED{
		@Override
		public ClientStateProxy processMessage(SIPMessage message, TransactionLayer tl) {
			System.out.println("TERMINATED");
			System.out.println(message.toStringMessage());
			return this;
		}
		
	};
	
	public abstract ClientStateProxy processMessage(SIPMessage message, TransactionLayer tl);

}
