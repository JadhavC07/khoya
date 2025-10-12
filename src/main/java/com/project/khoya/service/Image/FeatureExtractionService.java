package com.project.khoya.service.Image;

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

        try {
            Criteria<Image, float[]> criteria = Criteria.builder()
                    .setTypes(Image.class, float[].class)
                    .optApplication(Application.CV.IMAGE_CLASSIFICATION)
                    .optArtifactId("resnet")
                    .optEngine("PyTorch")
                    .optTranslator(new FeatureTranslator())
                    .optProgress(new ProgressBar())
                    .build();
            model = criteria.loadModel();

        } catch (ModelNotFoundException | MalformedModelException | IOException e) {
            log.error("Failed to load ResNet50 model", e);
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
        }
    }


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