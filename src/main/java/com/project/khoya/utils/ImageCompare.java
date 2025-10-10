//package com.project.khoya.utils;
//
//import org.opencv.core.*;
//import org.opencv.imgcodecs.Imgcodecs;
//import org.opencv.imgproc.Imgproc;
//import java.io.*;
//import java.net.URL;
//
//public class ImageCompare {
//    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }
//
//    public static void main(String[] args) throws Exception {
//        String url1 = "https://example.com/image1.jpg";
//        String url2 = "https://example.com/image2.jpg";
//
//        // Download both images
//        Mat img1 = urlToMat(url1);
//        Mat img2 = urlToMat(url2);
//
//        if (img1.empty() || img2.empty()) {
//            System.out.println("Could not load one of the images!");
//            return;
//        }
//
//        // Convert to grayscale
//        Imgproc.cvtColor(img1, img1, Imgproc.COLOR_BGR2GRAY);
//        Imgproc.cvtColor(img2, img2, Imgproc.COLOR_BGR2GRAY);
//
//        // Resize if different sizes
//        if (img1.size().width != img2.size().width || img1.size().height != img2.size().height) {
//            Imgproc.resize(img2, img2, img1.size());
//        }
//
//        // Compute visual similarity (MSE + SSIM style)
//        Mat diff = new Mat();
//        Core.absdiff(img1, img2, diff);
//        diff.convertTo(diff, CvType.CV_32F);
//        diff = diff.mul(diff);
//
//        Scalar s = Core.sumElems(diff);
//        double mse = s.val[0] / (double)(img1.channels() * img1.total());
//        double ssim = 1.0 / (1.0 + mse);
//
//        System.out.println("MSE  : " + mse);
//        System.out.println("SSIM : " + ssim);
//        System.out.println("Similarity: " + (ssim * 100) + "%");
//    }
//
//    private static Mat urlToMat(String imageUrl) throws IOException {
//        // Download image bytes
//        URL url = new URL(imageUrl);
//        InputStream in = url.openStream();
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        byte[] buffer = new byte[1024];
//        int n;
//        while ((n = in.read(buffer)) != -1) {
//            out.write(buffer, 0, n);
//        }
//        in.close();
//        byte[] imageBytes = out.toByteArray();
//
//        // Decode into Mat
//        Mat mat = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);
//        return mat;
//    }
//}
//
