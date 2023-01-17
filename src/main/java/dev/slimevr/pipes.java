package dev.slimevr;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import dev.slimevr.vr.processor.skeleton.HumanSkeleton;
import javax.swing.text.DefaultStyledDocument;

import com.jme3.math.Vector3f;

import dev.slimevr.vr.trackers.Tracker;

public class pipes {
    
   // static List<AutoDetectParser> parser_list = new ArrayList<AutoDetectParser>();
    static int SessionId;
    static boolean stop = false;
    public enum Signals {
        EXIT(-3), CHANGE_N_THREADS(-2), DISCONNECT_PIPE(-1);

        private final int id;

        Signals(int id) {
            this.id = id;
        }

        public int getValue() {
            return id;
        }
    }

    private static byte[] intToBytes(final int i) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(i);
        return bb.array();
    }

    private static int bytesToInt(final byte[] buffer) {
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        int signal_received = bb.getInt();
        return signal_received;
    }

  // public static String getExt(String filename) {
  //      return FilenameUtils.getExtension(filename);
   // }
    public static Long milis(){
        return Instant.now().toEpochMilli();
    }

    static RandomAccessFile connect_to_pipe(String pipe_path) throws InterruptedException {
        boolean failed = true, failed2 = true;
        RandomAccessFile pipe = null;
        stop = false;
        // while (failed) {
        // try {
        // FileInputStream f = new FileInputStream(pipe_path);
        // failed = false;
        // } catch (IOException e) {
        // Thread.sleep(20);
        // }
        // }
        while (failed2) {
            try {
                pipe = new RandomAccessFile(pipe_path, "rw");
                failed = false;
                if (stop == true){
                    System.out.println("Opening pipe interrupted");
                    return null;
                }
                break; // return pipe;
            } catch (Exception e) {
                failed2 = true;
                try {
                    Thread.sleep(500);
                } catch (Exception e2) {
                    System.out.println(e2);
                }
            }
        }
        return pipe;
    }

    static class PipeThread implements Runnable {
        Thread PipeThread;
        private int TparserId;
        byte[] buf = new byte[4];
        String PipePath;
        PipeThread(String pipePath) {
            PipePath = pipePath;
        }
        private byte[] aprilRecvBuff = new byte[57];
        private byte[] aprilVecBuff = new byte[8];
       // public byte aprilDataAvailable = 0;
        private final Vector3f aprilVec = new Vector3f();
        
        RandomAccessFile pipe = null;
        VRServer vrServerRef = null;
       List<Tracker> trackers;
      public Tracker rightUpperLegTracker;

        public boolean init(VRServer vrServer) throws InterruptedException, IOException
        {
            vrServerRef = vrServer;
            trackers = vrServerRef.getAllTrackers();
            for (Tracker tracker : trackers){
                String s = tracker.getName();
				if (s.equals("human://RIGHT_UPPER_LEG"))
				{ 
                    rightUpperLegTracker = tracker;
				}
            }
            boolean connectionSuccess = false;    
            System.out.println("Opening pipe " + PipePath);

            pipe = connect_to_pipe(PipePath);
             if (pipe == null)
                return false;
            System.out.println("Pipe " + PipePath + " opened");

            int ret = pipe.read(buf, 0, 1);
            pipe.write(buf, 0, 1);
            if (buf[0] == 99){
                connectionSuccess = true;
                System.out.println("Connection to Apriltag succesful");
            }
            else{
                connectionSuccess = false;
                System.out.println("Connection to Apriltag NOT succesful");
            }
            return connectionSuccess;
        }

        @Override

        public void run() {
            System.out.println("Pipe Thread running " + PipePath);

                try 
                {
                    pipe.write(1);
                    int ret = pipe.read(aprilRecvBuff, 0, 57);
                    if (ret == -1)
                    {
                        vrServerRef.connectToApril = false;
                        pipe.close();
                        pipe = null;
                    }
                    rightUpperLegTracker.SetAprilDataAvailable(aprilRecvBuff[0]);
                     //=  aprilRecvBuff[0:10];
                    System.arraycopy(aprilRecvBuff, 1, aprilVecBuff, 0, 8);
        
                    aprilVec.x = (float)vrServerRef.toDouble(aprilVecBuff);
                    System.arraycopy(aprilRecvBuff, 9, aprilVecBuff, 0, 8);
        
                    aprilVec.y = (float)vrServerRef.toDouble(aprilVecBuff);
                    System.arraycopy(aprilRecvBuff, 17, aprilVecBuff, 0, 8);
        
                    aprilVec.z = (float)vrServerRef.toDouble(aprilVecBuff);
        
                    float w, x, y, z;
        
                    System.arraycopy(aprilRecvBuff, 25, aprilVecBuff, 0, 8);
                    w = (float)vrServerRef.toDouble(aprilVecBuff);
        
                    System.arraycopy(aprilRecvBuff, 33, aprilVecBuff, 0, 8);
                    x = (float)vrServerRef.toDouble(aprilVecBuff);
        
                    System.arraycopy(aprilRecvBuff, 41, aprilVecBuff, 0, 8);
                    y = (float)vrServerRef.toDouble(aprilVecBuff);
        
                    System.arraycopy(aprilRecvBuff, 49, aprilVecBuff, 0, 8);
                    z = (float)vrServerRef.toDouble(aprilVecBuff);
        
                    rightUpperLegTracker.PrevAprilQuat.set(rightUpperLegTracker.aprilQuat);
                    rightUpperLegTracker.aprilQuat.set(x, y, z, w);
        
        
        
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                    vrServerRef.connectToApril = false;
                    pipe.close();
                    pipe = null;
                    }
                    catch (IOException e2) {
                        e2.printStackTrace();
                    }
                }
                
                byte[] size_bytes = intToBytes(-1);
                try {
                    pipe.write(size_bytes);
                    pipe.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("Tparser ID " + TparserId + " ended");
           

        }
    

        public void start() {
            // System.out.println("Thread started");
            if (PipeThread == null) {
                PipeThread = new Thread(this);
                PipeThread.start();
            }

        }

    }
}