import requests
import os

# URLs to try for the standard NSFW MobileNet V2 model
# This model typically has 5 classes: Drawing, Hentai, Neutral, Porn, Sexy
urls = [
    # GantMan Releases
    "https://github.com/GantMan/nsfw_model/releases/download/1.1.0/mobilenet_v2_140_224.tflite",
    "https://github.com/GantMan/nsfw_model/releases/download/1.1.0/nsfw_mobilenet_v2_140_224.tflite",
    "https://github.com/GantMan/nsfw_model/releases/download/1.0/mobilenet_v2_140_224.tflite",
    
    # Raw Content (Source Repos)
    "https://raw.githubusercontent.com/GantMan/nsfw_model/master/models/mobilenet_v2_140_224.tflite",
    "https://raw.githubusercontent.com/GantMan/nsfw_model/main/models/mobilenet_v2_140_224.tflite",
    
    # Mirrors / Projects using it
    "https://github.com/MaybeS/nsfw_tflite/raw/master/nsfw.tflite",
    "https://raw.githubusercontent.com/MaybeS/nsfw_tflite/master/nsfw.tflite",
    "https://raw.githubusercontent.com/tensorflow/tfjs-models/master/nsfwjs/mobilenet_v2/model.tflite",
    
    # Fallback to the one in temporary repo if I can find it logic... no.
]

target_dir = os.path.join("app", "src", "main", "assets")
target_file = os.path.join(target_dir, "haram_detector.tflite")

def download_model():
    if not os.path.exists(target_dir):
        os.makedirs(target_dir)

    for url in urls:
        print(f"⬇️ Attempting download from: {url}")
        try:
            # Fake user agent to avoid some blockers
            headers = {'User-Agent': 'Mozilla/5.0'}
            response = requests.get(url, headers=headers, stream=True, timeout=15)
            
            if response.status_code == 200:
                print(f"✅ Connection successful. Size: {response.headers.get('content-length')} bytes")
                
                # Verify size (should be > 1MB)
                content = response.content
                if len(content) < 100000: # 100KB
                    print("⚠️ File too small, likely invalid/redirect. Skipping.")
                    continue
                    
                with open(target_file, 'wb') as f:
                    f.write(content)
                print(f"🔥 Success! Model saved to {target_file}")
                return True
            else:
                print(f"❌ Failed with status: {response.status_code}")
                
        except Exception as e:
            print(f"❌ Error: {str(e)}")
            
    return False

if __name__ == "__main__":
    if download_model():
        print("🎉 HaramShield Model Acquired.")
    else:
        print("💀 All download attempts failed. Please verify internet connection.")
