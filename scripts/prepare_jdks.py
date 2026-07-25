#!/usr/bin/env python3
"""Download and normalize packaged JDKs into the repository-local jdks/ cache."""

from __future__ import annotations

import argparse
import json
import shutil
import sys
import tarfile
import tempfile
import time
import urllib.request
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


@dataclass(frozen=True)
class TargetSpec:
    key: str
    platform_dir: str
    arch_dir: str
    adoptium_os: str
    adoptium_arch: str
    archive_type: str

    @property
    def archive_suffix(self) -> str:
        return "zip" if self.archive_type == "zip" else "tar.gz"


TARGETS = {
    "mac-x64": TargetSpec("mac-x64", "mac", "x64", "mac", "x64", "tar.gz"),
    "mac-arm64": TargetSpec("mac-arm64", "mac", "arm64", "mac", "aarch64", "tar.gz"),
    "windows-x64": TargetSpec("windows-x64", "windows", "x64", "windows", "x64", "zip"),
    "linux-x64": TargetSpec("linux-x64", "linux", "x64", "linux", "x64", "tar.gz"),
}


def log(message: str) -> None:
    print(message, flush=True)


def format_size(num_bytes: int) -> str:
    value = float(num_bytes)
    for unit in ("B", "KiB", "MiB", "GiB", "TiB"):
        if value < 1024 or unit == "TiB":
            if unit == "B":
                return f"{int(value)} {unit}"
            return f"{value:.1f} {unit}"
        value /= 1024


def build_download_url(version: str, spec: TargetSpec) -> str:
    return (
        f"https://api.adoptium.net/v3/binary/latest/{version}/ga/"
        f"{spec.adoptium_os}/{spec.adoptium_arch}/jdk/hotspot/normal/eclipse"
    )


def java_binary_candidates(home_dir: Path) -> list[Path]:
    return [home_dir / "bin" / "java", home_dir / "bin" / "java.exe"]


def has_java_binary(home_dir: Path) -> bool:
    return any(candidate.exists() for candidate in java_binary_candidates(home_dir))


def parse_targets(raw_targets: str) -> list[TargetSpec]:
    requested = [item.strip() for item in raw_targets.split(",") if item.strip()]
    if not requested:
        raise ValueError("No targets were provided.")
    if requested == ["all"]:
        return list(TARGETS.values())

    unknown = [item for item in requested if item not in TARGETS]
    if unknown:
        supported = ", ".join(sorted(TARGETS))
        raise ValueError(f"Unsupported targets: {', '.join(unknown)}. Supported: {supported}")

    return [TARGETS[item] for item in requested]


def safe_extract_tar(archive_path: Path, destination: Path) -> None:
    with tarfile.open(archive_path, "r:gz") as archive:
        for member in archive.getmembers():
            member_path = destination / member.name
            resolved = member_path.resolve()
            if not str(resolved).startswith(str(destination.resolve())):
                raise RuntimeError(f"Refusing to extract unsafe archive entry: {member.name}")
        archive.extractall(destination)


def safe_extract_zip(archive_path: Path, destination: Path) -> None:
    with zipfile.ZipFile(archive_path) as archive:
        for member in archive.infolist():
            member_path = destination / member.filename
            resolved = member_path.resolve()
            if not str(resolved).startswith(str(destination.resolve())):
                raise RuntimeError(f"Refusing to extract unsafe archive entry: {member.filename}")
        archive.extractall(destination)


def download_file(url: str, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": "WePush-JDK-Prep/1.0",
            "Accept": "application/octet-stream",
        },
    )
    with urllib.request.urlopen(request, timeout=120) as response, destination.open("wb") as output:
        total_size_header = response.headers.get("Content-Length")
        total_size = int(total_size_header) if total_size_header and total_size_header.isdigit() else None
        if total_size is None:
            log("    download size: unknown")
        else:
            log(f"    download size: {format_size(total_size)}")

        bytes_written = 0
        last_reported = 0
        last_report_time = time.monotonic()
        while True:
            chunk = response.read(1024 * 1024)
            if not chunk:
                break
            output.write(chunk)
            bytes_written += len(chunk)
            now = time.monotonic()
            should_report = False
            if total_size is None:
                should_report = bytes_written - last_reported >= 10 * 1024 * 1024 or now - last_report_time >= 5
            else:
                should_report = bytes_written - last_reported >= 10 * 1024 * 1024 or now - last_report_time >= 5 or bytes_written == total_size

            if should_report:
                if total_size:
                    percent = bytes_written / total_size * 100
                    log(f"    downloading: {format_size(bytes_written)} / {format_size(total_size)} ({percent:.1f}%)")
                else:
                    log(f"    downloading: {format_size(bytes_written)}")
                last_reported = bytes_written
                last_report_time = now

        log(f"    downloaded : {format_size(bytes_written)}")


def locate_java_home(root: Path) -> Path:
    candidates: list[Path] = []
    for pattern in ("java", "java.exe"):
        for binary in root.rglob(pattern):
            if binary.parent.name != "bin":
                continue
            candidates.append(binary.parent.parent)

    if not candidates:
        raise RuntimeError(f"Could not locate a Java home under {root}")

    return sorted(candidates, key=lambda item: (len(item.relative_to(root).parts), str(item)))[0]


def write_metadata(destination: Path, spec: TargetSpec, version: str, source_url: str) -> None:
    metadata = {
        "target": spec.key,
        "platform": spec.platform_dir,
        "arch": spec.arch_dir,
        "jdkVersion": version,
        "vendor": "Eclipse Temurin",
        "source": source_url,
    }
    (destination / ".wepush-jdk.json").write_text(
        json.dumps(metadata, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


def install_from_existing_java_home(install_home: Path, java_home: Path) -> None:
    if not has_java_binary(java_home):
        raise RuntimeError(f"Provided java home is missing java executable: {java_home}")

    install_home.parent.mkdir(parents=True, exist_ok=True)
    if install_home.exists():
        shutil.rmtree(install_home)
    shutil.copytree(java_home, install_home, symlinks=True)


def prepare_target(
    project_root: Path,
    version: str,
    spec: TargetSpec,
    force: bool,
    resolve_only: bool,
    java_home_override: Path | None,
) -> Path:
    source_url = build_download_url(version, spec)
    downloads_dir = project_root / "downloads" / "jdks"
    archive_path = downloads_dir / f"temurin-{version}-{spec.key}.{spec.archive_suffix}"
    install_home = project_root / "jdks" / spec.platform_dir / spec.arch_dir / "home"
    log(f"==> {spec.key}")
    log(f"    source: {source_url}")
    log(f"    cache : {archive_path}")
    log(f"    home  : {install_home}")

    if resolve_only:
        return install_home

    if force:
        if archive_path.exists():
            archive_path.unlink()
        if install_home.exists():
            shutil.rmtree(install_home)

    if has_java_binary(install_home):
        log(f"    reuse : {install_home}")
        return install_home

    if java_home_override is not None:
        log(f"    reuse JAVA_HOME -> {java_home_override}")
        install_from_existing_java_home(install_home, java_home_override)
        write_metadata(install_home, spec, version, f"java-home:{java_home_override}")
        log(f"    ready : {install_home}")
        return install_home

    if not archive_path.exists():
        log(f"    download -> {archive_path.name}")
        download_file(source_url, archive_path)
    else:
        log(f"    archive reuse -> {archive_path.name}")

    with tempfile.TemporaryDirectory(prefix=f"wepush-{spec.key}-") as tmp_dir:
        extract_root = Path(tmp_dir) / "extract"
        extract_root.mkdir(parents=True, exist_ok=True)
        log(f"    extract -> {archive_path.name}")
        if spec.archive_type == "zip":
            safe_extract_zip(archive_path, extract_root)
        else:
            safe_extract_tar(archive_path, extract_root)

        discovered_home = locate_java_home(extract_root)
        staged_home = Path(tmp_dir) / "home"
        log(f"    stage  -> {discovered_home}")
        shutil.move(str(discovered_home), staged_home)

        install_home.parent.mkdir(parents=True, exist_ok=True)
        if install_home.exists():
            shutil.rmtree(install_home)
        shutil.move(str(staged_home), install_home)

    if not has_java_binary(install_home):
        raise RuntimeError(f"Prepared JDK is missing java executable: {install_home}")

    write_metadata(install_home, spec, version, source_url)
    log(f"    ready : {install_home}")
    return install_home


def main(argv: Iterable[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--targets",
        required=True,
        help="Comma-separated targets to prepare. Supported: mac-x64, mac-arm64, windows-x64, linux-x64, all",
    )
    parser.add_argument("--version", default="21", help="Temurin feature version to download. Default: 21")
    parser.add_argument(
        "--project-root",
        default=Path(__file__).resolve().parents[1],
        type=Path,
        help="Repository root. Defaults to the current WePush repository root.",
    )
    parser.add_argument(
        "--java-home",
        type=Path,
        help="Reuse an existing Java home instead of downloading a JDK archive.",
    )
    parser.add_argument("--force", action="store_true", help="Delete cached archive and installed JDK before preparing.")
    parser.add_argument("--resolve-only", action="store_true", help="Print the resolved plan without downloading anything.")
    args = parser.parse_args(list(argv) if argv is not None else None)

    try:
        targets = parse_targets(args.targets)
    except ValueError as exc:
        parser.error(str(exc))
        return 2

    project_root = args.project_root.resolve()
    java_home_override = args.java_home.resolve() if args.java_home else None
    for spec in targets:
        prepare_target(project_root, str(args.version), spec, args.force, args.resolve_only, java_home_override)

    return 0


if __name__ == "__main__":
    sys.exit(main())



