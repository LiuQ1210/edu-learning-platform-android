"""生成 DataStore Preferences 的 .preferences_pb 二进制内容。

仅用于本机调试：把伪造的登录态写进 emulator 里已安装的 debug 包私有目录，
以便在没有真实后端的情况下查看四个 Tab 的界面。

AndroidX DataStore（单文件 / FileStorage）的文件布局：

  offset 0..3 : Int32 LE       —— 内容的 CRC32 校验和（写入前先算好）
  offset 4..7 : Int32 LE       —— 内容字节数 N
  offset 8..  : N 字节         —— 序列化的 PreferenceMap（protobuf）

CRC32 只覆盖内容部分，不包含前面 8 字节头。

PreferenceMap 就是一个 map<string, Value>，每对键值的编码为：

  [keyLen : varint][key : UTF-8][valueLen : varint][Value]

Value 是个 oneof，各分支的 wire tag 实测如下（在 API 37 模拟器上逐个二分验证过）：

    boolean   -> 0x08
    float     -> 0x15
    int       -> 0x18
    long      -> 0x20
    string    -> 0x12   ← 本脚本用到
    stringSet -> 0x2A
    double    -> 0x31
    bytes     -> 0x3A

注意 string 不是「第 5 个字段号 5」，而是字段号 2，所以 tag = (2 << 3) | 2 = 0x12。
按直觉写成 0x2A 会被当成 stringSet，解析时抛 CorruptionException。
"""
import struct
import zlib
import sys

# Value oneof 各分支的 wire tag
TAG_BOOLEAN = 0x08
TAG_STRING = 0x12


def write_varint(value: int) -> bytes:
    out = bytearray()
    while True:
        byte = value & 0x7F
        value >>= 7
        if value:
            out.append(byte | 0x80)
        else:
            out.append(byte)
            return bytes(out)


def string_value(text: str) -> bytes:
    """编码 Value{ string = text }"""
    payload = text.encode("utf-8")
    return bytes([TAG_STRING]) + write_varint(len(payload)) + payload


def pair(key: str, text: str) -> bytes:
    key_bytes = key.encode("utf-8")
    value = string_value(text)
    return (
        write_varint(len(key_bytes)) + key_bytes
        + write_varint(len(value)) + value
    )


def build(prefs: dict) -> bytes:
    content = b"".join(pair(k, v) for k, v in prefs.items())
    crc = zlib.crc32(content) & 0xFFFFFFFF
    return struct.pack("<i", crc - (1 << 32) if crc >= (1 << 31) else crc) \
        + struct.pack("<i", len(content)) \
        + content


if __name__ == "__main__":
    # 与 UserPreferences.kt 的 Keys 一一对应
    prefs = {
        "access_token": "debug-fake-access-token-for-ui-preview",
        "refresh_token": "debug-fake-refresh-token-for-ui-preview",
        "user_id": "1001",
        "nickname": "张三",
        "avatar": "",
    }
    data = build(prefs)
    target = sys.argv[1] if len(sys.argv) > 1 else "learn_platform_prefs.preferences_pb"
    with open(target, "wb") as f:
        f.write(data)
    print(f"wrote {target}: {len(data)} bytes")
    print("header:", " ".join(f"{b:02x}" for b in data[:8]))
