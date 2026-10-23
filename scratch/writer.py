import sys, os, base64

if len(sys.argv) >= 3:
    path = sys.argv[1]
    b64 = sys.argv[2]
    content = base64.b64decode(b64).decode('utf-8')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content.strip() + '\n')
    print('Created:', path)
