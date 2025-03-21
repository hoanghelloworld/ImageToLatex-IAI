# ImageToLatex-IAI

*The goal of this project is to create a learning based system that takes an image of a math formula and returns corresponding LaTeX code.*


**Team member**:

✨Nguyễn Huy Hoàng

✨Vũ Trung Hiếu
### How to run
- Install mobile app to run
### Performance

## Normal Test Results
- **BLEU**: 0.8003 ± 0.0144  
- **NED**: 0.8826 ± 0.0099  
- **Accuracy**: 0.5805 ± 0.0244  
- **Samples**: 1000  
- **Time**: 1835.31s  

## Handwritten Test Results
- **BLEU**: 0.6860 ± 0.0134  
- **NED**: 0.8181 ± 0.0096  
- **Accuracy**: 0.4489 ± 0.0223  
- **Samples**: 1000  
- **Time**: 1764.92s  

## Dataset
3.4 million image-text pairs, including both handwritten mathematical expressions (200,330 examples) and printed mathematical expressions (3,237,250 examples)

[27GB data](https://huggingface.co/datasets/hoang-quoc-trung/fusion-image-to-latex-datasets)

Printed mathematical expressions: We collect from Im2latex-100k dataset [1], I2L-140K Normalized dataset and Im2latex-90k Normalized dataset [2], Im2latex-170k dataset 3, Im2latex-230k dataset 4, latexformulas dataset 5 and Im2latex dataset 6.

Handwritten mathematical expressions: We collected data from the Competition on Recognition of Online Handwritten Mathematical Expressions (CROHME) dataset [7, 8, 9], Aida Calculus Math Handwriting Recognition Dataset [10] and Handwritten Mathematical Expression Convert LaTeX [11].

## References
[1] [An Image is Worth 16x16 Words](https://arxiv.org/abs/2010.11929)

[2] [Attention Is All You Need](https://arxiv.org/abs/1706.03762)

[3] [Image-to-Markup Generation with Coarse-to-Fine Attention](https://arxiv.org/abs/1609.04938v2)

[4] [Mobile-net-V3](https://arxiv.org/pdf/1905.02244)
