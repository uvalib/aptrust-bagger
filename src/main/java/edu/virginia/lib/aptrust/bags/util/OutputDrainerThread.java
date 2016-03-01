package edu.virginia.lib.aptrust.bags.util;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class OutputDrainerThread implements Runnable {

    private InputStream is;
    
    private OutputStream os;
    
    public OutputDrainerThread(InputStream stream) {
        this(stream, null);
    }
    
    public OutputDrainerThread(InputStream input, OutputStream output) {
        is = input;
        os = output;
    }
    
    public void run() {
        try {
            byte[] buffer = new byte[1024];
            while (true) {
                try {
                    int read = is.read(buffer);
                    if (read == -1) {
                        return;
                    } else if (read == 0) {
                        Thread.sleep(300);
                    } else {
                        if (os != null) {
                            os.write(buffer, 0, read);
                        }
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } catch (InterruptedException ex) {
                    // out sleep was interrupted, no big deal
                }
            }
        } finally {
            if (os != null) {
                try {
                    os.flush();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        
    }
    
    
}
