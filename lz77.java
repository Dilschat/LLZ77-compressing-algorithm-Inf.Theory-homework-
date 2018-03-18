import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

public class lz77 {
    public lz77(File file) throws IOException {
        this.file = file;
        slidingBuffer = new byte[127];
        readyBuffer = new byte[127];
        Path path = Paths.get(file.getAbsolutePath());
        d = Files.readAllBytes(path);
    }

    public File getFile() {
        return file;
    }

    public File getCompressedFile() {
        return compressedFile;
    }

    void compress() throws IOException {
        data = new LinkedBlockingQueue<Byte>();
        for (int i = 0; i < d.length; i++) {
            data.add(d[i]);
        }
        compressedData = new LinkedList<>();
        BitSet metaInfo = new BitSet();
        int index = 0;
        loadToReadyBuffer(127);
        loadslidingBuffer(0);
        while (!data.isEmpty() || bufferSize > 0) {
            if (indexOf(readyBuffer[0], (byte) 0) == -1) {
                compressedData.add(readyBuffer[0]);
                loadslidingBuffer(1);
                loadToReadyBuffer(1);
                metaInfo.set(index, false);
                index++;
            } else {
                byte lenght = 1;
                byte firstPos = indexOf(readyBuffer[0], (byte) 0);
                byte pos = firstPos;
                byte newPos = indexOf(readyBuffer[1], (byte) (pos + 1));
                if (readyBuffer[0] == 0) {
                    compressedData.add(readyBuffer[0]);
                    metaInfo.set(index, false);
                    loadslidingBuffer(lenght);
                    loadToReadyBuffer(lenght);
                    index++;
                } else {
                    while (newPos == pos + 1) {
                        lenght++;
                        if (lenght < 127 && readyBuffer[lenght] != 0) {
                            pos = newPos;
                            newPos = indexOf(readyBuffer[lenght], (byte) (pos + 1));
                        } else {
                            lenght--;
                            break;
                        }

                    }
                }

                if (lenght == 1) {
                    compressedData.add(readyBuffer[0]);
                    metaInfo.set(index, false);
                    loadslidingBuffer(lenght);
                    loadToReadyBuffer(lenght);
                    index++;
                } else {
                    compressedData.add((byte) (lenght));
                    compressedData.add((byte) (127 - firstPos));

                    metaInfo.set(index, true);
                    loadslidingBuffer(lenght);
                    loadToReadyBuffer(lenght);
                    index++;
                }


            }
        }

        write("com" + file.getName(), compressedData, metaInfo);

    }

    public static void write(String filename, LinkedList<Byte> x, BitSet meta) throws IOException {
        byte[] metaInfo = meta.toByteArray();
        System.out.println("mea");
        System.out.println(meta.length());
        int size = x.size();
        System.out.println(size);
        byte[] num = ByteBuffer.allocate(4).putInt(size).array();
        for (int o = 0; o < num.length; o++) {
            x.addFirst(num[o]);
        }
        for (int o = num.length; o < 4; o++) {
            x.addFirst((byte) 0);
        }
        int mSize = metaInfo.length;
        byte[] compressed = new byte[x.size() + mSize];

        int p = -1;
        for (int i = 0; i < x.size(); i++) {

            compressed[++p] = (byte) x.get(i);

        }
        for (int i = 0; i < metaInfo.length; i++) {

            compressed[++p] = metaInfo[i];

        }
        FileOutputStream fos = new FileOutputStream(new File(filename));
        fos.write(compressed);
        fos.close();
    }

    byte indexOf(byte sh, byte start) {
        byte i;
        for (i = start; i < 127; i++) {
            if (slidingBuffer[i] == sh) {
                return i;
            }
        }
        return -1;
    }

    void loadToReadyBuffer(int num) {
        int i;
        for (i = 0; i < 127 - num; i++) {
            readyBuffer[i] = readyBuffer[num + i];

        }
        bufferSize = bufferSize - num;
        int p;
        for (p = i; p < 127; p++) {
            if (!data.isEmpty()) {
                readyBuffer[p] = data.remove();
                bufferSize = p + 1;
            } else {
                break;
            }

        }

        System.out.println(bufferSize);
    }

    void loadslidingBuffer(int num) throws IOException {
        for (int i = 0; i < 127 - num; i++) {

            slidingBuffer[i] = slidingBuffer[num + i];

        }
        for (int i = slidingBuffer.length - num; i < 127; i++) {
            for (int o = 0; o < num; o++) {
                if (i < 127 && !compressedData.isEmpty()) {
                    slidingBuffer[i] = readyBuffer[o];
                    i++;
                } else {
                    break;
                }
            }

        }

    }

    void uncompress() throws IOException {
        data = new LinkedBlockingQueue<Byte>();

        uncompressedData = new LinkedList<>();
        byte[] num = new byte[4];
        int o = 0;
        for (int i = 3; i > 0; i--) {
            num[i] = d[o];
            o++;
        }
        ByteBuffer a = ByteBuffer.wrap(num);

        int n = a.getInt();
        System.out.println(n);
        int i = 0;
        int imInfo = 0;
        for (int c = 4; c < n + 4; c++) {
            data.add(d[c]);
        }
        byte[] meta = new byte[d.length - n - 4];
        for (int c = n + 4; c < d.length; c++) {
            meta[i] = d[c];
            i++;
        }
        System.out.println(meta.length);
        i = 0;
        BitSet bitSet = BitSet.valueOf(meta);
        while (!data.isEmpty() && i < n) {
            byte b = data.remove();
            if (!bitSet.get(imInfo)) {
                uncompressedData.add(b);
                i++;
                imInfo++;
            } else {
                byte pos = data.remove();
                int size = uncompressedData.size();

                for (int m = size - pos; m < size - pos + b; m++) {
                    uncompressedData.add(uncompressedData.get(m));
                }
                i = i + 2;
                imInfo++;
            }
        }
        FileOutputStream fos = new FileOutputStream(new File("un" + file.getName()));
        byte[] unc = new byte[uncompressedData.size()];
        for (int t = 0; t < uncompressedData.size(); t++) {
            unc[t] = uncompressedData.get(t);
        }
        fos.write(unc);
        fos.close();


    }

    private LinkedList<Byte> uncompressedData;
    private LinkedList<Byte> compressedData;
    LinkedBlockingQueue<Byte> data;
    private byte[] slidingBuffer;
    private byte[] readyBuffer;
    private byte[] d;
    private File file;
    private int bufferSize;
    private File compressedFile;

}