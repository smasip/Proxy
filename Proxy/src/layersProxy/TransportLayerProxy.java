package layersProxy;

import java.io.IOException;
import java.net.DatagramPacket;

import layers.*;
import mensajesSIP.SIPException;
import mensajesSIP.SIPMessage;

public class TransportLayerProxy extends TransportLayer{

	@Override
	public void recvFromNetwork(){
		// TODO Auto-generated method stub
		byte[] buf = new byte[1024];
    	DatagramPacket p = new DatagramPacket(buf, buf.length);
    	SIPMessage message;
    	while(true) {
    		try {
    			datagramSocket.receive(p);
    			message = SIPMessage.parseMessage(new String(p.getData()));
    			transactionLayer.recvFromTransport(message);
				p.setData(buf, 0, buf.length);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SIPException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
		
	}
	
	

}
