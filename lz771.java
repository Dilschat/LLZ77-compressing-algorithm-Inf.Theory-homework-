import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;

/**
 * This class provide implementation of lz 77 compressing method
 *
 * @author Dilshat Salikhov
 */
public class lz771 {
    private static final int SLIDING_BUFFER_MAX_SIZE = 32000;
    private static final int READY_BUFFER_MAX_SIZE = 2000;

    public lz771(File file) throws IOException {
        this.file = file;
        Path path = Paths.get(file.getAbsolutePath());
        data = Files.readAllBytes(path);
        slidingBufferIndex = 0;
        slidingBufferLength = 0;
        readyBufferIndex = 0;
        if (READY_BUFFER_MAX_SIZE < data.length) {
            readyBufferLength = READY_BUFFER_MAX_SIZE;
        } else {
            readyBufferLength = data.length;
        }
        compressedData = ByteBuffer.allocate(data.length + 4 + data.length / 8);
    }

    public File getFile() {
        return file;
    }

    public File getCompressedFile() {
        return compressedFile;
    }

    public File getUncompressedFile() {
        return uncompressedFile;
    }

    public void setUncompressedFile(File uncompressedFile) {
        this.uncompressedFile = uncompressedFile;
    }

    /**
     * Compress file
     *
     * @throws IOException
     */
    void compress() throws IOException {
        BitSet metaInfo = new BitSet();
        int index = 0;
        while (readyBufferLength > 0) {
            if (indexOf(data[readyBufferIndex], (short) 0) == -1) {
                compressedData.put(data[readyBufferIndex]);
                loadslidingBuffer(1);
                loadToReadyBuffer(1);
                metaInfo.set(index, false);
                index++;
            } else {
                short length = 1;
                int firstPos = indexOf(data[readyBufferIndex], 0);
                int pos = firstPos;
                int newPos = -1;
                if (readyBufferIndex + length < data.length) {
                    newPos = indexOf(data[readyBufferIndex + length], (pos + 1));
                }

                while (newPos == pos + 1) {
                    length++;
                    if (length < readyBufferLength) {
                        pos = newPos;
                        newPos = indexOf(data[readyBufferIndex + length], (pos + 1));
                        if (newPos != pos + 1) {
                            length--;
                        }
                    } else {
                        break;
                    }

                }


                if (length == 1 || length == 2 || length == 3) {
                    for (int i = 0; i < length; i++) {
                        compressedData.put(data[readyBufferIndex + i]);
                        metaInfo.set(index, false);

                        index++;
                    }
                    loadslidingBuffer(length);
                    loadToReadyBuffer(length);
                } else {
                    compressedData.putShort((short) (length));
                    compressedData.putShort((short) (readyBufferIndex - firstPos));
                    metaInfo.set(index, true);
                    loadslidingBuffer(length);
                    loadToReadyBuffer(length);
                    index++;
                }


            }
        }

        write("com" + file.getName(), compressedData, metaInfo);

    }

    /**
     * Writes compressed data to new file
     *
     * @param filename
     * @param x
     * @param meta
     * @throws IOException
     */
    private void write(String filename, ByteBuffer x, BitSet meta) throws IOException {
        byte[] metaInfo = meta.toByteArray();
        ByteBuffer dataFinal = ByteBuffer.allocate(4 + x.position() + metaInfo.length);
        dataFinal.putInt(x.position());
        dataFinal.put(x.array(), 0, x.position());
        dataFinal.put(metaInfo);
        FileOutputStream fos = new FileOutputStream(new File(filename));
        fos.write(dataFinal.array());
        fos.close();
//        System.out.println("sefsgf");
//        boolean append = false;
//        FileChannel channel = new FileOutputStream(file, append).getChannel();
//        channel.write(dataFinal);
//        channel.close();

        this.compressedFile = new File(filename);

    }

    /**
     * finds index of repeated data in slidingWindow
     *
     * @param sh
     * @param start
     * @return
     */
    private int indexOf(byte sh, int start) {
        int i;
        for (i = slidingBufferIndex + start; i < slidingBufferIndex + slidingBufferLength; i++) {
            if (data[i] == sh) {
                return i;
            }
        }
        return -1;
    }

    /**
     * "Loads" new data innto readyBuffer
     *
     * @param num
     */
    private void loadToReadyBuffer(int num) {
        if (readyBufferIndex + readyBufferLength + num < data.length) {
            readyBufferIndex = readyBufferIndex + num;
        } else {
            readyBufferLength -= num;
            readyBufferIndex += num;
        }

    }

    /**
     * "Loads" new data innto slidingWindow
     *
     * @param num
     */
    private void loadslidingBuffer(int num) throws IOException {
        if (slidingBufferLength < SLIDING_BUFFER_MAX_SIZE) {
            if (slidingBufferIndex + slidingBufferLength + num <= data.length) {
                slidingBufferLength = slidingBufferLength + num;
            } else {
                slidingBufferLength = data.length - slidingBufferIndex;
            }
        } else {
            slidingBufferIndex = slidingBufferIndex + num;
            if (slidingBufferIndex + slidingBufferLength > data.length) {
                slidingBufferLength = data.length - slidingBufferIndex;
            }

        }

    }

    /**
     * uncompreses file
     *
     * @throws IOException
     */
    void uncompress() throws IOException {

        ByteBuffer dataForUncompress = ByteBuffer.allocate(data.length);
        dataForUncompress.put(data);
        dataForUncompress.position(0);
        ArrayList<Byte> uncompressedData = new ArrayList<>();
        int n = dataForUncompress.getInt();
        int i = 0;
        int imInfo = 0;
        byte[] meta = new byte[dataForUncompress.limit() - n - 4];
        for (i = 0; i < meta.length; i++) {
            meta[i] = dataForUncompress.get(4 + n + i);
        }
        i = 0;
        BitSet bitSet = BitSet.valueOf(meta);
        dataForUncompress.position(4);
        while (dataForUncompress.position() < 4 + n) {

            if (!bitSet.get(imInfo)) {
                uncompressedData.add(dataForUncompress.get());
                i++;
                imInfo++;
            } else {
                short b = dataForUncompress.getShort();
                short pos = dataForUncompress.getShort();
                int size = uncompressedData.size();

                for (int m = size - pos; m < size - pos + b; m++) {
                    uncompressedData.add(uncompressedData.get(m));
                }
                i = i + 2;
                imInfo++;
            }
        }
        System.out.println("dfsdf");
        FileOutputStream fos = new FileOutputStream(new File("un" + file.getName()));
        byte[] unc = new byte[uncompressedData.size()];
        for (int t = 0; t < uncompressedData.size(); t++) {
            fos.write(uncompressedData.get(t));
        }

        fos.close();
        this.uncompressedFile = new File("un" + file.getName());
    }


    private ByteBuffer compressedData;
    private int slidingBufferIndex;
    private int slidingBufferLength;
    private int readyBufferIndex;
    private int readyBufferLength;
    private byte[] data;
    private File file;
    private File compressedFile;
    private File uncompressedFile;

}

