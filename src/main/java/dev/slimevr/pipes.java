package dev.slimevr;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.DefaultStyledDocument;

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

    static class TparserThread implements Runnable {
        Thread Tparserthread;
        private int TparserId;
        byte[] file_path_len = new byte[4];

        TparserThread(int id) {
            TparserId = id;
        }

        @Override

        public void run() {
            System.out.println("Thread running ID" + TparserId);

            //AutoDetectParser parser = new AutoDetectParser();

            RandomAccessFile pipe = null;
           // RTFEditorKit rek = new RTFEditorKit();

            try {

                System.out.println("Tparser ID " + TparserId + "started");

                // System.out.println("Tparser ID " + id + ": Initiating..");
                // // Tika tika = new Tika();
                // System.out.println("Tparser ID " + id + ": ready");

                System.out.println("Tparser ID " + TparserId + ": opening pipe..");

                String pipe_path = String.format("\\\\.\\pipe\\tparser_pipe_id_%d-%d", SessionId, TparserId);

                pipe = connect_to_pipe(pipe_path);
                 if (pipe == null)
                    return;
                System.out.println("Tparser ID " + TparserId + "Pipe " + pipe_path + " opened");

                int ret = 0;
                String text;
                boolean from_html;
                while (true) {
                    String input_file = "";
                    ByteBuffer parsed_byte_size;
                    System.out.println("Tparser ID " + TparserId + ": wating for file..");

                    ret = pipe.read(file_path_len, 0, 4);
                    if (ret != 4)
                        System.out.println("Tparser ID " + TparserId + " ret wrong 0");

                    int receive_len = bytesToInt(file_path_len);

                    if (receive_len == Signals.EXIT.getValue() || receive_len == -1)
                        { 
                            System.out.println("Tparser ID " + TparserId + " received end signal or -1");
                            break;
                        }
                    if (receive_len == -10)  // its from html
                       { 
                        ret = pipe.read(file_path_len, 0, 4);
                        receive_len = bytesToInt(file_path_len);
                        from_html = true;
                       }   else from_html = false;
                       
                    byte[] buffer = new byte[receive_len];

                    System.out.println("Tparser ID " + TparserId + " receive_len " + receive_len);

                    ret = pipe.read(buffer, 0, receive_len);
                    if (ret != receive_len)
                        System.out.println("Tparser ID " + TparserId + " ret wrong 1");


                    long now = Instant.now().toEpochMilli();
                    
                    InputStream stream;
                    if (!from_html)
                    {
                        input_file = new String(buffer);
                        System.out.println("Tparser ID " + TparserId + ": parsing file " + input_file);
                        stream = new FileInputStream(input_file);
                    }
                    else
                       {
                        stream = new ByteArrayInputStream(buffer);
                        System.out.println("Tparser ID " + TparserId + ": parsing from html ");
                    }

                    if (true) {
                        BodyContentHandler handler = new BodyContentHandler();
                        Metadata metadata = new Metadata();
                       // parser_list.get(TparserId).parse(stream, handler, metadata);
                        text = handler.toString();
                    }
                    else {
                        DefaultStyledDocument doc = new DefaultStyledDocument();
                     //   rek.read(stream, doc, 0);
                        text = doc.getText(0, doc.getLength());
                    }

                    // String text = tika.parseToString(new File(input_file));

                    // byte[] parsed_byte_size_ = ByteBuffer.allocate(4).putInt(size).array();
                    byte[] bytes = text.getBytes();
                    int size = bytes.length;// - 1;
                    byte[] size_bytes = intToBytes(size);

                    pipe.write(size_bytes, 0, 4);
                    System.out.println("Tparser ID " + TparserId + " size " + size);

                    // if (ret != 4)
                    // System.out.println("Tparser ID " + TparserId + " ret wrong 2");

                    pipe.write(bytes, 0, size);
                    // if (ret != size)
                    // System.out.println("Tparser ID " + TparserId + " ret wrong 3");

                    System.out.println("Tparser ID " + TparserId + "parse took "
                            + Long.toString(Instant.now().toEpochMilli() - now));
                    // System.out.println(text);
                    stream.close();
                    // stream = null;
                    // handler = null;
                    // metadata = null;
                }
                byte[] size_bytes = intToBytes(-1);
                pipe.write(size_bytes);
                pipe.close();

                System.out.println("Tparser ID " + TparserId + " ended");
            } catch (Exception e) {
                System.out.println("Sparser ID " + TparserId + " something went wrong. \n" + e);
                try {
                    pipe.close();
                } catch (Exception e2) {
                    System.out.println("Sparser ID " + TparserId + " Something went wrong. \n" + e2);
                }
            }
            System.out.println("Tparser ID " + TparserId + " reached end of function");

        }

        public void start() {
            // System.out.println("Thread started");
            if (Tparserthread == null) {
                Tparserthread = new Thread(this);
                Tparserthread.start();
            }

        }

    }
}