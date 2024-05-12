from flask import Flask, render_template, request, jsonify
from PIL import Image
from pix2tex import cli as pix2tex
import io

app = Flask(__name__)
model = pix2tex.LatexOCR()

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/predict', methods=['POST'])
def predict():
    if 'file' not in request.files:
        return jsonify({'error': 'No file uploaded'})
    
    file = request.files['file']
    if file.filename == '':
        return jsonify({'error': 'No file selected'})
    
    try:
        img = Image.open(io.BytesIO(file.read()))
        math = model(img)
        return render_template('index.html', latex=math)
    except Exception as e:
        return jsonify({'error': str(e)})

if __name__ == '__main__':
    app.run(debug=True)