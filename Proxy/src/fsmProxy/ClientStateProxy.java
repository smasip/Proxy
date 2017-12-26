package fsmProxy;

import mensajesSIP.*;
import layersProxy.*;
import layers.*;

public enum ClientStateProxy {
	CALLING {
		@Override
		public ClientStateProxy processMessage(SIPMessage message, TransactionLayer tl) {
			
			if (message instanceof RingingMessage) {
				System.out.println("CLIENT: CALLING -> PROCEEDING");
				tl.sendToUser(message);
				return PROCEEDING;
			}else if (message instanceof OKMessage) {
				System.out.println("CLIENT: CALLING -> TERMINATED");
				tl.sendToUser(message);
				return TERMINATED;
			}else if (message instanceof NotFoundMessage || 
					  message instanceof ProxyAuthenticationMessage ||
					  message instanceof RequestTimeoutMessage ||
					  message instanceof BusyHereMessage ||
					  message instanceof ServiceUnavailableMessage) 
			{
				System.out.println("CLIENT: CALLING -> COMPLETED");
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
			    message instanceof RingingMessage) {
				System.out.println("CLIENT: PROCEEDING -> PROCEEDING");
				tl.sendToUser(message);
				return this;
			}else if (message instanceof OKMessage) {
				System.out.println("CLIENT: PROCEEDING -> TERMINATED");
				tl.sendToUser(message);
				return TERMINATED;
			}else if (message instanceof NotFoundMessage || 
					  message instanceof ProxyAuthenticationMessage ||
					  message instanceof RequestTimeoutMessage ||
					  message instanceof BusyHereMessage ||
					  message instanceof ServiceUnavailableMessage) 
			{
				System.out.println("CLIENT: PROCEEDING -> COMPLETED");
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
				tl.sendACK(message);
			}
			
			return this;
			
		}
		
	},
	TERMINATED{
		@Override
		public ClientStateProxy processMessage(SIPMessage message, TransactionLayer tl) {
			if(message instanceof InviteMessage) {
				System.out.println("CLIENT: TERMINATED -> CALLING");
				tl.sendRequest(message);
				return CALLING;
			}
			return this;
		}
		
	};
	
	public abstract ClientStateProxy processMessage(SIPMessage message, TransactionLayer tl);

}
