package ly.readon.android_ssdp;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;

/**
 * @author Bernd Verst(@berndverst)
 */
public class UPnPDiscovery extends AsyncTask
{
    HashSet<String> addresses = new HashSet<>();
    Context ctx;

    public UPnPDiscovery(Context context) {
        ctx = context;
    }


    @Override
    protected Object doInBackground(Object[] params) {

        WifiManager wifi = (WifiManager)ctx.getSystemService( ctx.getApplicationContext().WIFI_SERVICE );

        if(wifi != null) {

            WifiManager.MulticastLock lock = wifi.createMulticastLock("The Lock");
            lock.acquire();

            DatagramSocket socket = null;

            try {

                InetAddress group = InetAddress.getByName("239.255.255.250");
                int port = 1900;
                String query =
                        "M-SEARCH * HTTP/1.1\r\n" +
                                "HOST: 239.255.255.250:1900\r\n"+
                                "MAN: \"ssdp:discover\"\r\n"+
                                "MX: 1\r\n"+
                                "ST: urn:schemas-upnp-org:service:AVTransport:1\r\n"+  // Use for Sonos
                                //"ST: ssdp:all\r\n"+  // Use this for all UPnP Devices
                                "\r\n";

                socket = new DatagramSocket(port);
                socket.setReuseAddress(true);

                DatagramPacket dgram = new DatagramPacket(query.getBytes(), query.length(),
                        group, port);
                socket.send(dgram);

                long time = System.currentTimeMillis();
                long curTime = System.currentTimeMillis();

                // Let's consider all the responses we can get in 1 second
                while (curTime - time < 1000) {
                    DatagramPacket p = new DatagramPacket(new byte[12], 12);
                    socket.receive(p);

                    String s = new String(p.getData(), 0, p.getLength());
                    if (s.toUpperCase().equals("HTTP/1.1 200")) {
                        addresses.add(p.getAddress().getHostAddress());
                    }

                    curTime = System.currentTimeMillis();
                }

            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                socket.close();
            }
            lock.release();
        }
        return null;
    }

    public static String[] discoverDevices(Context ctx) {
        UPnPDiscovery discover = new UPnPDiscovery(ctx);
        discover.execute();
        try {
            Thread.sleep(1500);
            return discover.addresses.toArray(new String[discover.addresses.size()]);
        } catch (InterruptedException e) {
            return null;
        }
    }
}
