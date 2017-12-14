package fsmProxy;

import java.io.IOException;

import mensajesSIP.*;
import layersProxy.*;
import layers.*;

public enum ServerStateProxy {
	
	
	PROCEEDING{
		@Override
		public ServerStateProxy processMessage(SIPMessage message, TransactionLayer tl) {
			
			if (message instanceof InviteMessage) {
				try {
					System.out.println("PROCEEDING -> PROCEEDING");
					TryingMessage tryingMessage = (TryingMessage) SIPMessage.createResponse(SIPMessage._100_TRYING, message);
					((TransactionLayerProxy) tl).sendToTransportServer(tryingMessage);
					tl.sendToUser(message);
					return this;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else if (message instanceof RingingMessage) {
				try {
					System.out.println("PROCEEDING -> PROCEEDING");
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
				System.out.println("PROCEEDING -> COMPLETED");
				tl.sendError(message);
				return COMPLETED;
			}else if (message instanceof OKMessage) {
				try {
					System.out.println("PROCEEDING -> TERMINATED");
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
			
			if(message instanceof ACKMessage) {
				System.out.println("COMPLETED -> TERMINATED");
				tl.cancelTimer();
				return TERMINATED;
			}
			
			return this;
		}
		
	},
	TERMINATED{
		@Override
		public ServerStateProxy processMessage(SIPMessage message, TransactionLayer tl) {
			return this;
		}
		
	};
	
	public abstract ServerStateProxy processMessage(SIPMessage message, TransactionLayer tl);


}
