from __future__ import annotations

import io
import tarfile
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch

from scripts.prepare_jdks import (
    TARGETS,
    format_size,
    has_java_version,
    has_jmods,
    install_from_existing_java_home,
    install_jmods_from_archive,
    locate_java_home,
    locate_jmods_dir,
    parse_targets,
    prepare_target,
)


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
            (source_home / "jmods").mkdir()
            (source_home / "jmods" / "java.base.jmod").write_bytes(b"jmod")
            with patch("scripts.prepare_jdks.download_file") as download:
                result = prepare_target(root, "25", TARGETS["mac-arm64"], False, False, source_home)
                download.assert_not_called()
            self.assertEqual(result, cached_home)
            self.assertTrue(has_java_version(result, "25"))
            self.assertTrue(has_jmods(result))

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

    def test_locate_jmods_dir_for_nested_and_macos_layouts(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            nested = root / "jdk-25" / "jmods"
            nested.mkdir(parents=True)
            (nested / "java.base.jmod").write_bytes(b"jmod")
            self.assertEqual(locate_jmods_dir(root), nested)

            macos = root / "temurin-25.jdk" / "Contents" / "Home" / "jmods"
            macos.mkdir(parents=True)
            (macos / "java.base.jmod").write_bytes(b"jmod")
            self.assertEqual(locate_jmods_dir(root / "temurin-25.jdk"), macos)

    def test_install_jmods_from_tar_and_zip(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            tar_path = root / "jmods.tar.gz"
            payload = b"jmod"
            with tarfile.open(tar_path, "w:gz") as archive:
                info = tarfile.TarInfo("jdk-25.jdk/Contents/Home/jmods/java.base.jmod")
                info.size = len(payload)
                archive.addfile(info, io.BytesIO(payload))
            tar_home = root / "tar-home"
            tar_home.mkdir()
            install_jmods_from_archive(tar_path, "tar.gz", tar_home)
            self.assertEqual((tar_home / "jmods" / "java.base.jmod").read_bytes(), payload)

            zip_path = root / "jmods.zip"
            with zipfile.ZipFile(zip_path, "w") as archive:
                archive.writestr("jdk-25/jmods/java.desktop.jmod", payload)
            zip_home = root / "zip-home"
            zip_home.mkdir()
            install_jmods_from_archive(zip_path, "zip", zip_home)
            self.assertTrue(has_jmods(zip_home))

    def test_cached_jdk_without_jmods_downloads_jmods_only(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            cached_home = root / "jdks" / "linux" / "x64" / "home"
            (cached_home / "bin").mkdir(parents=True)
            (cached_home / "bin" / "java").write_text("", encoding="utf-8")
            (cached_home / "release").write_text('JAVA_VERSION="25.0.1"\n', encoding="utf-8")

            def write_archive(url: str, destination: Path) -> None:
                del url
                destination.parent.mkdir(parents=True, exist_ok=True)
                payload = b"jmod"
                with tarfile.open(destination, "w:gz") as archive:
                    info = tarfile.TarInfo("jdk-25/jmods/java.base.jmod")
                    info.size = len(payload)
                    archive.addfile(info, io.BytesIO(payload))

            with patch("scripts.prepare_jdks.download_file", side_effect=write_archive) as download:
                result = prepare_target(root, "25", TARGETS["linux-x64"], False, False, None)
            download.assert_called_once()
            self.assertIn("/jmods/", download.call_args.args[0])
            self.assertTrue(has_jmods(result))

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
