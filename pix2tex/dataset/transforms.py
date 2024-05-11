# Import các thư viện cần thiết
import albumentations as alb  # Import albumentations với tên tắt alb
from albumentations.pytorch import ToTensorV2  # Import ToTensorV2 từ albumentations.pytorch

# Định nghĩa các phép biến đổi cho dữ liệu huấn luyện
train_transform = alb.Compose(
    [
        alb.Compose(
            [alb.ShiftScaleRotate(shift_limit=0, scale_limit=(-.15, 0), rotate_limit=1, border_mode=0, interpolation=3,
                                  value=[255, 255, 255], p=1),  # Áp dụng phép dịch, co giãn, xoay ảnh
             alb.GridDistortion(distort_limit=0.1, border_mode=0, interpolation=3, value=[255, 255, 255], p=.5)], p=.15),  # Áp dụng méo ảnh lưới
        alb.RGBShift(r_shift_limit=15, g_shift_limit=15,  # Tăng cường màu sắc
                     b_shift_limit=15, p=0.3),
        alb.GaussNoise(10, p=.2),  # Thêm nhiễu Gauss
        alb.RandomBrightnessContrast(.05, (-.2, 0), True, p=0.2),  # Tăng cường độ sáng và độ tương phản ngẫu nhiên
        alb.ImageCompression(95, p=.3),  # Nén ảnh
        alb.ToGray(always_apply=True),  # Chuyển ảnh sang ảnh xám
        alb.Normalize((0.7931, 0.7931, 0.7931), (0.1738, 0.1738, 0.1738)),  # Chuẩn hóa ảnh
        ToTensorV2(),  # Chuyển ảnh thành tensor
    ]
)

# Định nghĩa các phép biến đổi cho dữ liệu kiểm tra
test_transform = alb.Compose(
    [
        alb.ToGray(always_apply=True),  # Chuyển ảnh sang ảnh xám
        alb.Normalize((0.7931, 0.7931, 0.7931), (0.1738, 0.1738, 0.1738)),  # Chuẩn hóa ảnh
        ToTensorV2(),  # Chuyển ảnh thành tensor
    ]
)
