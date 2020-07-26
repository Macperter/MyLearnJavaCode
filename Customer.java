package tcp_task_new;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.rmi.server.UID;
import java.util.Scanner;

public class Customer {
    public static void main(String[] args) {
        Scanner obj = new Scanner(System.in);
        Socket socket = null;

        /*
         * System.out.println("please input UID"); Socket socket = null; try { socket =
         * new Socket("127.0.0.1", 9909); } catch (UnknownHostException e) { // TODO
         * Auto-generated catch block e.printStackTrace(); } catch (IOException e) { //
         * TODO Auto-generated catch block e.printStackTrace(); }
         */
        boolean t = true;
        while (t) {
            System.out.println("please input UID");
            String str = obj.nextLine().trim();
            try {
                socket = new Socket("127.0.0.1", 9909);
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                InputStream in = socket.getInputStream();
                OutputStream ou = socket.getOutputStream();
                ou.write(str.getBytes());
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                int len = 0;
                byte[] by = new byte[1 << 10];
                if ((len = in.read(by)) != -1) {
                    bo.write(by, 0, len);
                }
                String tmp = bo.toString().trim();
                if (tmp.equals("[403]登录成功")) {
                    System.out.println(tmp);
                    t = false;
                    Terminal tl = new Terminal(str, socket);
                    tl.init();
                } else {
                    System.out.println(tmp);
                }
            } catch (Exception e) {
                // TODO: handle exception
            }

        }
        obj.close();
    }
};

class Terminal {
    private String UID = "";
    private Socket socket = null;
    private InputStream in = null;
    private OutputStream ou = null;
    private CreateThread ct = null;

    public Terminal(String UID, Socket socket) {
        this.UID = UID;
        this.socket = socket;
    }

    public void init() throws IOException {
        // jsocket = new Socket("127.0.0.1", 9909);
        in = socket.getInputStream();
        ou = socket.getOutputStream();
        try {
            ct = new CreateThread(this);
            new Thread(ct).start();
            Read();

        } catch (Exception e) {
            // TODO: handle exception
            ct.stop();
            ExitTerminal();

        }
    }

    public void Read() throws IOException {
        Scanner obj = new Scanner(System.in);
        System.out.println("send to who");
        String tmp = obj.nextLine();
        try {

            while (!tmp.equals("end")) {
                System.out.println("inner");
                String inner = obj.nextLine();
                pack_info pi = new pack_info(this.UID, inner, tmp);
                ou.write(pi.toString().getBytes(StandardCharsets.UTF_8));
                System.out.println("send to who");
                tmp = obj.nextLine();
            }
        } finally {
            obj.close();
        }

    }

    public void Write() throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] bt = new byte[1 << 10];
        int len = 0;
        if ((len = in.read(bt)) != -1)
            bo.write(bt, 0, len);
        String str = bo.toString().trim();
        if (str.matches("\\[\\d{3}\\].*")) {
            System.out.println(str);
        } else {
            pack_info t = new pack_info();
            t.expack(str);
            System.out.printf("%s say %s\n", t.send, t.inner);
        }

    }

    public void ExitTerminal() {
        try {
            in.close();
            ou.close();
            socket.close();
            System.out.println("进程退出");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            System.gc();
        }

    }
};

class CreateThread implements Runnable {
    private Terminal terminal = null;
    private boolean t = true;

    public CreateThread(Terminal terminal) {
        this.terminal = terminal;
    }

    @Override
    public void run() {
        // terminal.Write();

        while (t) {
            try {
                terminal.Write();
            } catch (Exception e) {
                // TODO: handle exception
                t = false;

            }
        }
    }

    public void stop() {
        t = false;
    }
};