from __future__ import annotations

import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from scripts.prepare_jdks import TARGETS, format_size, has_java_version, install_from_existing_java_home, locate_java_home, parse_targets, prepare_target


class PrepareJdksTests(unittest.TestCase):
    def test_java_version_reads_release_without_running_target_binary(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            home = Path(tmp_dir)
            self.assertFalse(has_java_version(home, "25"))
            for version in ("25", "25.0.1", "25.0.4.1", "25+36"):
                (home / "release").write_text(f'JAVA_VERSION="{version}"\n', encoding="utf-8")
                self.assertTrue(has_java_version(home, "25"))
                self.assertFalse(has_java_version(home, "21"))

    def test_replaces_cached_java_21_with_requested_java_25(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            cached_home = root / "jdks" / "mac" / "arm64" / "home"
            source_home = root / "source-home"
            for home, version in ((cached_home, "21.0.9"), (source_home, "25.0.1")):
                (home / "bin").mkdir(parents=True)
                (home / "bin" / "java").write_text("", encoding="utf-8")
                (home / "release").write_text(f'JAVA_VERSION="{version}"\n', encoding="utf-8")
            with patch("scripts.prepare_jdks.download_file") as download:
                result = prepare_target(root, "25", TARGETS["mac-arm64"], False, False, source_home)
                download.assert_not_called()
            self.assertEqual(result, cached_home)
            self.assertTrue(has_java_version(result, "25"))

    def test_rejects_java_home_with_old_version(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            source_home = root / "source-home"
            source_home.mkdir()
            (source_home / "release").write_text('JAVA_VERSION="21.0.9"\n', encoding="utf-8")
            with self.assertRaisesRegex(RuntimeError, "must contain JDK 25"):
                prepare_target(root, "25", TARGETS["mac-arm64"], False, False, source_home)

    def test_format_size_for_bytes(self) -> None:
        self.assertEqual(format_size(512), "512 B")

    def test_format_size_for_mebibytes(self) -> None:
        self.assertEqual(format_size(5 * 1024 * 1024), "5.0 MiB")

    def test_parse_targets_accepts_all(self) -> None:
        targets = parse_targets("all")
        self.assertEqual({target.key for target in targets}, set(TARGETS))

    def test_parse_targets_rejects_unknown_target(self) -> None:
        with self.assertRaises(ValueError):
            parse_targets("mac-x64,unknown")

    def test_locate_java_home_for_standard_layout(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            home = root / "jdk-21"
            (home / "bin").mkdir(parents=True)
            (home / "bin" / "java").write_text("", encoding="utf-8")
            self.assertEqual(locate_java_home(root), home)

    def test_locate_java_home_for_macos_layout(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            home = root / "temurin-21.jdk" / "Contents" / "Home"
            (home / "bin").mkdir(parents=True)
            (home / "bin" / "java").write_text("", encoding="utf-8")
            self.assertEqual(locate_java_home(root), home)

    def test_install_from_existing_java_home(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            source_home = root / "source-home"
            (source_home / "bin").mkdir(parents=True)
            (source_home / "bin" / "java").write_text("", encoding="utf-8")
            destination_home = root / "jdks" / "mac" / "x64" / "home"

            install_from_existing_java_home(destination_home, source_home)

            self.assertTrue((destination_home / "bin" / "java").exists())


if __name__ == "__main__":
    unittest.main()
