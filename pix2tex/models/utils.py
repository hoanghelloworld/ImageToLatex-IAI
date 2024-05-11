import torch
import torch.nn as nn

from . import hybrid
from . import vit
from . import transformer


class Model(nn.Module):
    def __init__(self, encoder, decoder, args):
        super().__init__()
        self.encoder = encoder
        self.decoder = decoder
        self.args = args

    def data_parallel(self, x: torch.Tensor, device_ids, output_device=None, **kwargs):
        # Nếu không có device_ids hoặc chỉ có 1 device thì chạy bình thường
        if not device_ids or len(device_ids) == 1:
            return self(x, **kwargs)
        # Nếu không chỉ định output_device thì lấy device đầu tiên
        if output_device is None:
            output_device = device_ids[0]
        # Nhân bản model cho các device
        replicas = nn.parallel.replicate(self, device_ids)
        # Chia đều tensor đầu vào cho các device
        inputs = nn.parallel.scatter(x, device_ids)  
        # Nhân bản các tham số cho các device
        kwargs = nn.parallel.scatter(kwargs, device_ids)  
        replicas = replicas[:len(inputs)]
        kwargs = kwargs[:len(inputs)]
        # Chạy song song trên các device
        outputs = nn.parallel.parallel_apply(replicas, inputs, kwargs)
        # Gộp kết quả và tính trung bình
        return nn.parallel.gather(outputs, output_device).mean()

    def forward(self, x: torch.Tensor, tgt_seq: torch.Tensor,  **kwargs):
        # Mã hóa đầu vào bằng encoder
        encoded = self.encoder(x)
        # Giải mã bằng decoder
        out = self.decoder(tgt_seq, context=encoded, **kwargs)
        return out

    @torch.no_grad()
    def generate(self, x: torch.Tensor, temperature: float = 0.25):
        # Sinh câu với đầu vào x sử dụng beam search
        return self.decoder.generate((torch.LongTensor([self.args.bos_token]*len(x))[:, None]).to(x.device), self.args.max_seq_len,
                                     eos_token=self.args.eos_token, context=self.encoder(x), temperature=temperature)


def get_model(args):
    # Lấy encoder dựa trên cấu trúc được chỉ định
    if args.encoder_structure.lower() == 'vit':
        encoder = vit.get_encoder(args)
    elif args.encoder_structure.lower() == 'hybrid':
        encoder = hybrid.get_encoder(args)
    else:
        raise NotImplementedError('Encoder structure "%s" not supported.' % args.encoder_structure)
    # Lấy decoder
    decoder = transformer.get_decoder(args)
    # Chuyển encoder và decoder sang device
    encoder.to(args.device)
    decoder.to(args.device)
    # Khởi tạo model
    model = Model(encoder, decoder, args)
    # Nếu sử dụng wandb thì theo dõi model
    if args.wandb:
        import wandb
        wandb.watch(model)

    return model
