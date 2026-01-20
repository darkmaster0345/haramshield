# HaramShield ML Models

This directory should contain the TensorFlow Lite model files:

## Required Models

1. **nsfw_model.tflite** - NudeNet-style NSFW detection model
   - Input: 224x224x3 RGB image
   - Output: 2 classes [Safe, NSFW]

2. **object_detection.tflite** - YOLOv10-style object detection model
   - Input: 320x320x3 RGB image
   - Output: Detection boxes with class IDs

## Obtaining Models

### Option 1: Use Pre-trained Models
- NudeNet: https://github.com/notAI-tech/NudeNet
- YOLOv10: https://github.com/ultralytics/ultralytics

### Option 2: Train Custom Models
Train on custom datasets for alcohol/tobacco detection.

## Converting to TFLite

```python
import tensorflow as tf

# Convert Keras model
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

with open('model.tflite', 'wb') as f:
    f.write(tflite_model)
```

## Labels File

Create `labels.txt` with one class per line for object detection:
```
bottle
wine glass
beer
cigarette
...
```
