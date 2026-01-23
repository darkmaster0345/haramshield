import tensorflow as tf
import os

model_path = os.path.join("app", "src", "main", "assets", "haram_detector.tflite")

try:
    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()
    
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    
    print(f"✅ Valid TFLite Model found at {model_path}")
    print(f"Input Shape: {input_details[0]['shape']}")
    print(f"Output Shape: {output_details[0]['shape']}")
    print(f"Input Type: {input_details[0]['dtype']}")
    print(f"Output Type: {output_details[0]['dtype']}")
    
except Exception as e:
    print(f"❌ Model invalid or corrupt: {e}")
