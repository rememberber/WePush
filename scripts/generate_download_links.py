#!/usr/bin/env python3
"""Generate download_links.json pointing at GitHub Release assets."""

from __future__ import annotations

import argparse
import json
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

DEFAULT_REPO = "rememberber/WePush"

PLATFORM_RULES: dict[str, tuple[str, ...]] = {
    "windows": ("windows-x64",),
    "macSilicon": ("mac-apple-silicon",),
    "mac": ("mac-intel",),
    "linux": ("linux-x64",),
}

INSTALLER_SUFFIXES: dict[str, tuple[str, ...]] = {
    "windows": (".exe", ".msi", ".zip"),
    "macSilicon": (".dmg", ".pkg", ".zip"),
    "mac": (".dmg", ".pkg", ".zip"),
    "linux": (".deb", ".rpm", ".tar.gz"),
}


@dataclass(frozen=True)
class AssetMatch:
    platform_key: str
    file_name: str


def normalize_tag(tag: str) -> str:
    tag = tag.strip()
    if not tag:
        raise ValueError("Release tag must not be empty")
    return tag if tag.startswith("v") else f"v{tag}"


def version_from_tag(tag: str) -> str:
    normalized = normalize_tag(tag)
    return normalized[1:] if normalized.startswith("v") else normalized


def release_asset_url(repo: str, tag: str, file_name: str) -> str:
    return f"https://github.com/{repo}/releases/download/{normalize_tag(tag)}/{file_name}"


def list_assets(assets_dir: Path) -> list[Path]:
    if not assets_dir.exists():
        return []
    return sorted(path for path in assets_dir.rglob("*") if path.is_file())


def find_installer(assets: Iterable[Path], version: str, target_label: str, suffixes: tuple[str, ...]) -> Path | None:
    prefix = f"WePush-{version}-{target_label}"
    for suffix in suffixes:
        exact = [asset for asset in assets if asset.name == f"{prefix}{suffix}"]
        if exact:
            return exact[0]

        prefixed = [asset for asset in assets if asset.name.startswith(f"{prefix}-") and asset.name.endswith(suffix)]
        if prefixed:
            return prefixed[0]
    return None


def match_assets(assets_dir: Path, version: str) -> list[AssetMatch]:
    assets = list_assets(assets_dir)
    matches: list[AssetMatch] = []

    for platform_key, target_labels in PLATFORM_RULES.items():
        suffixes = INSTALLER_SUFFIXES[platform_key]
        for target_label in target_labels:
            installer = find_installer(assets, version, target_label, suffixes)
            if installer is not None:
                matches.append(AssetMatch(platform_key=platform_key, file_name=installer.name))
                break

    return matches


def build_links(
    assets_dir: Path,
    tag: str,
    repo: str = DEFAULT_REPO,
    existing_links: dict[str, str] | None = None,
) -> dict[str, str]:
    version = version_from_tag(tag)
    links = dict(existing_links or {})
    for match in match_assets(assets_dir, version):
        links[match.platform_key] = release_asset_url(repo, tag, match.file_name)
    return links


def write_links(output_path: Path, links: dict[str, str]) -> None:
    ordered_keys = [key for key in PLATFORM_RULES if key in links]
    extra_keys = sorted(key for key in links if key not in PLATFORM_RULES)
    ordered_links = {key: links[key] for key in ordered_keys + extra_keys}
    output_path.write_text(json.dumps(ordered_links, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def load_existing_links(path: Path | None) -> dict[str, str]:
    if path is None or not path.exists():
        return {}
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError(f"Expected object in {path}")
    return {str(key): str(value) for key, value in data.items()}


def main(argv: Iterable[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--assets-dir", required=True, type=Path)
    parser.add_argument("--tag", required=True)
    parser.add_argument("--repo", default=DEFAULT_REPO)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--merge-existing", action="store_true")
    args = parser.parse_args(list(argv) if argv is not None else None)

    existing_links = load_existing_links(args.output) if args.merge_existing else {}
    links = build_links(args.assets_dir.resolve(), args.tag, args.repo, existing_links)
    if not links:
        raise SystemExit("No release assets matched the expected CI naming convention")

    write_links(args.output.resolve(), links)
    for key, url in links.items():
        print(f"{key}: {url}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
