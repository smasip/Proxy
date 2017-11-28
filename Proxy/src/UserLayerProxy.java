
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import layers.*;
import mensajesSIP.*;

public class UserLayerProxy extends UserLayer{


	@Override
	public void recvFromTransaction(SIPMessage message) {
		if(message instanceof InviteMessage) {
			String key = message.getToUri();
			String[] s = Proxy.locationService.get(key).split(":");
			InetAddress address;
			try {
				address = InetAddress.getByName(s[0]);
				int port = Integer.valueOf(s[1]);
				byte[] buf = new byte[1024];
				DatagramPacket p = new DatagramPacket(buf, buf.length, address, port);
				((TransactionLayerProxy)transactionLayer).recvFromUser(message, p);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else {
			((TransactionLayerProxy)transactionLayer).recvFromUser(message, null);
		}
		
	}

}