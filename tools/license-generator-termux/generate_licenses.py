#!/usr/bin/env python3
"""
Generator kode lisensi AetherX — versi ringan untuk Termux.

Tidak butuh firebase-admin atau package pip pihak ketiga sama sekali.
Hanya memakai modul bawaan Python (json, base64, hashlib, urllib) plus
binary `openssl` (via subprocess) untuk menandatangani JWT service account.

=== Setup (sekali saja di Termux) ===

    pkg update
    pkg install python openssl-tool

    # Ambil service account key:
    # Firebase Console -> Project Settings -> Service accounts ->
    # "Generate new private key" -> simpan sebagai serviceAccountKey.json
    # DI FOLDER YANG SAMA dengan skrip ini.
    #
    # PENTING: file ini kunci akses PENUH ke Firestore-mu (bypass semua
    # Security Rules). Jangan pernah dibagikan / diupload ke tempat publik.

=== Pemakaian ===

    python3 generate_licenses.py --count 50 --days 30
    python3 generate_licenses.py --count 10 --days 7 --prefix TRIAL
    python3 generate_licenses.py --count 5 --days 30 --dry-run
"""

import argparse
import base64
import json
import os
import secrets
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timedelta, timezone

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
KEY_PATH = os.path.join(SCRIPT_DIR, "serviceAccountKey.json")

# Alfabet tanpa karakter yang gampang ketuker (0/O, 1/I) supaya nyaman
# diketik manual oleh pengguna.
ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

FIRESTORE_SCOPE = "https://www.googleapis.com/auth/datastore"
TOKEN_URL = "https://oauth2.googleapis.com/token"


def b64url(data: bytes) -> str:
    """Base64url encode tanpa padding, sesuai spesifikasi JWT."""
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def random_segment(length: int) -> str:
    return "".join(secrets.choice(ALPHABET) for _ in range(length))


def generate_code(prefix: str) -> str:
    return f"{prefix}-{random_segment(4)}-{random_segment(4)}-{random_segment(4)}"


def load_service_account():
    if not os.path.exists(KEY_PATH):
        print(
            f"File serviceAccountKey.json tidak ditemukan di {KEY_PATH}.\n"
            "Lihat docstring di atas skrip ini untuk cara mendapatkannya, "
            "atau jalankan dengan --dry-run untuk melihat kode tanpa "
            "menulis ke Firestore.",
            file=sys.stderr,
        )
        sys.exit(1)
    with open(KEY_PATH, "r", encoding="utf-8") as f:
        return json.load(f)


def get_access_token(service_account: dict) -> str:
    """
    Mengambil OAuth2 access token dari Google lewat alur JWT Bearer
    (RFC 7523) — proses yang sama yang dilakukan firebase-admin di balik
    layar, tapi ditulis manual di sini supaya tidak butuh dependency berat.
    """
    client_email = service_account["client_email"]
    private_key_pem = service_account["private_key"]

    now = int(time.time())
    header = {"alg": "RS256", "typ": "JWT"}
    claims = {
        "iss": client_email,
        "scope": FIRESTORE_SCOPE,
        "aud": TOKEN_URL,
        "iat": now,
        "exp": now + 3600,
    }

    signing_input = f"{b64url(json.dumps(header).encode())}.{b64url(json.dumps(claims).encode())}"

    # Tulis private key ke file sementara (openssl butuh path file, bukan
    # stdin, untuk opsi -sign) di direktori yang hanya bisa dibaca user ini.
    key_file = os.path.join(SCRIPT_DIR, ".tmp_signing_key.pem")
    try:
        with open(key_file, "w", encoding="utf-8") as f:
            f.write(private_key_pem)
        os.chmod(key_file, 0o600)

        proc = subprocess.run(
            ["openssl", "dgst", "-sha256", "-sign", key_file],
            input=signing_input.encode("ascii"),
            capture_output=True,
        )
        if proc.returncode != 0:
            print("Gagal menandatangani JWT dengan openssl:", file=sys.stderr)
            print(proc.stderr.decode("utf-8", errors="replace"), file=sys.stderr)
            sys.exit(1)
        signature = proc.stdout
    finally:
        if os.path.exists(key_file):
            os.remove(key_file)

    jwt_assertion = f"{signing_input}.{b64url(signature)}"

    post_data = urllib.parse.urlencode(
        {
            "grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
            "assertion": jwt_assertion,
        }
    ).encode("ascii")

    req = urllib.request.Request(TOKEN_URL, data=post_data, method="POST")
    req.add_header("Content-Type", "application/x-www-form-urlencoded")

    try:
        with urllib.request.urlopen(req) as resp:
            token_response = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        print("Gagal menukar JWT ke access token:", file=sys.stderr)
        print(e.read().decode("utf-8", errors="replace"), file=sys.stderr)
        sys.exit(1)

    return token_response["access_token"]


def firestore_value(value):
    """Mengonversi nilai Python ke format Firestore REST API (`fields`)."""
    if value is None:
        return {"nullValue": None}
    if isinstance(value, bool):
        return {"booleanValue": value}
    if isinstance(value, str):
        return {"stringValue": value}
    if isinstance(value, datetime):
        return {"timestampValue": value.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%fZ")}
    if value == "SERVER_TIMESTAMP":
        # Firestore REST API tidak punya sentinel server-timestamp lewat
        # createDocument biasa seperti SDK — kita isi waktu lokal saat
        # request dikirim. Untuk `createdAt` ini cukup akurat (selisihnya
        # cuma round-trip network, bukan hal yang dicek rules).
        return {"timestampValue": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%fZ")}
    raise TypeError(f"Tipe tidak didukung: {type(value)}")


def create_license_document(project_id: str, access_token: str, code: str, expires_at: datetime):
    """
    Membuat SATU dokumen di collection `licenses` dengan document ID = kode
    lisensi, lewat Firestore REST API `createDocument` dengan `documentId`
    eksplisit.
    """
    url = (
        f"https://firestore.googleapis.com/v1/projects/{project_id}"
        f"/databases/(default)/documents/licenses?documentId={code}"
    )

    body = {
        "fields": {
            "deviceId": firestore_value(None),
            "status": firestore_value("unused"),
            "activatedAt": firestore_value(None),
            "expiresAt": firestore_value(expires_at),
            "createdAt": firestore_value("SERVER_TIMESTAMP"),
        }
    }

    req = urllib.request.Request(
        url, data=json.dumps(body).encode("utf-8"), method="POST"
    )
    req.add_header("Authorization", f"Bearer {access_token}")
    req.add_header("Content-Type", "application/json")

    try:
        with urllib.request.urlopen(req) as resp:
            resp.read()
        return True, None
    except urllib.error.HTTPError as e:
        return False, e.read().decode("utf-8", errors="replace")


def main():
    parser = argparse.ArgumentParser(description="Generator kode lisensi AetherX (Termux-friendly)")
    parser.add_argument("--count", type=int, required=True, help="Jumlah kode yang dibuat")
    parser.add_argument("--days", type=int, required=True, help="Masa berlaku dalam hari sejak sekarang")
    parser.add_argument("--prefix", default="AETX", help="Awalan kode (default: AETX)")
    parser.add_argument("--out", default=None, help="Nama file CSV hasil")
    parser.add_argument("--dry-run", action="store_true", help="Tampilkan kode tanpa menulis ke Firestore")
    args = parser.parse_args()

    if args.count <= 0:
        print("--count harus lebih dari 0", file=sys.stderr)
        sys.exit(1)
    if args.days <= 0:
        print("--days harus lebih dari 0", file=sys.stderr)
        sys.exit(1)

    codes = set()
    while len(codes) < args.count:
        codes.add(generate_code(args.prefix))
    code_list = sorted(codes)

    now = datetime.now(timezone.utc)
    expires_at = now + timedelta(days=args.days)

    print(f"Akan membuat {args.count} kode lisensi:")
    print(f"  Prefix       : {args.prefix}")
    print(f"  Masa berlaku : {args.days} hari (kadaluarsa {expires_at.isoformat()})")
    print(f"  Mode         : {'DRY RUN (tidak menulis ke Firestore)' if args.dry_run else 'LIVE (menulis ke Firestore)'}")
    print()

    if args.dry_run:
        for code in code_list:
            print(code)
    else:
        service_account = load_service_account()
        project_id = service_account["project_id"]

        expected_project_id = os.environ.get("EXPECTED_PROJECT_ID")
        if expected_project_id and project_id != expected_project_id:
            print(
                f"serviceAccountKey.json ini untuk project Firebase \"{project_id}\", "
                f"tapi EXPECTED_PROJECT_ID diisi \"{expected_project_id}\".\n"
                "Kode lisensi yang dibuat TIDAK akan terlihat di aplikasi Android kalau "
                "project-nya beda. Ambil ulang serviceAccountKey.json dari project yang benar, "
                "atau jalankan dengan `EXPECTED_PROJECT_ID=... python3 generate_licenses.py ...`.",
                file=sys.stderr,
            )
            sys.exit(1)
        if not expected_project_id:
            print(
                f"Peringatan: EXPECTED_PROJECT_ID belum di-set — kode akan ditulis ke project "
                f"\"{project_id}\" tanpa verifikasi. Pastikan ini project Firebase yang sama "
                "dengan project_id di app/google-services.json aplikasi Android-mu.\n",
                file=sys.stderr,
            )

        print("Mengambil access token dari Google OAuth2...")
        access_token = get_access_token(service_account)
        print("Access token diperoleh. Menulis dokumen ke Firestore...\n")

        success_count = 0
        for i, code in enumerate(code_list, start=1):
            ok, error = create_license_document(project_id, access_token, code, expires_at)
            if ok:
                success_count += 1
                print(f"  [{i}/{len(code_list)}] OK   {code}")
            else:
                print(f"  [{i}/{len(code_list)}] GAGAL {code}: {error}", file=sys.stderr)

        print(f"\nSelesai: {success_count}/{len(code_list)} kode berhasil ditulis ke Firestore.")
        if success_count < len(code_list):
            print(
                "Sebagian kode GAGAL ditulis — cek pesan error di atas "
                "(umumnya karena token kadaluarsa untuk batch besar, atau "
                "izin service account belum tepat).",
                file=sys.stderr,
            )

    out_path = args.out or f"licenses-{now.strftime('%Y%m%dT%H%M%SZ')}.csv"
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("code,expires_at\n")
        for code in code_list:
            f.write(f"{code},{expires_at.isoformat()}\n")
    print(f"Daftar kode disimpan ke: {out_path}")


if __name__ == "__main__":
    main()
