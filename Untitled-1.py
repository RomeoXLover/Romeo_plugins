#!/usr/bin/env python3
import os
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parent
SERVER_DIR = ROOT / "local_server"
PLUGINS_DIR = SERVER_DIR / "plugins"
PAPER_JAR = ROOT / "paper" / "build" / "libs" / "paper-1.0.0.jar"


def ensure_java():
    java = shutil.which("java")
    if not java:
        raise SystemExit("Java is not installed or not in PATH. Install Java 21+ and try again.")


def download_paper_jar():
    # You can replace this URL with the exact Paper 1.21.1 build you want.
    # This script downloads the Paper jar directly.
    url = "https://api.papermc.io/v2/projects/paper/versions/1.21.1/builds/latest/downloads/paper-1.21.1.jar"
    target = SERVER_DIR / "paper.jar"

    if target.exists():
        return target

    print(f"Downloading Paper server jar to {target}...")
    import urllib.request

    SERVER_DIR.mkdir(parents=True, exist_ok=True)
    urllib.request.urlretrieve(url, target)
    return target


def write_eula():
    (SERVER_DIR / "eula.txt").write_text("eula=true\n", encoding="utf-8")


def write_server_properties():
    props = """\
server-name=Romeo Local Test
motd=Romeo Local Test
online-mode=false
max-players=20
server-port=25565
pvp=false
allow-flight=true
spawn-protection=0
enable-command-block=false
"""
    (SERVER_DIR / "server.properties").write_text(props, encoding="utf-8")


def install_plugin():
    PLUGINS_DIR.mkdir(parents=True, exist_ok=True)
    if not PAPER_JAR.exists():
        raise SystemExit(
            f"Plugin jar not found: {PAPER_JAR}\n"
            "Build it first with: ./gradlew shadowJar"
        )
    target = PLUGINS_DIR / PAPER_JAR.name
    shutil.copy2(PAPER_JAR, target)
    print(f"Installed plugin jar: {target}")


def start_server():
    jar = SERVER_DIR / "paper.jar"
    if not jar.exists():
        jar = download_paper_jar()

    command = [
        "java",
        "-Xms512M",
        "-Xmx1G",
        "-jar",
        str(jar),
        "--nogui",
    ]

    print("Starting Paper server...")
    print("Connect with: 127.0.0.1:25565")
    subprocess.run(command, cwd=SERVER_DIR)


def main():
    ensure_java()
    SERVER_DIR.mkdir(parents=True, exist_ok=True)
    write_eula()
    write_server_properties()
    install_plugin()
    start_server()


if __name__ == "__main__":
    main()
