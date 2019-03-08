import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author xuqiang
 * @date 2019/3/7
 */
public class QRCodeUtil {

    //设置生个图片格式
    private static final String FORMAT = "png";
    private static final String CHASET = "utf-8";
    private static final int MARGIN = 2;
    private static final int QRCODE_SIZE = 300;
    private static final int LOGO_SIZE = 60;
    public static final int BLACK = 0xFF000000;
    public static final int WHITE = 0xFFFFFFFF;

    /**
     * 获取二维码
     *
     * @param context 设置二维码内容
     * @param width
     * @param height
     * @param logo    是否需要添加logo
     * @return
     */
    public static BufferedImage encode(String context, int width, int height, boolean logo) {
        //设置额外参数
        Map<EncodeHintType, Object> map = new HashMap<>();
        //设置编码集
        map.put(EncodeHintType.CHARACTER_SET, CHASET);
        //容错率，指定容错等级，例如二维码中使用的ErrorCorrectionLevel
        map.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        //生成条码的时候使用，指定边距，单位像素，受格式的影响。类型Integer, 或String代表的数字类型
        map.put(EncodeHintType.MARGIN, MARGIN);
        //生成二维码，（参数为：编码的内容、编码的方式（二维码、条形码...）、首选的宽度、首选的高度、编码时的额外参数）
        BitMatrix encode = null;
        try {
            encode = new MultiFormatWriter().encode(context, BarcodeFormat.QR_CODE, width, height, map);
            return toBufferedImage(encode, logo);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * MatrixToImageWriter中有此方法，但生成的是二进制图片无法插入彩色图标，
     * imageType：
     * TYPE_BYTE_BINARY 二进制
     * TYPE_INT_RGB 支持 8位RGB颜色
     * TYPE_INT_RGB 支持 8位RGBA颜色
     *
     * @param matrix
     * @return
     */
    public static BufferedImage toBufferedImage(BitMatrix matrix, boolean logo) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        int imageType = logo ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_BYTE_BINARY;
        BufferedImage image = new BufferedImage(width, height, imageType);
        int onColor = BLACK;
        int offColor = WHITE;
        int[] rowPixels = new int[width];
        BitArray row = new BitArray(width);
        for (int y = 0; y < height; y++) {
            row = matrix.getRow(y, row);
            for (int x = 0; x < width; x++) {
                rowPixels[x] = row.get(x) ? onColor : offColor;
            }
            image.setRGB(0, y, width, 1, rowPixels, 0, width);
        }
        return image;
    }

    /**
     * 返回logoImage, 大于60像素会返回缩略图
     *
     * @param logoPath logo地址
     * @return
     */
    public static Image getLogo(String logoPath) {
        try {
            Image logo = ImageIO.read(new File(logoPath));
            int width = logo.getWidth(null), height = logo.getHeight(null);
            boolean widthGt, heightGt = false;
            if ((widthGt = (width > LOGO_SIZE)) || (heightGt = (height > LOGO_SIZE))) {
                // 如何logo宽度大于 LOGO_SIZE 则获取缩略图
                width = widthGt ? LOGO_SIZE : width;
                height = heightGt || height > LOGO_SIZE ? LOGO_SIZE : height;
                return logo.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            }
            return logo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 插入logo
     *
     * @param img
     * @param logo
     * @return
     * @throws IOException
     */
    public static BufferedImage insertLogo(BufferedImage img, Image logo) {
        int width = logo.getWidth(null), height = logo.getHeight(null);
        Graphics2D graphics = img.createGraphics();
        int x = (img.getWidth() - width) / 2;
        int y = (img.getHeight() - height) / 2;
        // 插入logo
        graphics.drawImage(logo, x, y, width, height, null);
        // 插入圆角边框
        Shape shape = new RoundRectangle2D.Float(x, y, width, height, 15, 15);
        graphics.setStroke(new BasicStroke(3f));
        graphics.draw(shape);
        graphics.dispose();
        return img;
    }

    /**
     * 获取二维码
     *
     * @param context  设置二维码内容
     * @param width
     * @param height
     * @param logoPath
     * @return
     */
    public static BufferedImage encode(String context, int width, int height, String logoPath) {
        Image logo = getLogo(logoPath);
        BufferedImage image = encode(context, width, height, logoPath != null);
        return insertLogo(image, logo);
    }

    /**
     * 获取二维码
     *
     * @param context 设置二维码内容
     * @return
     */
    public static BufferedImage encode(String context, String logoPath) {
        return encode(context, QRCODE_SIZE, QRCODE_SIZE, logoPath);
    }

    /**
     * 批量生成二维码，生成zip压缩包
     *
     * @param outputStream
     * @param logoPath
     * @param context
     */
    public static void encodeBatch(OutputStream outputStream, String logoPath, List<String> context) {
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, Charset.forName(CHASET));
        try {
            for (String s : context) {
                BufferedImage image = encode(s, logoPath);
                zipOutputStream.putNextEntry(new ZipEntry(s + "." + FORMAT));
                ImageIO.write(image, FORMAT, zipOutputStream);
            }
            zipOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws IOException, WriterException {
        String logoPath = "C:\\Users\\15968\\Documents\\IdeaProjects\\work\\zxing-demo\\src\\main\\resources\\logo.png";
//        String logoPath = "C:\\Users\\15968\\Documents\\IdeaProjects\\work\\zxing-demo\\src\\main\\resources\\logo-big.png";
        ImageIO.write(encode("我是一个苦逼的程序猿", logoPath), FORMAT, new File("D:\\test1.png"));

//        String[] context = new String[]{"我是一个苦逼的程序员", "苦逼就算了还丑", "丑就算了还穷"};
//        encodeBatch(new FileOutputStream("D:\\test.zip"), logoPath, Arrays.asList(context));

    }


}
