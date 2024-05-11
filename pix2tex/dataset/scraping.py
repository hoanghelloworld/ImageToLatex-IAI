import os
import sys
import random
from tqdm import tqdm
import html
import requests
import re
import argparse
import logging
from typing import Callable, List, Tuple
from pix2tex.dataset.extract_latex import find_math
# Biểu thức chính quy cho các thẻ HTML
htmltags = re.compile(r'<(noscript|script)>.*?<\/\1>', re.S)
# Biểu thức chính quy cho các liên kết trên Wikipedia
wikilinks = re.compile(r'href="/wiki/(.*?)"')
# Đường dẫn cơ sở của Wikipedia
wiki_base = 'https://en.wikipedia.org/wiki/'
# Biểu thức chính quy cho các liên kết trên Stack Exchange
stackexchangelinks = re.compile(r'(?:(https:\/\/\w+)\.stack\w+\.com|)\/questions\/(\d+\/[\w\d\/-]+)')
# Đường dẫn cơ sở của Stack Exchange về toán học
math_stack_exchange_base = 'https://math.stackexchange.com/questions/'
# Đường dẫn cơ sở của Stack Exchange về vật lý
physics_stack_exchange_base = 'https://physics.stackexchange.com/questions/'

# Hàm tìm kiếm đệ quy
def recursive_search(parser: Callable, seeds: List[str], depth: int = 2, skip: List[str] = [], unit: str = 'links', base_url: str = None, **kwargs) -> Tuple[List[str], List[str]]:
    """Tìm kiếm đệ quy. Tìm kiếm trong `seeds` để tìm các phép toán và các trang web tiếp theo.

    Args:
        parser (Callable): Một hàm trả về `Tuple[List[str], List[str]]` của các phép toán và các id (cho `base_url`) tương ứng.
        seeds (List[str]): Danh sách id ban đầu.
        depth (int, optional): Số lần lặp để tìm kiếm. Mặc định là 2.
        skip (List[str], optional): Danh sách id đã được ghé thăm. Mặc định là [].
        unit (str, optional): Mô tả đơn vị của thanh tiến trình. Mặc định là 'links'.
        base_url (str, optional): URL cơ sở để thêm id vào. Mặc định là None.

    Returns:
        Tuple[List[str],List[str]]: Trả về danh sách các phép toán được tìm thấy và các id đã ghé thăm tương ứng.
    """
    visited, links = set(skip), set(seeds)
    math = []
    try:
        for i in range(int(depth)):
            link_list = list(links)
            random.shuffle(link_list)
            t_bar = tqdm(link_list, initial=len(visited), unit=unit)
            for link in t_bar:
                if not link in visited:
                    t_bar.set_description('searching %s' % (link[:15]))
                    if base_url:
                        m, l = parser(base_url+link, **kwargs)
                    else:
                        m, l = parser(link, **kwargs)
                    # Kiểm tra xem chúng ta có thu được bất kỳ phép toán nào từ trang Wikipedia này không
                    # Nếu không, dừng tìm kiếm ở nhánh này
                    if len(m) > 0:
                        for li in l:
                            links.add(li)
                        # t_bar.total = len(links)
                        math.extend(m)
                    visited.add(link)
        return list(visited), list(set(math))
    except Exception as e:
        logging.debug(e)
        return list(visited), list(set(math))
    except KeyboardInterrupt:
        return list(visited), list(set(math))

# Hàm phân tích URL
def parse_url(url, encoding=None):
    r = requests.get(url)
    if r.ok:
        if encoding:
            r.encoding = encoding
        return html.unescape(re.sub(htmltags, '', r.text))
    return ''

# Hàm phân tích trang Wikipedia
def parse_wiki(url):
    text = parse_url(url)
    linked = list(set([l for l in re.findall(wikilinks, text) if not ':' in l]))
    return find_math(text, wiki=True), linked

# Hàm phân tích Stack Exchange
def parse_stack_exchange(url):
    text = parse_url(url)
    linked = list(set([l[1] for l in re.findall(stackexchangelinks, text) if url.startswith(l[0])]))
    return find_math(text, wiki=False), linked

# Hàm tìm kiếm đệ quy trên Wikipedia
def recursive_wiki(seeds, depth=4, skip=[], base_url=wiki_base):
    '''Tìm kiếm đệ quy trên Wikipedia để tìm các phép toán. Mỗi liên kết trên trang đầu `start` sẽ được ghé thăm trong lượt tiếp theo và cứ như vậy, cho đến khi không còn có
    phép toán nào trong trang con nữa. Quá trình này sẽ được lặp lại `depth` lần.'''
    start = [s.split('/')[-1] for s in seeds]
    return recursive_search(parse_wiki, start, depth, skip, base_url=base_url, unit=' links')

# Hàm tìm kiếm đệ quy trên Stack Exchange
def recursive_stack_exchange(seeds, depth=4, skip=[], base_url=math_stack_exchange_base):
    '''Tìm kiếm đệ quy qua các câu hỏi trên Stack Exchange'''
    start = [s.partition(base_url.split('//')[-1])[-1] for s in seeds]
    return recursive_search(parse_stack_exchange, start, depth, skip, base_url=base_url, unit=' questions')

# Hàm main
if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Trích xuất phép toán từ các trang web')
    parser.add_argument('-m', '--mode', default='auto', choices=['auto', 'wiki', 'math_stack', 'physics_stack'],
                        help='Website nào để thu thập. Lựa chọn: `auto` xác định bằng đầu vào, `wiki` Wikipedia, \
                        `math_stack` math.stackexchange, `physics_stack` physics.stackexchange.')
    parser.add_argument(nargs='*', dest='url', default=['https://en.wikipedia.org/wiki/Mathematics', 'https://en.wikipedia.org/wiki/Physics'],
                        help='URL bắt đầu. Mặc định: trang Wikipedia về Toán học và Vật lý')
    parser.add_argument('-o', '--out', default=os.path.join(os.path.dirname(os.path.realpath(__file__)), 'data'), help='Thư mục đầu ra')
    args = parser.parse_args()
    if '.' in args.out:
        args.out = os.path.dirname(args.out)
    # Xác định trang web
    if args.mode == 'auto':
        if len(args.url) == 0:
            raise ValueError('Cung cấp một URL bắt đầu')
        url = args.url[0]
        if re.search(wikilinks, url) is not None:
            args.mode = 'wiki'
        elif re.search(stackexchangelinks, url) is not None:
            if 'math' in url:
                args.mode = 'math_stack'
            elif 'physics' in url:
                args.mode = 'physics_stack'
        else:
            raise NotImplementedError('Không nhận diện được trang web')
    skips = os.path.join(args.out, f'visited_{args.mode}.txt')
    if os.path.exists(skips):
        skip = open(skips, 'r', encoding='utf-8').read().split('\n')
    else:
        skip = []
    try:
        if args.mode == 'physics_stack':
            visited, math = recursive_stack_exchange(args.url, base_url=physics_stack_exchange_base)
        elif args.mode == 'math_stack':
            visited, math = recursive_stack_exchange(args.url, base_url=math_stack_exchange_base)
        elif args.mode == 'wiki':
            visited, math = recursive_wiki(args.url)
    except KeyboardInterrupt:
        pass
    print('Tìm thấy %i trường hợp mã LaTeX. Lưu vào %s' % (len(math), args.out))
    for l, name in zip([visited, math], [f'visited_{args.mode}.txt', f'math_{args.mode}.txt']):
        f = os.path.join(args.out, name)
        if not os.path.exists(f):
            open(f, 'w').write('')
        f = open(f, 'a', encoding='utf-8')
        for element in l:
            f.write(element)
            f.write('\n')
        f.close()
