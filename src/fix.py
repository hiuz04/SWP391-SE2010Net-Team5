import re

filepath = r'd:\Ky8\SWP\SWP391-SE2010Net-Team5\src\main\webapp\WEB-INF\admin\dashboard.jsp'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

fixes = [
    (r'\"Đã xác\s+nhận\"', '\"Đã xác nhận\"'),
    (r'\"bg-warning\s+text-dark\"', '\"bg-warning text-dark\"'),
    (r'\"Chờ\s+thanh toán\"', '\"Chờ thanh toán\"'),
    (r'\"Đang\s+đá\"', '\"Đang đá\"'),
    (r'\"Hoàn\s+thành\"', '\"Hoàn thành\"'),
    (r'\"Đã\s+hủy\"', '\"Đã hủy\"')
]

for p, r in fixes:
    content = re.sub(p, r, content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
print('Fixed dashboard.jsp')
