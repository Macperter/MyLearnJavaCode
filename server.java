package tcp_task_new;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.rmi.server.UID;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class server {
    public static void main(String[] args) {
        DataBase db = new DataBase();

        ExecutorService es = Executors.newCachedThreadPool();
        try {
            ServerSocket sv = new ServerSocket(9909);
            while (true) {
                Socket socket = sv.accept();
                String tmp = fun(socket, db);
                if (tmp == null) {
                    socket.getOutputStream().write("[403]用户已在别处登录".getBytes());
                    socket.close();
                } else {
                    socket.getOutputStream().write("[403]登录成功".getBytes());
                    Service ser = new Service(tmp, socket, db);
                    db.writ(tmp, ser);
                    es.execute(ser);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // TODO: handle exception
            es.shutdown();
        }
    }

    public static String fun(Socket tmp, DataBase inf) {
        InputStream in = null;
        try {
            in = tmp.getInputStream();
            ByteArrayOutputStream by = new ByteArrayOutputStream();
            int len = 0;
            byte[] buf = new byte[1 << 10];
            if ((len = in.read(buf)) != -1) {
                by.write(buf, 0, len);
            }
            String str = new String(by.toByteArray());
            str = str.trim();
            if (inf.count(str))
                return null;
            return str;

        } catch (IOException e) {
            // TODO: handle exception
        }
        return null;

    }
}

class pack_info {
    public String send = "";
    public String inner = "";
    public String rev = "";

    public pack_info() {
    }

    public pack_info(String send, String inner, String rev) {
        this.send = send;
        this.inner = inner;
        this.rev = rev;
    }

    public void expack(String str) {
        String tmp = "\\[UID\\]\\s+?\"(\\w+?)\"\\s+?\\[INNER\\]\\s+?\"([\\s\\S]*?)\"\\s+?\\[REV\\]\\s+?\"(\\w+?)\"";
        Pattern pattern = Pattern.compile(tmp);
        Matcher matches = pattern.matcher(str);
        if (matches.find()) {
            send = matches.group(1);
            inner = matches.group(2);
            rev = matches.group(3);
        }

    }

    @Override
    public String toString() {
        return String.format("[UID] \"%s\"\n[INNER] \"%s\"\n[REV] \"%s\"", send, inner, rev);
    }
};

class DataBase {
    private static Lock lock = new ReentrantLock();
    private Map<String, Service> uid = new HashMap<>();

    public Service read(String name) {
        lock.lock();
        try {
            if (uid.containsKey(name)) {
                return uid.get(name);
            }
            return null;
        } finally {
            lock.unlock();
            // TODO: handle exception
        }

    }

    public boolean count(String name) {
        return uid.containsKey(name);
    }

    public boolean writ(String name, Service tmp) {
        lock.lock();
        try {
            if (uid.containsKey(name))
                return false;
            uid.put(name, tmp);
            return true;
        } finally {
            // TODO: handle exception
            lock.unlock();
        }

    }

    public void strongweit(String name, Service tmp) {
        lock.lock();
        try {
            uid.put(name, tmp);
        } finally {
            // TODO: handle exception
            lock.unlock();
        }
    }

    public boolean remove(String name) {
        lock.lock();
        try {
            if (uid.containsKey(name)) {
                uid.remove(name);
                return true;
            }
            return false;
        } finally {
            // TODO: handle exception
            lock.unlock();
        }

    }
};

class Service implements Runnable {
    private String UID = null;
    private Socket socket = null;
    private InputStream in = null;
    private OutputStream ou = null;
    private DataBase db = null;
    private boolean RunSign = true;

    public Service(String UID, Socket socket, DataBase db) {
        this.UID = UID;
        this.socket = socket;
        this.db = db;
        try {             
            ou = socket.getOutputStream();
            in = socket.getInputStream();
        } catch (IOException e) {
            //
            e.printStackTrace();
        }
    }

    public boolean TaskConnect() {
        try {
            socket.sendUrgentData(0x1F);
        } catch (IOException e) {
            // TODO: handle exception
            return false;
        }
        return true;
    }

    @Override
    public void run() {
        while (RunSign) {
            Getinput();
        }
    }

    public void Getinput() {
        ByteArrayOutputStream by = new ByteArrayOutputStream();
        byte[] buf = new byte[1 << 10];
        int len = 0;
        try {
            if ((len = in.read(buf)) != -1)
                by.write(buf, 0, len);
            String str = new String(by.toByteArray(), StandardCharsets.UTF_8); // by.toString();//String str =
                                                                               // by.toString();
            System.out.println(str);
            pack_info pi = new pack_info();
            pi.expack(str);
            if (db.count(pi.rev)) {
                Service tmp = db.read(pi.rev);

                try {
                    tmp.GetOutput(pi.toString());
                } catch (Exception e) {
                    // TODO: handle exception

                    System.out.println(e.getCause());
                    GetOutput("[404]对方网络不佳，或断开" + pi.rev);
                    tmp.ExitService();
                }
            } else {
                GetOutput("[404]该用户未上线");
            }
        } catch (IOException e) {
            // TODO: handle exception
            ExitService();
            return;
        }

    }

    public void Stop() {
        RunSign = false;
    }

    public void GetOutput(String str) throws IOException {
        ou.write(str.getBytes());
    }

    public void ExitService() {
        db.remove(UID);
        System.out.println(UID + "ThreadEnd");
        try {
            socket.shutdownInput();
            socket.shutdownOutput();
            db.remove(UID);
            socket.close();
            RunSign = false;
            System.gc();
        } catch (IOException e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }
};