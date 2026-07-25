#!/usr/bin/env python3
"""Collect packaged artifacts into a release-friendly directory with normalized names."""

from __future__ import annotations

import argparse
import os
import re
import shutil
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

SUPPORTED_SUFFIXES = (
    ".tar.gz",
    ".tar.xz",
    ".dmg",
    ".pkg",
    ".zip",
    ".deb",
    ".rpm",
    ".msi",
    ".exe",
    ".jar",
)

IGNORED_TOKENS = {
    "mac",
    "macos",
    "osx",
    "windows",
    "win",
    "linux",
    "universal",
    "x64",
    "x86_64",
    "x86",
    "amd64",
    "arm64",
    "aarch64",
}


@dataclass(frozen=True)
class ProjectInfo:
    name: str
    version: str
    artifact_id: str


@dataclass(frozen=True)
class CollectedAsset:
    source: Path
    destination: Path


def parse_project_info(pom_path: Path) -> ProjectInfo:
    root = ET.parse(pom_path).getroot()
    namespace = {"m": "http://maven.apache.org/POM/4.0.0"}

    def text(xpath: str) -> str:
        node = root.find(xpath, namespace)
        if node is None or node.text is None:
            raise RuntimeError(f"Missing required element: {xpath}")
        return node.text.strip()

    return ProjectInfo(
        name=text("m:name"),
        version=text("m:version"),
        artifact_id=text("m:artifactId"),
    )


def detect_suffix(file_name: str) -> str | None:
    lowered = file_name.lower()
    for suffix in SUPPORTED_SUFFIXES:
        if lowered.endswith(suffix):
            return suffix
    return None


def tokenise(value: str) -> list[str]:
    return [token for token in re.split(r"[^A-Za-z0-9]+", value.lower()) if token]


def extra_label(file_name: str, project: ProjectInfo) -> str:
    suffix = detect_suffix(file_name)
    if suffix is None:
        raise RuntimeError(f"Unsupported asset type: {file_name}")

    stem = file_name[: -len(suffix)]
    tokens = tokenise(stem)
    removable = set(tokenise(project.name)) | set(tokenise(project.artifact_id)) | set(tokenise(project.version)) | IGNORED_TOKENS
    leftovers = [token for token in tokens if token not in removable]
    return "-".join(leftovers)


def normalized_name(file_name: str, project: ProjectInfo, target_label: str) -> str:
    suffix = detect_suffix(file_name)
    if suffix is None:
        raise RuntimeError(f"Unsupported asset type: {file_name}")

    extra = extra_label(file_name, project)
    parts = [project.name, project.version, target_label]
    if extra:
        parts.append(extra)
    return f"{'-'.join(parts)}{suffix}"


def supported_assets(source_dir: Path) -> list[Path]:
    files = [path for path in source_dir.iterdir() if path.is_file() and detect_suffix(path.name)]
    return sorted(files)


def collect_assets(project_root: Path, source_dir: Path, output_dir: Path, target_label: str) -> list[CollectedAsset]:
    project = parse_project_info(project_root / "pom.xml")
    assets = supported_assets(source_dir)
    if not assets:
        raise RuntimeError(f"No supported build artifacts found under {source_dir}")

    output_dir.mkdir(parents=True, exist_ok=True)
    collected: list[CollectedAsset] = []
    seen_names: set[str] = set()

    for asset in assets:
        destination_name = normalized_name(asset.name, project, target_label)
        if destination_name in seen_names:
            raise RuntimeError(f"Duplicate normalized asset name generated: {destination_name}")
        seen_names.add(destination_name)
        destination = output_dir / destination_name
        shutil.copy2(asset, destination)
        collected.append(CollectedAsset(source=asset, destination=destination))

    return collected


def append_summary(collected: list[CollectedAsset], target_label: str) -> None:
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_path:
        return

    lines = [f"### {target_label}", "", "| Source | Release asset |", "| --- | --- |"]
    for item in collected:
        lines.append(f"| `{item.source.name}` | `{item.destination.name}` |")
    lines.append("")
    Path(summary_path).open("a", encoding="utf-8").write("\n".join(lines) + "\n")


def main(argv: Iterable[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project-root", default=Path(__file__).resolve().parents[1], type=Path)
    parser.add_argument("--source-dir", required=True, type=Path)
    parser.add_argument("--output-dir", required=True, type=Path)
    parser.add_argument("--target-label", required=True)
    args = parser.parse_args(list(argv) if argv is not None else None)

    project_root = args.project_root.resolve()
    source_dir = args.source_dir.resolve()
    output_dir = args.output_dir.resolve()

    collected = collect_assets(project_root, source_dir, output_dir, args.target_label)
    for item in collected:
        print(f"{item.source} -> {item.destination}")
    append_summary(collected, args.target_label)
    return 0


if __name__ == "__main__":
    sys.exit(main())


