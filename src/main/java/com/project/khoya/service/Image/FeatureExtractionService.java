package com.project.khoya.service.Image;//package com.project.khoya.service.Image;
//
//import ai.djl.ModelException;
//import ai.djl.inference.Predictor;
//import ai.djl.modality.cv.Image;
//import ai.djl.modality.cv.transform.CenterCrop;
//import ai.djl.modality.cv.transform.Normalize;
//import ai.djl.modality.cv.transform.Resize;
//import ai.djl.modality.cv.transform.ToTensor;
//import ai.djl.ndarray.NDArray;
//import ai.djl.ndarray.NDList;
//import ai.djl.ndarray.NDManager;
//import ai.djl.repository.zoo.Criteria;
//import ai.djl.repository.zoo.ModelNotFoundException;
//import ai.djl.repository.zoo.ZooModel;
//import ai.djl.training.util.ProgressBar;
//import ai.djl.translate.Pipeline;
//import ai.djl.translate.TranslateException;
//import ai.djl.translate.Translator;
//import ai.djl.translate.TranslatorContext;
//import jakarta.annotation.PostConstruct;
//import jakarta.annotation.PreDestroy;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//import java.nio.ByteBuffer;
//
//@Service
//@Slf4j
//public class FeatureExtractionService {
//
//    @Value("${model.resnet50.path:src/main/resources/models/resnet50_features.pt}")
//    private String modelPath;
//
//    @Value("${model.resnet50.engine:PyTorch}")
//    private String modelEngine;
//
//    private ZooModel<Image, float[]> model;
//    private static final int FEATURE_DIMENSION = 2048; // ResNet50 output dimension
//
//    @PostConstruct
//    public void init() {
//        log.info("Initializing ResNet50 feature extractor from: {}", modelPath);
//        try {
//            Criteria<Image, float[]> criteria = Criteria.builder()
//                    .setTypes(Image.class, float[].class)
//                    .optModelUrls("file:" + modelPath)
//                    .optTranslator(new FeatureTranslator())
//                    .optEngine(modelEngine)
//                    .optProgress(new ProgressBar())
//                    .build();
//
//            model = criteria.loadModel();
//            log.info("✓ ResNet50 feature extractor loaded successfully (dimension: {})", FEATURE_DIMENSION);
//
//            // Warm up the model with a dummy prediction
//            warmUpModel();
//
//        } catch (ModelNotFoundException e) {
//            log.error("❌ Model file not found at: {}", modelPath, e);
//            throw new IllegalStateException("ResNet50 model not found. Please ensure the model file exists at: " + modelPath, e);
//        } catch (ModelException | IOException e) {
//            log.error("❌ Error loading ResNet50 model", e);
//            throw new IllegalStateException("Failed to initialize ResNet50 model", e);
//        }
//    }
//
//    /**
//     * Warm up the model with a dummy prediction to avoid cold start delays.
//     */
//    private void warmUpModel() {
//        try (NDManager manager = NDManager.newBaseManager()) {
//            // Create a dummy 224x224x3 image
//            NDArray dummyImage = manager.zeros(new ai.djl.ndarray.types.Shape(224, 224, 3));
//            log.info("Model warm-up completed");
//        } catch (Exception e) {
//            log.warn("Model warm-up failed, but this is not critical: {}", e.getMessage());
//        }
//    }
//
//    public float[] extractFeatures(Image image) throws TranslateException {
//        if (model == null) {
//            throw new IllegalStateException("Model not initialized. Call init() first.");
//        }
//
//        try (Predictor<Image, float[]> predictor = model.newPredictor()) {
//            float[] features = predictor.predict(image);
//
//            // L2 normalize the features for better cosine similarity
//            return l2Normalize(features);
//
//        } catch (Exception e) {
//            log.error("Failed to extract features from image", e);
//            throw new TranslateException("Feature extraction failed: " + e.getMessage(), e);
//        }
//    }
//
//
//    public byte[] extractFeaturesAsBytes(Image image) throws TranslateException {
//        float[] features = extractFeatures(image);
//        return floatArrayToBytes(features);
//    }
//
//
//    private float[] l2Normalize(float[] features) {
//        double sumSquares = 0.0;
//        for (float f : features) {
//            sumSquares += f * f;
//        }
//
//        double norm = Math.sqrt(sumSquares);
//        if (norm < 1e-12) {
//            log.warn("Feature vector has near-zero norm, returning as-is");
//            return features;
//        }
//
//        float[] normalized = new float[features.length];
//        for (int i = 0; i < features.length; i++) {
//            normalized[i] = (float) (features[i] / norm);
//        }
//
//        return normalized;
//    }
//
//
//    public double calculateCosineSimilarity(float[] vec1, float[] vec2) {
//        if (vec1 == null || vec2 == null) {
//            throw new IllegalArgumentException("Feature vectors cannot be null");
//        }
//
//        if (vec1.length != vec2.length) {
//            throw new IllegalArgumentException(
//                    String.format("Feature vectors must have same length (got %d and %d)",
//                            vec1.length, vec2.length)
//            );
//        }
//
//        double dotProduct = 0.0;
//        double norm1 = 0.0;
//        double norm2 = 0.0;
//
//        for (int i = 0; i < vec1.length; i++) {
//            dotProduct += vec1[i] * vec2[i];
//            norm1 += vec1[i] * vec1[i];
//            norm2 += vec2[i] * vec2[i];
//        }
//
//        if (norm1 < 1e-12 || norm2 < 1e-12) {
//            log.warn("One or both vectors have near-zero norm");
//            return 0.0;
//        }
//
//        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
//    }
//
//    /**
//     * Convert float array to byte array for database storage.
//     */
//    public byte[] floatArrayToBytes(float[] array) {
//        ByteBuffer buffer = ByteBuffer.allocate(array.length * Float.BYTES);
//        for (float value : array) {
//            buffer.putFloat(value);
//        }
//        return buffer.array();
//    }
//
//    /**
//     * Convert byte array back to float array from database.
//     */
//    public float[] bytesToFloatArray(byte[] bytes) {
//        if (bytes == null || bytes.length == 0) {
//            throw new IllegalArgumentException("Byte array cannot be null or empty");
//        }
//
//        if (bytes.length % Float.BYTES != 0) {
//            throw new IllegalArgumentException("Invalid byte array length for float conversion");
//        }
//
//        ByteBuffer buffer = ByteBuffer.wrap(bytes);
//        float[] array = new float[bytes.length / Float.BYTES];
//        for (int i = 0; i < array.length; i++) {
//            array[i] = buffer.getFloat();
//        }
//        return array;
//    }
//
//    /**
//     * Get the expected feature dimension size.
//     */
//    public int getFeatureDimension() {
//        return FEATURE_DIMENSION;
//    }
//
//    /**
//     * Clean up model resources on application shutdown.
//     */
//    @PreDestroy
//    public void cleanup() {
//        if (model != null) {
//            model.close();
//            log.info("ResNet50 model resources cleaned up");
//        }
//    }
//
//    /**
//     * Translator for ResNet50 preprocessing and feature extraction.
//     * Applies standard ImageNet preprocessing pipeline.
//     */
//    private static class FeatureTranslator implements Translator<Image, float[]> {
//        private final Pipeline pipeline;
//
//        public FeatureTranslator() {
//            pipeline = new Pipeline();
//
//            // Standard ImageNet preprocessing
//            pipeline.add(new Resize(256));                    // Resize shorter edge to 256
//            pipeline.add(new CenterCrop(224, 224));           // Center crop to 224x224
//            pipeline.add(new ToTensor());                      // Convert to tensor [0,1]
//            pipeline.add(new Normalize(                        // Normalize with ImageNet stats
//                    new float[]{0.485f, 0.456f, 0.406f},      // Mean
//                    new float[]{0.229f, 0.224f, 0.225f}       // Std
//            ));
//        }
//
//        @Override
//        public NDList processInput(TranslatorContext ctx, Image input) {
//            NDArray array = input.toNDArray(ctx.getNDManager(), Image.Flag.COLOR);
//            NDList inputList = new NDList(array);
//            return pipeline.transform(inputList);
//        }
//
//        @Override
//        public float[] processOutput(TranslatorContext ctx, NDList list) {
//            // Get the feature vector and squeeze dimensions
//            NDArray array = list.singletonOrThrow().squeeze();
//            return array.toFloatArray();
//        }
//    }
//}



import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.transform.CenterCrop;
import ai.djl.modality.cv.transform.Normalize;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.Pipeline;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.ByteBuffer;

@Service
@Slf4j
public class FeatureExtractionService {

    private ZooModel<Image, float[]> model;
    private static final int FEATURE_DIMENSION = 2048;

    @PostConstruct
    public void init() {
        log.info("🚀 Initializing ResNet50 feature extractor...");

        try {
            Criteria<Image, float[]> criteria = Criteria.builder()
                    .setTypes(Image.class, float[].class)
                    .optApplication(Application.CV.IMAGE_CLASSIFICATION)
                    .optArtifactId("resnet")
                    .optEngine("PyTorch")
                    .optTranslator(new FeatureTranslator())
                    .optProgress(new ProgressBar())
                    .build();

            log.info("📥 Downloading ResNet model from DJL Zoo...");
            model = criteria.loadModel();
            log.info("✅ ResNet50 loaded successfully!");

        } catch (ModelNotFoundException | MalformedModelException | IOException e) {
            log.error("❌ Failed to load ResNet50 model", e);
            log.error("Model loading failed. Feature extraction will not be available.");
            log.error("The app will continue, but similarity search won't work.");
        }
    }

    public float[] extractFeatures(Image image) throws TranslateException {
        if (model == null) {
            throw new IllegalStateException("Model not loaded - feature extraction unavailable");
        }

        try (Predictor<Image, float[]> predictor = model.newPredictor()) {
            float[] features = predictor.predict(image);
            return l2Normalize(features);
        } catch (Exception e) {
            log.error("Failed to extract features", e);
            throw new TranslateException("Feature extraction failed: " + e.getMessage(), e);
        }
    }

    public byte[] extractFeaturesAsBytes(Image image) throws TranslateException {
        float[] features = extractFeatures(image);
        return floatArrayToBytes(features);
    }

    private float[] l2Normalize(float[] features) {
        double sumSquares = 0.0;
        for (float f : features) {
            sumSquares += f * f;
        }

        double norm = Math.sqrt(sumSquares);
        if (norm < 1e-12) {
            log.warn("Feature vector has near-zero norm");
            return features;
        }

        float[] normalized = new float[features.length];
        for (int i = 0; i < features.length; i++) {
            normalized[i] = (float) (features[i] / norm);
        }

        return normalized;
    }

    public double calculateCosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null) {
            throw new IllegalArgumentException("Vectors cannot be null");
        }

        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException(
                    String.format("Vector lengths must match (got %d and %d)", vec1.length, vec2.length)
            );
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 < 1e-12 || norm2 < 1e-12) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    public byte[] floatArrayToBytes(float[] array) {
        ByteBuffer buffer = ByteBuffer.allocate(array.length * Float.BYTES);
        for (float value : array) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }

    public float[] bytesToFloatArray(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Byte array cannot be null or empty");
        }

        if (bytes.length % Float.BYTES != 0) {
            throw new IllegalArgumentException("Invalid byte array length");
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        float[] array = new float[bytes.length / Float.BYTES];
        for (int i = 0; i < array.length; i++) {
            array[i] = buffer.getFloat();
        }
        return array;
    }

    public boolean isModelLoaded() {
        return model != null;
    }

    public int getFeatureDimension() {
        return FEATURE_DIMENSION;
    }

    @PreDestroy
    public void cleanup() {
        if (model != null) {
            model.close();
            log.info("✓ ResNet model resources cleaned up");
        }
    }

    /**
     * FIXED: Feature extraction translator that properly applies preprocessing pipeline
     */
    private static class FeatureTranslator implements Translator<Image, float[]> {
        private final Pipeline pipeline;

        public FeatureTranslator() {
            pipeline = new Pipeline();
            // Standard ImageNet preprocessing
            pipeline.add(new Resize(256));
            pipeline.add(new CenterCrop(224, 224));
            pipeline.add(new ToTensor());  // This converts HWC -> CHW and scales to [0,1]
            pipeline.add(new Normalize(
                    new float[]{0.485f, 0.456f, 0.406f},  // ImageNet mean
                    new float[]{0.229f, 0.224f, 0.225f}   // ImageNet std
            ));
        }

        @Override
        public NDList processInput(TranslatorContext ctx, Image input) throws Exception {
            // CRITICAL FIX: Apply the preprocessing pipeline!
            NDArray array = input.toNDArray(ctx.getNDManager(), Image.Flag.COLOR);

            // Apply all transformations in the pipeline
            NDList processed = pipeline.transform(new NDList(array));

            return processed;
        }

        @Override
        public float[] processOutput(TranslatorContext ctx, NDList list) {
            // Get the output feature vector
            NDArray output = list.singletonOrThrow();

            // If it's a 2D array (batch_size, features), flatten it
            if (output.getShape().dimension() > 1) {
                output = output.flatten();
            }

            return output.toFloatArray();
        }
    }
}