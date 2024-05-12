from flask import Flask, request, render_template
import PIL

if int(PIL.__version__[0]) < 9:
    print('Mandatory restart: Execute this cell again!')
    import os
    os.kill(os.getpid(), 9)

app = Flask(__name__)
app.static_folder = 'static'

def upload_files():
    from google.colab import files
    from io import BytesIO

    uploaded = files.upload()
    return [(name, BytesIO(b)) for name, b in uploaded.items()]

from pix2tex import cli as pix2tex
from PIL import Image

model = pix2tex.LatexOCR()

from IPython.display import HTML, Math

display(HTML("<script src='https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.3/"
             "latest.js?config=default'></script>"))

table = r'\\begin{array} {l|l} %s \\end{array}'

@app.route('/')
def home():
    imgs = upload_files()
    predictions = []
    for name, f in imgs:
        img = Image.open(f)
        math = model(img)
        print(math)
        predictions.append('\\mathrm{%s} & \\displaystyle{%s}' % (name, math))
    latex_table = Math(table % '\\\\\\'.join(predictions))
    return render_template('index.html', latex_table=latex_table)

if __name__ == '__main__':
    app.run(debug=True)