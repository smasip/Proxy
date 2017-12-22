package fsmProxy;

import java.io.IOException;
import java.util.ArrayList;

import mensajesSIP.*;
import layersProxy.*;
import layers.*;

public enum ClientStateProxy {
	CALLING {
		@Override
		public ClientStateProxy processMessage(SIPMessage message, TransactionLayer tl) {
			
			if(message instanceof InviteMessage) {
				try {
					System.out.println("CALLING -> CALLING");
					((TransactionLayerProxy)tl).sendToTransportRequest(message);
					return this;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else if (message instanceof RingingMessage) {
				System.out.println("CALLING -> PROCEEDING");
				tl.sendToUser(message);
				return PROCEEDING;
			}else if (message instanceof OKMessage) {
				System.out.println("CALLING -> TERMINATED");
				tl.sendToUser(message);
				return TERMINATED;
			}else if (message instanceof NotFoundMessage || 
					  message instanceof ProxyAuthenticationMessage ||
					  message instanceof RequestTimeoutMessage ||
					  message instanceof BusyHereMessage ||
					  message instanceof ServiceUnavailableMessage) 
			{
				System.out.println("CALLING -> COMPLETED");
				tl.sendACK(message);
				return COMPLETED;
			}
			return this;
		}

	},
	PROCEEDING{
		@Override
		public ClientStateProxy processMessage(SIPMessage message, TransactionLayer tl) {
			
			if (message instanceof TryingMessage || 
			    message instanceof RingingMessage) 
			{
				System.out.println("PROCEEDING -> PROCEEDING");
				tl.sendToUser(message);
				return this;
			}else if (message instanceof OKMessage) {
				System.out.println("PROCEEDING -> TERMINATED");
				tl.sendToUser(message);
				return TERMINATED;
			}else if (message instanceof NotFoundMessage || 
					  message instanceof ProxyAuthenticationMessage ||
					  message instanceof RequestTimeoutMessage ||
					  message instanceof BusyHereMessage ||
					  message instanceof ServiceUnavailableMessage) 
			{
				System.out.println("PROCEEDING -> COMPLETED");
				tl.sendACK(message);
				return COMPLETED;
			}
			
			return this;
		}

	},
	COMPLETED{
		@Override
		public ClientStateProxy processMessage(SIPMessage message, TransactionLayer tl) {
			if (message instanceof NotFoundMessage || 
				message instanceof ProxyAuthenticationMessage ||
				message instanceof RequestTimeoutMessage ||
				message instanceof BusyHereMessage ||
				message instanceof ServiceUnavailableMessage) 
			{
				System.out.println("COMPLETED -> TERMINATED");
				tl.sendACK(message);
			}
			
			return this;
			
		}
		
	},
	TERMINATED{
		@Override
		public ClientStateProxy processMessage(SIPMessage message, TransactionLayer tl) {
			return this;
		}
		
	};
	
	public abstract ClientStateProxy processMessage(SIPMessage message, TransactionLayer tl);

}
