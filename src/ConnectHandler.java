
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.UUID;

public class ConnectHandler extends Thread {
    ;
    private Socket connectSocket;
    private UUID GUID;
    private Hashtable UHPT;
    private Hashtable UHRT;
    private Hashtable ipPortTable;


    public ConnectHandler(Socket connectSocket, Hashtable UHPT, Hashtable UHRT, Hashtable ipPortTable) {
        // init server tables
        this.connectSocket = connectSocket;
        this.GUID = UUID.randomUUID();
        this.UHPT = UHPT;
        this.UHRT = UHRT;
        this.ipPortTable = ipPortTable;
        UHPT.put(GUID, (int) (Math.random() * 100));
    }

    @Override
    public void run() {
        try {
            // receive information from clients
            InputStream inputStream = connectSocket.getInputStream();
            while (true) {
                byte[] data = new byte[1024];
                int len;
                while ((len = inputStream.read(data)) != -1) {
                    String resource = new String(data, 0, len);
                    System.out.println("From client: " + resource);
                    // handle information
                    handleResource(resource);
                }
            }
        } catch (Exception e) {
            System.out.println("Server Thread Error." + e);
            removeResource();
        }
    }

    private void handleResource(String resource) {
        // shore conbined information of ip and port of new client
        if (resource.length() != 33) {
            ipPortTable.put(GUID, resource);
        } else if (resource.length() == 33) {
            // handle shared md5
            if (resource.substring(0, 1).equals("s")) {
                String share = resource.substring(1);
                ArrayList list;
                if (UHRT.containsKey(share)) {
                    list = (ArrayList) UHRT.get(share);
                } else {
                    list = new ArrayList();
                }
                list.add(GUID);
                UHRT.put(share, list);
            } else if (resource.substring(0, 1).equals("g")) {
                // handle get file request
                String get = resource.substring(1);
                if (UHRT.containsKey(get)) {
                    ArrayList list = (ArrayList) UHRT.get(get);
                    // ip address and port for target client generated by selection algorithm
                    String ipPort = (String) ipPortTable.get(selection(list));
                    try {
                        // reply ip and port information
                        Writer writer = new OutputStreamWriter(connectSocket.getOutputStream(), "UTF-8");
                        writer.write(ipPort);
                        writer.flush();
                        System.out.println("server replies: " + ipPort);
                    } catch (Exception e) {
                        System.out.println("Resource Handle Error." + e);
                    }
                }
            }
        }
    }

    private void removeResource() {
        try {
            // remove GUID of offline client in UHPT and UHRT
            UHPT.remove(GUID);
            Enumeration key = UHRT.keys();
            while (key.hasMoreElements()) {
                String md5 = (String) key.nextElement();
                ArrayList list = (ArrayList) UHRT.get(md5);
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).equals(GUID)) {
                        list.remove(i);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Resource Remove Error." + e);
        }
    }

    // selection function
    private UUID selection(ArrayList list) {
        int distance = 100;
        // GUID of target client
        UUID GUIDt = null;
        for (int i = 0; i < list.size(); i++) {
            if (distance > (int) UHPT.get(list.get(i))) {
                distance = (int) UHPT.get(list.get(i));
                GUIDt = (UUID) list.get(i);
            }
        }
        return GUIDt;
    }
}

