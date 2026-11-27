"""
智能体 WebSocket 流式链路测试（直连后端，不启动本地服务）

用法：
  python agent-ws-stream-test.py
  python agent-ws-stream-test.py --base-url http://192.168.100.3:1000 --phone 13800001234 --password Test123456

覆盖：
  登录 → WS 握手(/ws/forum?token=) → 订阅 user:{userId} →
  POST /agent/chat/stream → 收到 agent.start / agent.delta / agent.done

为什么不用现成 websocket 库：
  只用标准库，避免额外安装；同时能精确控制握手头（部分网关对 Connection 头很敏感）。
"""
import argparse
import base64
import json
import os
import socket
import struct
import threading
import time
import urllib.parse
import urllib.request


def api_json(base_url, path, body=None, token=None, method="POST"):
    data = None
    headers = {"Content-Type": "application/json; charset=utf-8"}
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(base_url + path, data=data, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8", "replace"))


def login(base_url, phone, password):
    try:
        r = api_json(base_url, "/api/v1/login", {"phone": phone, "password": password})
    except Exception:  # noqa: BLE001
        r = {"code": -1}
    if r.get("code") != 0 or not r.get("token"):
        try:
            api_json(base_url, "/api/v1/register",
                     {"phone": phone, "password": password, "confirmPassword": password})
            print("[register] ok")
        except Exception as exc:  # noqa: BLE001
            print("[register]", exc)
        r = api_json(base_url, "/api/v1/login", {"phone": phone, "password": password})
    if r.get("code") != 0:
        raise SystemExit("登录失败：" + str(r.get("msg")))
    print("[login] userId=%s" % r.get("userId"))
    return r["token"], str(r["userId"])


def send_text(sock, text):
    data = text.encode("utf-8")
    header = bytearray([0x81])
    mask = os.urandom(4)
    n = len(data)
    if n < 126:
        header.append(0x80 | n)
    elif n < 65536:
        header.append(0x80 | 126)
        header += struct.pack(">H", n)
    else:
        header.append(0x80 | 127)
        header += struct.pack(">Q", n)
    header += mask
    sock.sendall(bytes(header) + bytes(b ^ mask[i % 4] for i, b in enumerate(data)))


def read_exact(sock, n):
    buf = b""
    while len(buf) < n:
        chunk = sock.recv(n - len(buf))
        if not chunk:
            raise EOFError
        buf += chunk
    return buf


def read_frame(sock):
    b1, b2 = read_exact(sock, 2)
    opcode = b1 & 0x0F
    length = b2 & 0x7F
    if length == 126:
        length = struct.unpack(">H", read_exact(sock, 2))[0]
    elif length == 127:
        length = struct.unpack(">Q", read_exact(sock, 8))[0]
    masked = b2 & 0x80
    mask = read_exact(sock, 4) if masked else b""
    payload = read_exact(sock, length)
    if masked:
        payload = bytes(b ^ mask[i % 4] for i, b in enumerate(payload))
    return opcode, payload


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://192.168.100.3:1000")
    parser.add_argument("--phone", default="13800001234")
    parser.add_argument("--password", default="Test123456")
    parser.add_argument("--question", default="用三句话说明 TCP 三次握手的作用")
    args = parser.parse_args()

    base = args.base_url.rstrip("/")
    token, user_id = login(base, args.phone, args.password)

    host = urllib.parse.urlparse(base)
    port = host.port or (443 if host.scheme == "https" else 80)
    path = "/ws/forum?token=" + urllib.parse.quote(token)
    key = base64.b64encode(os.urandom(16)).decode()
    sock = socket.create_connection((host.hostname, port), timeout=10)
    sock.sendall((
        "GET " + path + " HTTP/1.1\r\n"
        "Host: " + host.netloc + "\r\n"
        "Upgrade: websocket\r\n"
        "Connection: Upgrade\r\n"
        "Sec-WebSocket-Key: " + key + "\r\n"
        "Sec-WebSocket-Version: 13\r\n"
        "Origin: " + base + "\r\n\r\n"
    ).encode())
    resp = b""
    while b"\r\n\r\n" not in resp:
        resp += sock.recv(1024)
    status = resp.decode("latin-1").split("\r\n")[0]
    print("[handshake]", status)
    if "101" not in status:
        raise SystemExit("WebSocket 握手失败：" + status)

    send_text(sock, json.dumps({"action": "subscribe", "topics": ["user:" + user_id]}))
    time.sleep(0.4)

    result = {"start": False, "delta": 0, "done": False}

    def submit():
        try:
            r = api_json(base, "/api/v1/agent/chat/stream", {"question": args.question}, token)
            print("[submit] requestId=%s sessionId=%s" % (r.get("requestId"), r.get("sessionId")))
        except Exception as exc:  # noqa: BLE001
            print("[submit-error]", exc)

    threading.Thread(target=submit, daemon=True).start()

    sock.settimeout(20)
    deadline = time.time() + 60
    while time.time() < deadline:
        try:
            opcode, payload = read_frame(sock)
        except Exception as exc:  # noqa: BLE001
            print("[frame-error]", type(exc).__name__, exc)
            break
        if opcode == 0x8:
            break
        if opcode not in (0x1, 0x2):
            continue
        try:
            event = json.loads(payload.decode("utf-8", "replace"))
        except Exception:  # noqa: BLE001
            continue
        etype = event.get("type", "")
        data = event.get("data") or event
        if etype == "agent.start":
            result["start"] = True
            print("[start] requestId=%s sessionId=%s" % (data.get("requestId"), data.get("sessionId")))
        elif etype == "agent.delta":
            result["delta"] += 1
            print("[delta] seq=%s content=%r" % (data.get("seq"), (data.get("content") or "")[:40]))
        elif etype == "agent.done":
            result["done"] = True
            print("[done] messageId=%s answerLen=%s refs=%s" % (
                data.get("messageId"), len(data.get("answer") or ""), len(data.get("refs") or [])))
            break
        elif etype == "agent.error":
            print("[error]", data.get("message"))
            break

    sock.close()
    ok = result["start"] and result["delta"] > 0 and result["done"]
    print("\nRESULT: %s (start=%s delta=%s done=%s)" % (
        "PASS" if ok else "FAIL", result["start"], result["delta"], result["done"]))
    raise SystemExit(0 if ok else 1)


if __name__ == "__main__":
    main()
