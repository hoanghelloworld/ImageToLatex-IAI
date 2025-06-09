# EduVision-IAI

## Introduction
EduVision-IAI is an Android application that converts mathematical formulas from images into LaTeX code. The project combines computer vision and machine learning to accurately recognize both handwritten and printed mathematical expressions, making it easier for students, educators, and researchers to digitize mathematical content.

### Key Features
- Convert images of mathematical formulas to LaTeX code
- Support for both handwritten and printed mathematical expressions
- Real-time LaTeX preview
- Built-in chat assistant for LaTeX help
- Modern and user-friendly interface

### Team Members
✨ Nguyễn Huy Hoàng - 22022584
✨ Vũ Trung Hiếu - 22022515

## Setup Tutorial

### 1. Server Setup
1. Clone the LaTeX Assistant Server repository:
   ```bash
   git clone https://github.com/hawkin-dono/Latex_Assistant_Server.git
   cd Latex_Assistant_Server
   ```

2. Install server dependencies:
   ```bash
   pip install -r requirements.txt
   ```

3. Install LaTeX dependencies:
   - For Unix systems:
     ```bash
     make
     make install
     ```
   - For Windows:
     - Install TeX Live or MiKTeX distribution
     - Ensure tex4ht system is included

4. Configure environment:
   - Create a `.env` file in the server root directory
   - Add your Together AI credentials:
     ```
     TOGETHER_API_KEY=your_api_key_here
     TOGETHER_MODEL_NAME=your_model_name_here
     ```

5. Start the server:
   ```bash
   uvicorn api:app --reload
   ```

### 2. Mobile App Setup

1. Get your server's IP address:
   - On Windows: Run `ipconfig` in command prompt
   - On Linux/Mac: Run `ifconfig` in terminal
   - Note down the IPv4 address (e.g., 192.168.1.6)

2. Configure server properties:
   - Open `EduVision/app/src/main/assets/server.properties`
   - Update the server URL with your IP address:
     ```properties
     server.url=http://192.168.1.6:8088
     server.endpoint=/image_to_latex
     server.render_endpoint=/render_latex
     server.chatbot_endpoint=/chatbot
     ```

3. Run the Android app:
   - Open the project in Android Studio
   - Build and run the application on your device or emulator


## License

MIT License

Copyright (c) 2024 EduVision-IAI

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
