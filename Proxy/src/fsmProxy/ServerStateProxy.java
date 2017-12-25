package fsmProxy;

import java.io.IOException;

import mensajesSIP.*;
import layersProxy.*;
import layers.*;

public enum ServerStateProxy {
	
	
	PROCEEDING{
		@Override
		public ServerStateProxy processMessage(SIPMessage message, TransactionLayer tl) {
			
			if (message instanceof RingingMessage) {
				System.out.println("SERVER: PROCEEDING -> PROCEEDING");
				((TransactionLayerProxy) tl).sendToTransportResponse(message);
				return this;
			}else if (message instanceof NotFoundMessage || 
					  message instanceof ProxyAuthenticationMessage ||
					  message instanceof RequestTimeoutMessage ||
					  message instanceof BusyHereMessage ||
					  message instanceof ServiceUnavailableMessage) {
				System.out.println("SERVER: PROCEEDING -> COMPLETED");
				tl.sendError(message);
				return COMPLETED;
			}else if (message instanceof OKMessage) {
				System.out.println("SERVER: PROCEEDING -> TERMINATED");
				((TransactionLayerProxy) tl).sendToTransportResponse(message);
				return TERMINATED;
			}
			return this;
		}

	},
	COMPLETED{
		@Override
		public ServerStateProxy processMessage(SIPMessage message, TransactionLayer tl) {
			
			if(message instanceof ACKMessage) {
				System.out.println("SERVER: COMPLETED -> TERMINATED");
				tl.cancelTimer();
				return TERMINATED;
			}
			
			return this;
		}
		
	},
	TERMINATED{
		@Override
		public ServerStateProxy processMessage(SIPMessage message, TransactionLayer tl) {
			if (message instanceof InviteMessage) {
				System.out.println("SERVER: TERMINATED -> PROCEEDING");
				TryingMessage tryingMessage = (TryingMessage) SIPMessage.createResponse(SIPMessage._100_TRYING, message);
				((TransactionLayerProxy) tl).sendToTransportResponse(tryingMessage);
				tl.sendToUser(message);
				return PROCEEDING;
			}
			return this;
		}
		
	};
	
	public abstract ServerStateProxy processMessage(SIPMessage message, TransactionLayer tl);


}
