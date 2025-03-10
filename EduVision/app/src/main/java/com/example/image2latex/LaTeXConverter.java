package com.example.image2latex;
import org.pytorch.Tensor;
import android.content.Context;
import org.json.JSONObject;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
// Add this near the top of LaTeXConverter.java
import android.util.Log;


public class LaTeXConverter {
    private final Module encoder;
    private final Module decoder;
    private final Map<String, Integer> vocab;
    private final Map<Integer, String> reverseVocab;
    private static final int MAX_LENGTH = 200;
    private static final int BEAM_SIZE = 5;
    private volatile boolean cancelRequested = false;
    public void cancelConversion() {
        cancelRequested = true;
        Log.d("LaTeXConverter", "Cancellation requested");
    }
    
    // Add this method to reset cancellation state
    public void resetCancellation() {
        cancelRequested = false;
    }


    private String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        Log.d("LaTeXConverter", "Checking file: " + file.getAbsolutePath());
        
        if (!file.exists() || file.length() == 0) {
            Log.d("LaTeXConverter", "File doesn't exist or is empty, copying from assets...");
            file.delete(); // Delete if exists but empty
            
            try (InputStream is = context.getAssets().open(assetName)) {
                if (is == null) {
                    throw new IOException("Failed to open asset: " + assetName);
                }
                
                try (FileOutputStream os = new FileOutputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    long total = 0;
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                        total += read;
                    }
                    os.flush();
                    Log.d("LaTeXConverter", "Copied " + total + " bytes for " + assetName);
                }
            }
            
            if (!file.exists() || file.length() == 0) {
                throw new IOException("Failed to create file or file is empty: " + file.getAbsolutePath());
            }
        }
        
        Log.d("LaTeXConverter", "Using model file: " + file.getAbsolutePath() + " (size: " + file.length() + " bytes)");
        return file.getAbsolutePath();
    }
    public LaTeXConverter(Context context) {
        try {
            Log.d("LaTeXConverter", "Internal files directory: " + context.getFilesDir().getAbsolutePath());
            String[] assets = context.getAssets().list("");
            Log.d("LaTeXConverter", "Available assets: " + String.join(", ", assets));

            // Initialize maps
            vocab = new HashMap<>();
            reverseVocab = new HashMap<>();

            // Load vocabulary first
            Log.d("LaTeXConverter", "Loading vocabulary...");
            Map<String, Integer> loadedVocab = loadVocabulary(context);
            vocab.putAll(loadedVocab); // Copy loaded vocabulary to class field
            reverseVocab.putAll(createReverseVocab(loadedVocab));

            if (vocab.isEmpty()) {
                throw new RuntimeException("Failed to load vocabulary");
            }
            Log.d("LaTeXConverter", "Vocabulary loaded with " + vocab.size() + " tokens");

    
            // Load encoder
            String encoderPath = assetFilePath(context, "encoder_traced.ptl");
            Log.d("LaTeXConverter", "Loading encoder from: " + encoderPath);
            File encoderFile = new File(encoderPath);
            if (!encoderFile.exists() || encoderFile.length() == 0) {
                throw new IOException("Encoder file is missing or empty");
            }
            encoder = Module.load(encoderPath);
            if (encoder == null) {
                throw new RuntimeException("Failed to load encoder model");
            }
    
            // Load decoder
            String decoderPath = assetFilePath(context, "decoder_traced.ptl");
            Log.d("LaTeXConverter", "Loading decoder from: " + decoderPath);
            File decoderFile = new File(decoderPath);
            if (!decoderFile.exists() || decoderFile.length() == 0) {
                throw new IOException("Decoder file is missing or empty");
            }
            decoder = Module.load(decoderPath);
            if (decoder == null) {
                throw new RuntimeException("Failed to load decoder model");
            }
    
            Log.d("LaTeXConverter", "Initialization complete");
        } catch (Exception e) {
            Log.e("LaTeXConverter", "Error initializing converter", e);
            throw new RuntimeException("Error loading models: " + e.getMessage(), e);
        }
    }
    private Map<String, Integer> loadVocabulary(Context context) throws IOException {
        Map<String, Integer> vocabMap = new HashMap<>();
        try {
            Log.d("LaTeXConverter", "Opening tokenizer.json...");
            InputStream is = context.getAssets().open("tokenizer.json");
            int size = is.available();
            Log.d("LaTeXConverter", "tokenizer.json size: " + size + " bytes");

            byte[] buffer = new byte[size];
            int bytesRead = is.read(buffer);
            is.close();
            
            Log.d("LaTeXConverter", "Read " + bytesRead + " bytes from tokenizer.json");
            String json = new String(buffer, "UTF-8");
            
            JSONObject jsonObject = new JSONObject(json);
            JSONObject vocab = jsonObject.getJSONObject("vocab");
            
            Iterator<String> keys = vocab.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                int value = vocab.getInt(key);
                vocabMap.put(key, value);
            }
            
            Log.d("LaTeXConverter", "Loaded " + vocabMap.size() + " vocabulary items");
        } catch (Exception e) {
            Log.e("LaTeXConverter", "Error loading vocabulary", e);
            throw new IOException("Error loading vocabulary: " + e.getMessage(), e);
        }
        return vocabMap;
    }

    private Map<Integer, String> createReverseVocab(Map<String, Integer> vocab) {
        Map<Integer, String> reverse = new HashMap<>();
        for (Map.Entry<String, Integer> entry : vocab.entrySet()) {
            reverse.put(entry.getValue(), entry.getKey());
        }
        return reverse;
    }
    // Add this method near the top of the class
    private void validateInputTensor(float[] tensorData) {
        if (tensorData == null) {
            throw new IllegalArgumentException("Input tensor cannot be null");
        }
        
        if (tensorData.length != 150 * 700) {
            throw new IllegalArgumentException("Input tensor must be of size 150x700, got " + tensorData.length);
        }
        
        // Check if tensor contains valid values (0.0 or 1.0)
        boolean hasValidValues = false;
        for (float val : tensorData) {
            if (val != 0.0f && val != 1.0f) {
                Log.w("LaTeXConverter", "Tensor contains non-binary value: " + val);
            }
            if (val > 0.0f) {
                hasValidValues = true;
            }
        }
        
        if (!hasValidValues) {
            throw new IllegalArgumentException("Input tensor contains no positive values");
        }
    }


    public String convert(float[] tensorData) {
        resetCancellation();
        Log.d("LaTeXConverter", "Starting conversion...");
// At the beginning of the convert method
        Log.d("LaTeXConverter", "Starting conversion with tensor size: " + tensorData.length);
        Log.d("LaTeXConverter", "Expected tensor shape: [1, 1, 150, 700]");
        validateInputTensor(tensorData);
        
        // Create input tensor [1, 1, 150, 700] - NOT [1, 1, 1, 150, 700]
        float[] reshapedData = new float[1 * 1 * 150 * 700];
        System.arraycopy(tensorData, 0, reshapedData, 0, tensorData.length);
        
        // Fix: Create a proper 4D tensor instead of 5D
// Modify your tensor creation:
// Instead of:
// float[] reshapedData = new float[1 * 1 * 150 * 700];
// System.arraycopy(tensorData, 0, reshapedData, 0, tensorData.length);
// Tensor inputTensor = Tensor.fromBlob(reshapedData, new long[]{1, 1, 150, 700});

// Use:
        Tensor inputTensor = Tensor.fromBlob(tensorData, new long[]{1, 150, 700});
        Tensor features = null;
        
        try {
            // Encoder forward pass
            Log.d("LaTeXConverter", "Created input tensor of shape [1, 1, 150, 700]");
            IValue encoderOutput = encoder.forward(IValue.from(inputTensor));
            Log.d("LaTeXConverter", "Encoder forward pass completed");
            features = encoderOutput.toTensor();
            // Initialize beam search
            ArrayList<Integer> startSequence = new ArrayList<>();
            startSequence.add(vocab.get("<START>"));
            BeamNode initialBeam = new BeamNode(startSequence, 0.0f);
            
            PriorityQueue<BeamNode> beams = new PriorityQueue<>();
            beams.add(initialBeam);
            
            List<BeamNode> completed = new ArrayList<>();
            
            // Beam search process
            Log.d("LaTeXConverter", "Starting beam search with beam size: " + BEAM_SIZE);
            for (int step = 0; step < MAX_LENGTH; step++) {
                if (beams.isEmpty()) {
                    Log.d("LaTeXConverter", "Beam queue empty at step " + step);
                    break;
                }
                if (cancelRequested) {
                    Log.d("LaTeXConverter", "Conversion cancelled at step " + step);
                    return "Conversion cancelled";
                }
                PriorityQueue<BeamNode> candidates = new PriorityQueue<>();
                int beamsProcessed = 0;
                
                while (!beams.isEmpty() && beamsProcessed < BEAM_SIZE) {
                    BeamNode beam = beams.poll();
                    beamsProcessed++;
                    
                    // If end token is generated, add to completed list
                    if (beam.sequence.size() > 1 && beam.lastToken() == vocab.get("<END>")) {
                        completed.add(beam);
                        Log.d("LaTeXConverter", "Found completed beam at step " + step);
                        continue;
                    }
                    
                    // Prepare input for decoder
                    long[] seqArray = new long[beam.sequence.size()];
                    for (int i = 0; i < beam.sequence.size(); i++) {
                        seqArray[i] = beam.sequence.get(i);
                    }
                    
                    Tensor seqTensor = null;
                    try {
                        seqTensor = Tensor.fromBlob(seqArray, new long[]{1, beam.sequence.size()});
                        
                        // Decoder forward pass
                        IValue[] inputs = new IValue[]{IValue.from(features), IValue.from(seqTensor)};
                        IValue output = decoder.forward(inputs);
                        Tensor logits = null;
                        
                        try {
                            logits = output.toTensor();
                            float[] logitsArray = logits.getDataAsFloatArray();
                            int lastTimeStep = beam.sequence.size() - 1;
                            int vocabSize = vocab.size();
                            
                            // Get logits for the last time step
                            float[] lastLogits = new float[vocabSize];
                            System.arraycopy(logitsArray, lastTimeStep * vocabSize, lastLogits, 0, vocabSize);
                            
                            // Process logits and add new candidates
                            processLogitsAndAddCandidates(lastLogits, beam, candidates);
                        } finally {
                            // Let garbage collection handle tensor cleanup
                            if (logits != null) {
                                Log.d("LaTeXConverter", "Finished processing logits tensor");
                            }
                        }
                    } catch (Exception e) {
                        Log.e("LaTeXConverter", "Error in decoder forward pass", e);
                    } finally {
                        // Let garbage collection handle tensor cleanup
                        if (seqTensor != null) {
                            Log.d("LaTeXConverter", "Finished processing sequence tensor");
                        }
                    }
                }
                    
                // Early stopping if we have enough completed sequences
                if (completed.size() >= BEAM_SIZE) {
                    Log.d("LaTeXConverter", "Early stopping at step " + step + " with " + completed.size() + " completed sequences");
                    break;
                }
                
                // Reset beams for next iteration
                beams = new PriorityQueue<>();
                int count = 0;
                while (!candidates.isEmpty() && count < BEAM_SIZE) {
                    beams.add(candidates.poll());
                    count++;
                }
                
                Log.d("LaTeXConverter", "Step " + step + " complete with " + beams.size() + " beams for next iteration");
            }
            
            // Add any remaining beams to completed list
            while (!beams.isEmpty()) {
                BeamNode beam = beams.poll();
                if (beam.sequence.size() > 1) {
                    completed.add(beam);
                }
            }
            
            Log.d("LaTeXConverter", "Beam search completed with " + completed.size() + " candidates");
            return processResults(completed, beams);
            
        } catch (Exception e) {
            Log.e("LaTeXConverter", "Error during conversion", e);
            e.printStackTrace(); // Print full stack trace for debugging
            return "Error: " + e.getMessage();
        } finally {
            // Ensure resources are properly released
            if (inputTensor != null || features != null) {
                Log.d("LaTeXConverter", "Letting garbage collector handle tensors");
                // Let garbage collection handle the tensors
                // No explicit calls to close() or release() as they're not available in this version
            }
        }
    }

    private void processLogitsAndAddCandidates(float[] logits, BeamNode beam, PriorityQueue<BeamNode> candidates) {
        // Apply softmax
        float max = Float.NEGATIVE_INFINITY;
        for (float val : logits) {
            if (val > max) max = val;
        }
        
        float sum = 0.0f;
        float[] probs = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            probs[i] = (float) Math.exp(logits[i] - max);
            sum += probs[i];
        }
        
        for (int i = 0; i < probs.length; i++) {
            probs[i] = (float) Math.log(probs[i] / sum);
        }
        
        // Get top-k indices
        int[] topK = getTopK(probs, BEAM_SIZE);
        
        // Create new candidates
        for (int tokenId : topK) {
            ArrayList<Integer> newSeq = new ArrayList<>(beam.sequence);
            newSeq.add(tokenId);
            float newScore = beam.score + probs[tokenId];
            candidates.add(new BeamNode(newSeq, newScore));
        }
    }

    private String processResults(List<BeamNode> completed, PriorityQueue<BeamNode> beams) {
        // Find best sequence
        BeamNode best = null;
        if (!completed.isEmpty()) {
            // Sort completed beams by score
            Collections.sort(completed);
            best = completed.get(0);
        } else if (!beams.isEmpty()) {
            best = beams.peek();
        }
        
        if (best == null) {
            return "Failed to generate LaTeX";
        }
        
        // Convert tokens to LaTeX string
        StringBuilder result = new StringBuilder();
        Log.d("LaTeXConverter", "Best sequence length: " + best.sequence.size());
        
        for (int i = 1; i < best.sequence.size(); i++) {
            int token = best.sequence.get(i);
            if (token == vocab.get("<END>")) {
                break;
            }
            
            String word = reverseVocab.get(token);
            if (word != null && !word.equals("<PAD>") && !word.equals("<START>") && !word.equals("<UNK>")) {
                result.append(word).append(" ");
                Log.d("LaTeXConverter", "Added token: " + word);
            }
        }
        
        Log.d("LaTeXConverter", "Final result: " + result.toString());
        return result.toString().trim();
    }
    
    private static class BeamNode implements Comparable<BeamNode> {
        final ArrayList<Integer> sequence;
        final float score;
        
        BeamNode(ArrayList<Integer> sequence, float score) {
            this.sequence = sequence;
            this.score = score;
        }
        
        int lastToken() {
            return sequence.get(sequence.size() - 1);
        }
        
        @Override
        public int compareTo(BeamNode other) {
            return Float.compare(other.score, this.score); // Descending order
        }
    }
    
    private int[] getTopK(float[] array, int k) {
        // Create index-value pairs
        Integer[] indices = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            indices[i] = i;
        }
        
        // Sort by values
        Arrays.sort(indices, (a, b) -> Float.compare(array[b], array[a]));
        
        // Take top k
        int[] result = new int[Math.min(k, indices.length)];
        for (int i = 0; i < result.length; i++) {
            result[i] = indices[i];
        }
        
        return result;
    }
}