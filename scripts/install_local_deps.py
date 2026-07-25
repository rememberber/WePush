#!/usr/bin/env python3
"""Install vendored jars from lib/ into the project-local Maven repository.

These artifacts are not available from public HTTPS repositories. The generated
layout under lib-repo/ is referenced by pom.xml as a file:// repository so
dependency resolution works on fresh CI runners.
"""

from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path
from typing import Iterable, Optional, Sequence, Tuple

Dep = Tuple[str, str, str, str]

DEPS: Sequence[Dep] = (
    ("taobao-sdk-java-auto.jar", "com.taobao", "taobao-sdk-java-auto", "1.0.0"),
    ("cpdetector_1.0.10.jar", "net.sourceforge.cpdetector", "cpdetector", "1.0.10"),
    ("antlr-2.7.4.jar", "net.sourceforge.cpdetector", "antlr", "2.7.4"),
    ("chardet-1.0.jar", "net.sourceforge.cpdetector", "chardet", "1.0.0"),
    ("jargs-1.0.jar", "net.sourceforge.cpdetector", "jargs", "1.0.0"),
)


def install_file(
    project_root: Path,
    local_repo: Path,
    jar_name: str,
    group_id: str,
    artifact_id: str,
    version: str,
) -> None:
    jar_path = project_root / "lib" / jar_name
    if not jar_path.is_file():
        raise FileNotFoundError("Missing vendored jar: {}".format(jar_path))
    cmd = [
        "mvn",
        "--batch-mode",
        "--no-transfer-progress",
        "org.apache.maven.plugins:maven-install-plugin:3.1.3:install-file",
        "-Dfile={}".format(jar_path),
        "-DgroupId={}".format(group_id),
        "-DartifactId={}".format(artifact_id),
        "-Dversion={}".format(version),
        "-Dpackaging=jar",
        "-DgeneratePom=true",
        "-DlocalRepositoryPath={}".format(local_repo),
        "-DcreateChecksum=true",
    ]
    print("Installing {}:{}:{} -> {}".format(group_id, artifact_id, version, local_repo))
    subprocess.run(cmd, cwd=str(project_root), check=True)


def install_all(project_root: Path, deps: Iterable[Dep]) -> Path:
    local_repo = project_root / "lib-repo"
    local_repo.mkdir(parents=True, exist_ok=True)
    for jar_name, group_id, artifact_id, version in deps:
        install_file(project_root, local_repo, jar_name, group_id, artifact_id, version)
    return local_repo


def main(argv: Optional[Sequence[str]] = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--project-root",
        type=Path,
        default=Path(__file__).resolve().parents[1],
    )
    args = parser.parse_args(list(argv) if argv is not None else None)
    project_root = args.project_root.resolve()
    local_repo = install_all(project_root, DEPS)
    print("Project-local Maven repository ready: {}".format(local_repo))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:  # pragma: no cover - CLI surface
        print("error: {}".format(exc), file=sys.stderr)
        raise SystemExit(1)
