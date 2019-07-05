package com.example.demo;

import net.coobird.thumbnailator.Thumbnails;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "img")
public class Pic {

    private static String defaultImgSavePath = "upload/images"; //默认的图片存储路径
    private static String defaultThumbnailSavePath = "upload/thumbnail"; //默认的缩略图存储路径
    // 缩略图大小
    private static int width = 200;
    private static int height = 200;
    private static float waterMarkFontSize = 0.05f;//水印比例

    /**
     * 页面上传文件
     *
     * @param multiFile
     * @param request
     * @return
     */
    public static String MultipartFileToFile(MultipartFile multiFile, HttpServletRequest request) {
        // 获取文件名
        String fileName = multiFile.getOriginalFilename();
        if (fileName == null || fileName.length() == 0) {
            return "file is empty";
        }
        // 获取文件后缀
        String prefix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        if (prefix.equalsIgnoreCase("jpg")
                || prefix.equalsIgnoreCase("jpeg")
                || prefix.equalsIgnoreCase("png")
                || prefix.equalsIgnoreCase("gif")) {
            try {
                String saveFileName = getDefaultPath(request, defaultImgSavePath, prefix);
                int index = saveFileName.lastIndexOf("/");
                String newFileName = saveFileName.substring(index + 1);
//                File file = File.createTempFile(newFileName, "", new File(saveFileName.substring(0, index)));//mgj 莫名其妙的问题
                File file = new File(saveFileName);
                multiFile.transferTo(file);
                String path = ClassUtils.getDefaultClassLoader().getResource("").getPath();
                return getDefaultWebUrl(request, saveFileName.substring(path.length()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return "prefix is false";
        }
        return null;
    }

    /**
     * 页面上传文件,并保存缩略图
     *
     * @param multiFile
     * @param request
     * @param thumbnail
     * @return
     */
    public static Map<String, String> MultipartFileToFile(MultipartFile multiFile, HttpServletRequest request
            , boolean thumbnail) {
        // 获取文件名
        String fileName = multiFile.getOriginalFilename();
        if (fileName == null || fileName.length() == 0) {
            return null;
        }
        Map<String, String> map = new HashMap<>();
        // 获取文件后缀
        String prefix = fileName.substring(fileName.lastIndexOf(".") + 1);
        if (prefix.equalsIgnoreCase("jpg")
                || prefix.equalsIgnoreCase("jpeg")
                || prefix.equalsIgnoreCase("png")
                || prefix.equalsIgnoreCase("gif")) {
            if (thumbnail) {
                try {
                    MultipartFile mul = new MockMultipartFile(fileName, fileName, ""
                            , multiFile.getInputStream());
                    String saveFilePath = getDefaultPath(request, defaultImgSavePath, "");
                    int index = saveFilePath.lastIndexOf("/");
//                    File file = File.createTempFile(saveFilePath.substring(index+1), prefix, new File(saveFilePath.substring(0, index)));
                    File file = new File(saveFilePath + "." + prefix);
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    multiFile.transferTo(file);
                    String webUrl = getDefaultWebUrl(request, saveFilePath + "." + prefix);//获取图片的访问路径
                    map.put("img", webUrl);
                    String thumbnailPath = saveFilePath.replace(defaultImgSavePath, defaultThumbnailSavePath) +
                            "." + prefix; //获取缩略图存储路径

                    thumbnail(mul, thumbnailPath);//存储缩略图
                    String thumbnailWebUrl = getDefaultWebUrl(request, thumbnailPath); //获取缩略图的访问路径
                    map.put("thumbnail", thumbnailWebUrl);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                map.put("img", MultipartFileToFile(multiFile, request));
            }
        } else {
            return null;
        }
        return map;
    }

    /**
     * 页面上传文件,添加水印，并保存缩略图
     *
     * @param multiFile
     * @param request
     * @param thumbnail
     * @param text
     * @param x
     * @param y
     * @return
     */
    public static Map<String, String> MultipartFileToFile(MultipartFile multiFile, HttpServletRequest request
            , boolean thumbnail, String text, int x, int y) {
        // 获取文件名
        String fileName = multiFile.getOriginalFilename();
        if (fileName == null || fileName.length() == 0) {
            return null;
        }
        Map<String, String> map = new HashMap<>();
        // 获取文件后缀
        String prefix = fileName.substring(fileName.lastIndexOf(".") + 1);
        if (prefix.equalsIgnoreCase("jpg")
                || prefix.equalsIgnoreCase("jpeg")
                || prefix.equalsIgnoreCase("png")
                || prefix.equalsIgnoreCase("gif")) {
            MultipartFile waterMark = null;
            if (thumbnail) {
                try {
                    MultipartFile mul = new MockMultipartFile(fileName, fileName, ""
                            , multiFile.getInputStream());//复制一份图片
                    waterMark = setWaterMark(multiFile, text); //替换成已经有水印的图片
                    String saveFilePath = getDefaultPath(request, defaultImgSavePath, "");
                    int index = saveFilePath.lastIndexOf("/");
                    File file = new File(saveFilePath + "." + prefix);
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    multiFile.transferTo(file);
                    String webUrl = getDefaultWebUrl(request, saveFilePath + "." + prefix);//获取图片的访问路径
                    map.put("img", webUrl);

                    String thumbnailPath = saveFilePath.replace(defaultImgSavePath, defaultThumbnailSavePath) +
                            "." + prefix; //获取缩略图存储路径
                    thumbnail(mul, thumbnailPath);//存储缩略图
                    String thumbnailWebUrl = getDefaultWebUrl(request, thumbnailPath); //获取缩略图的访问路径
                    map.put("thumbnail", thumbnailWebUrl);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                map.put("img", MultipartFileToFile(multiFile, request));
            }
        } else {
            return null;
        }
        return map;
    }

    /**
     * 通过BASE64Decoder解码，并生成图片
     *
     * @param encodedImageStr
     * @return url地址
     */
    public static String base64ToImage(String encodedImageStr, HttpServletRequest request) {
        if (encodedImageStr == null)
            return "图片不存在";
        try {
            // Base64解码图片
            String data = encodedImageStr.substring(encodedImageStr.indexOf(","), encodedImageStr.length() - 1);
            byte[] imageByteArray = Base64.decodeBase64(data);

            String saveFilePath = getDefaultPath(request, defaultImgSavePath, ""); // 图片的新名字
            System.out.println("绝对地址" + saveFilePath);
            save2Disk(imageByteArray, saveFilePath);// 存储图片

            String webUrl = getDefaultWebUrl(request, saveFilePath); // 图片网络地址
            System.out.println("Image Successfully Stored");
            return webUrl;
        } catch (FileNotFoundException fnfe) {
            System.out.println("Image Path not found" + fnfe);
        } catch (IOException ioe) {
            System.out.println("Exception while converting the Image " + ioe);
        }
        return "fail";
    }

    /**
     * 通过BASE64Decoder解码，并生成图片和缩略图
     *
     * @param encodedImageStr
     * @param request
     * @param thumbnail
     * @return
     */
    public static Map<String, String> base64ToImage(String encodedImageStr, HttpServletRequest request
            , boolean thumbnail) {
        Map<String, String> map = new HashMap<>();
        if (encodedImageStr == null)
            return null;
        if (thumbnail) {
            try {
                // Base64解码图片
                String data = encodedImageStr.substring(encodedImageStr.indexOf(","), encodedImageStr.length() - 1);
                byte[] imageByteArray = Base64.decodeBase64(data);

                String saveFilePath = getDefaultPath(request, defaultImgSavePath, ""); // 图片的新名字
                System.out.println("绝对地址" + saveFilePath);
                save2Disk(imageByteArray, saveFilePath);// 存储图片
                String webUrl = getDefaultWebUrl(request, saveFilePath); // 图片网络地址
                map.put("img", webUrl);
                String thumbnailPath = defaultThumbnailSavePath + saveFilePath
                        .substring(defaultImgSavePath.length(), saveFilePath.length()); //获取缩略图存储路径
                thumbnail(saveFilePath, thumbnailPath);
                String thumbnailWebUrl = getDefaultWebUrl(request, thumbnailPath); //获取缩略图的访问路径
                map.put("thumbnail", thumbnailWebUrl);
                System.out.println("Image Successfully Stored");
            } catch (FileNotFoundException fnfe) {
                System.out.println("Image Path not found" + fnfe);
            } catch (IOException ioe) {
                System.out.println("Exception while converting the Image " + ioe);
            }
        } else {
            String webUrl = base64ToImage(encodedImageStr, request);
            map.put("img", webUrl);
        }
        return map;
    }

    /**
     * 返回设置好水印的图片
     *
     * @param multiFile
     * @param text
     * @return
     * @throws IOException
     */
    public static MultipartFile setWaterMark(MultipartFile multiFile, String text) {
        // 获取文件后缀
        String fileName = multiFile.getOriginalFilename();
        String prefix = fileName.substring(fileName.lastIndexOf("."));
        if ("gif".equalsIgnoreCase(prefix)) {
            return null;
        }
        InputStream is = null;
        Image img = null;
        try {
            is = multiFile.getInputStream();
            img = ImageIO.read(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int width = img.getWidth(null);
        int height = img.getHeight(null);
        int fontSize = Math.round(width * waterMarkFontSize); //获取固定比例的字体大小
        BufferedImage bfimg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Font font = new Font("黑体", Font.PLAIN, fontSize);
        markWord(bfimg, img, text, font, Color.gray, 10, 10);//设置水印
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        ImageOutputStream imgos = null;
        try {
            imgos = ImageIO.createImageOutputStream(bs);
            ImageIO.write(bfimg, prefix, imgos);
            MultipartFile multipartFile = new MockMultipartFile(fileName, fileName, ""
                    , new ByteArrayInputStream(bs.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                imgos.close();
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return multiFile;
    }

    /**
     * 添加文字水印
     *
     * @param bfimg
     * @param img
     * @param text
     * @param font
     * @param color
     * @param x
     * @param y
     */
    private static void markWord(BufferedImage bfimg, Image img, String text, Font font, Color color, int x, int y) {
        Graphics2D g = bfimg.createGraphics();
        g.drawImage(img, 0, 0, width, height, null);
        g.setColor(color);
        g.setFont(font);
        g.drawString(text, x, y);
        g.dispose();
    }

    /**
     * 生成并获取图片的相对存储路径
     *
     * @param request
     * @param prefix  如果 prefix为空则返回不带文件后缀的路径
     * @return
     */
    private static String getDefaultPath(HttpServletRequest request, String path, String prefix) {
        String saveFileName = getFileName(request, path);
        if (null == prefix || "".equals(prefix)) {
            return saveFileName;
        } else {
            saveFileName += "." + prefix;
        }
        return saveFileName;
    }

    /**
     * 生成以时间戳来命名的文件名
     *
     * @param request
     * @param file
     * @return
     */
    private static String getFileName(HttpServletRequest request, String file) {
        //项目路径
//        String saveDirectory = request.getSession().getServletContext().getRealPath("");
        //静态文件路径
        String path = ClassUtils.getDefaultClassLoader().getResource("").getPath();
        // 建年的文件夹
        path += defaultImgSavePath;
        Calendar date = Calendar.getInstance();
        int iYear = date.get(Calendar.YEAR);
        path += "/" + iYear;
        int iMonth = date.get(Calendar.MONTH) + 1;
        path += "/" + iMonth;
        int iDay = date.get(Calendar.DATE);
        path += "/" + iDay;

        File f = new File(path);
        if (!f.isDirectory()) {
            f.mkdirs();
        }
        String saveFileName = iYear + "" + iMonth + "" + iDay + "" + date.get(Calendar.HOUR) + date.get(Calendar.MINUTE) + "" + date.get(Calendar.SECOND);
        return path + "/" + saveFileName;
    }

    /**
     * 获取默认的图片访问地址
     *
     * @param request
     * @param saveFilePath
     * @return
     */
    private static String getDefaultWebUrl(HttpServletRequest request, String saveFilePath) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/" + request.getContextPath() + saveFilePath;
    }

    /**
     * 存储图片
     *
     * @param data
     * @param path
     * @throws IOException
     */
    private static void save2Disk(byte[] data, String path) throws IOException {
        FileOutputStream imageOutFile = new FileOutputStream(path);
        imageOutFile.write(data);
        imageOutFile.close();
    }

    /**
     * 生成并存储图片缩略图
     *
     * @param file
     * @param savePath
     * @throws IOException
     */
    private static void thumbnail(MultipartFile file, String savePath) throws IOException {
        int index = savePath.lastIndexOf("/");
        File f = new File(savePath.substring(0, index));
        if (!f.exists()) {
            f.mkdirs();
        }
//        f = new File(savePath);
//        if (!f.exists()) {
//            f.createNewFile();
//        }
        System.out.println("savePath\t" + savePath);
        Thumbnails.of(file.getInputStream())
                .size(width, height)
                .toFile(savePath);
    }

    /**
     * 生成并存储图片缩略图 以文件路径来获取
     *
     * @param file
     * @param savePath
     * @throws IOException
     */
    private static void thumbnail(String file, String savePath) throws IOException {
        Thumbnails.of(file)
                .size(width, height)
                .toFile(savePath);
    }
}
