#!/usr/bin/env python3
"""Validate WePush Java releases and render GitHub Release notes."""

from __future__ import annotations

import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

DEFAULT_REPO = "rememberber/WePush"
SEMVER_PATTERN = re.compile(
    r"^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)"
    r"(?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?"
    r"(?:\+([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?$"
)


@dataclass(frozen=True)
class SemVer:
    major: int
    minor: int
    patch: int
    prerelease: tuple[str, ...]


@dataclass(frozen=True)
class ReleaseInfo:
    version: str
    tag: str
    app_version: str
    title: str
    notes: str
    prerelease: bool


def parse_semver(value: str) -> SemVer:
    match = SEMVER_PATTERN.fullmatch(value.strip())
    if match is None:
        raise ValueError(f"Invalid semantic version: {value}")
    prerelease = tuple(match.group(4).split(".")) if match.group(4) else ()
    if any(part.isdigit() and len(part) > 1 and part.startswith("0") for part in prerelease):
        raise ValueError(f"Invalid semantic version: {value}")
    return SemVer(int(match.group(1)), int(match.group(2)), int(match.group(3)), prerelease)


def load_json(path: Path) -> object:
    return json.loads(path.read_text(encoding="utf-8"))


def pom_version(path: Path) -> str:
    root = ET.parse(path).getroot()
    namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
    node = root.find("m:version", namespace)
    if node is None or node.text is None:
        raise ValueError(f"Missing project version in {path}")
    return node.text.strip()


def app_version_from_pom(version: str) -> str:
    """WePush runtime version keeps the historical v_ prefix."""
    return f"v_{version}"


def release_changes(log: str) -> list[str]:
    changes: list[str] = []
    for raw_line in log.splitlines():
        line = raw_line.strip()
        if not line:
            continue
        changes.append(f"- {line[1:].strip()}" if line.startswith("●") else f"- {line}")
    return changes


def java_release_notes(version: str, title: str, log: str) -> str:
    changes = release_changes(log)
    if not changes:
        raise ValueError(f"WePush {version} has no release notes")
    return "\n".join([
        "## 中文",
        "",
        f"> Version: {version}",
        "",
        f"### 更新内容: {title}",
        "",
        *changes,
        "",
    ]).rstrip()


def java_release_info(project_root: Path, tag: str) -> ReleaseInfo:
    version = pom_version(project_root / "pom.xml")
    parsed = parse_semver(version)
    if parsed.prerelease:
        raise ValueError("WePush prereleases are not supported by the stable update feed")
    expected_tag = f"v{version}"
    if tag != expected_tag:
        raise ValueError(f"Java tag must be {expected_tag}, got {tag}")

    app_version = app_version_from_pom(version)
    ui_consts = (project_root / "src/main/java/com/fangxuele/tool/push/ui/UiConsts.java").read_text(encoding="utf-8")
    ui_match = re.search(r'APP_VERSION\s*=\s*"([^"]+)"', ui_consts)
    if ui_match is None or ui_match.group(1) != app_version:
        actual = ui_match.group(1) if ui_match else "missing"
        raise ValueError(f"UiConsts.APP_VERSION must be {app_version}, got {actual}")

    summary_path = project_root / "src/main/resources/version_summary.json"
    summary = load_json(summary_path)
    if not isinstance(summary, dict) or summary.get("currentVersion") != app_version:
        raise ValueError(f"version_summary.currentVersion must be {app_version}")
    version_index = summary.get("versionIndex")
    details = summary.get("versionDetailList")
    if not isinstance(version_index, dict) or app_version not in version_index:
        raise ValueError(f"version_summary.versionIndex is missing {app_version}")
    if not isinstance(details, list):
        raise ValueError("version_summary.versionDetailList must be an array")
    detail = next((item for item in details if isinstance(item, dict) and item.get("version") == app_version), None)
    if detail is None or not isinstance(detail.get("title"), str) or not detail["title"].strip():
        raise ValueError(f"version_summary.versionDetailList is missing title for {app_version}")
    if not isinstance(detail.get("log"), str) or not detail["log"].strip():
        raise ValueError(f"version_summary.versionDetailList is missing log for {app_version}")

    title = f"WePush {version}"
    notes = java_release_notes(version, detail["title"], detail["log"])
    return ReleaseInfo(version, tag, app_version, title, notes, False)


def append_github_output(path: Path | None, values: dict[str, str]) -> None:
    if path is None:
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as output:
        for key, value in values.items():
            if "\n" in value or "\r" in value:
                raise ValueError(f"GitHub output must be single-line: {key}")
            output.write(f"{key}={value}\n")


def write_java_release_body(path: Path, info: ReleaseInfo) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(f"# {info.title}\n\n{info.notes}\n", encoding="utf-8")


def release_outputs(info: ReleaseInfo) -> dict[str, str]:
    return {
        "version": info.version,
        "tag": info.tag,
        "title": info.title,
        "prerelease": str(info.prerelease).lower(),
        "make_latest": "false" if info.prerelease else "true",
    }


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)
    default_root = Path(__file__).resolve().parents[1]

    validate_java = subparsers.add_parser("validate-java")
    validate_java.add_argument("--project-root", type=Path, default=default_root)
    validate_java.add_argument("--tag", required=True)
    validate_java.add_argument("--github-output", type=Path)
    validate_java.add_argument("--body-output", required=True, type=Path)
    return parser


def main(argv: Iterable[str] | None = None) -> int:
    args = build_parser().parse_args(list(argv) if argv is not None else None)
    if args.command == "validate-java":
        info = java_release_info(args.project_root.resolve(), args.tag)
        write_java_release_body(args.body_output.resolve(), info)
        append_github_output(args.github_output, release_outputs(info))
        print(f"Validated {info.tag}: {info.title} (app version {info.app_version})")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (ValueError, OSError, json.JSONDecodeError, ET.ParseError) as error:
        raise SystemExit(str(error)) from error
