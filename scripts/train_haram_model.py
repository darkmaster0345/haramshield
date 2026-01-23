import os
import shutil
import kaggle
from mediapipe_model_maker import image_classifier
from datasets import load_dataset

# --- Configuration ---
DATA_DIR = 'data'
MODEL_FILENAME = 'haram_detector.tflite'
LABELS = ['NSFW', 'ALCOHOL', 'PORK', 'SAFE']

# Kaggle Datasets
KAGGLE_ALCOHOL = 'dataclusterlabs/alcohol-bottle-images-glass-bottles'
KAGGLE_PORK = 'namhoangtrinh/fresh-spoiled-meat'
KAGGLE_NSFW = 'dmitryyemelyanov/nsfw-dataset'
KAGGLE_SAFE = 'dansbecker/food-101'

def setup_directories():
    if os.path.exists(DATA_DIR):
        shutil.rmtree(DATA_DIR)
    for label in LABELS:
        os.makedirs(os.path.join(DATA_DIR, label), exist_ok=True)

def download_kaggle_dataset(dataset_name, target_dir):
    print(f"📥 Downloading {dataset_name}...")
    try:
        kaggle.api.dataset_download_files(dataset_name, path='temp_download', unzip=True)
        for root, _, files in os.walk('temp_download'):
            for file in files:
                if file.lower().endswith(('.png', '.jpg', '.jpeg')):
                    shutil.move(os.path.join(root, file), os.path.join(target_dir, file))
        shutil.rmtree('temp_download')
    except Exception as e:
        print(f"❌ Error downloading {dataset_name}: {e}")

def prepare_data():
    setup_directories()
    download_kaggle_dataset(KAGGLE_ALCOHOL, os.path.join(DATA_DIR, 'ALCOHOL'))
    download_kaggle_dataset(KAGGLE_PORK, os.path.join(DATA_DIR, 'PORK'))
    download_kaggle_dataset(KAGGLE_NSFW, os.path.join(DATA_DIR, 'NSFW'))
    
    # SAFE Data Subset
    download_kaggle_dataset(KAGGLE_SAFE, os.path.join(DATA_DIR, 'SAFE'))

def train_model():
    print("🧠 Loading Data into MediaPipe...")
    data = image_classifier.Dataset.from_folder(DATA_DIR)
    train_data, rest_data = data.split(0.8)
    validation_data, test_data = rest_data.split(0.5)

    print("🚀 Training HaramShield (MobileNet-V2)...")
    options = image_classifier.ImageClassifierOptions(
        supported_model=image_classifier.SupportedModels.MOBILENET_V2,
        hparams=image_classifier.HParams(epochs=20, batch_size=32, export_dir=".")
    )
    
    model = image_classifier.ImageClassifier.create(
        train_data=train_data,
        validation_data=validation_data,
        options=options
    )

    print("📊 Evaluating...")
    loss, acc = model.evaluate(test_data)
    print(f"✅ Accuracy: {acc:.2f}")

    print("📦 Exporting Model...")
    model.export_model(MODEL_FILENAME)
    print(f"🔥 Success! {MODEL_FILENAME} created.")

if __name__ == '__main__':
    prepare_data()
    train_model()