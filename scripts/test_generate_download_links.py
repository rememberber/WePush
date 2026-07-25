from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from scripts.generate_download_links import build_links, find_installer, release_asset_url, version_from_tag


class GenerateDownloadLinksTests(unittest.TestCase):
    def test_version_from_tag(self) -> None:
        self.assertEqual(version_from_tag("v1.7.0"), "1.7.0")
        self.assertEqual(version_from_tag("1.7.0"), "1.7.0")

    def test_release_asset_url(self) -> None:
        actual = release_asset_url("rememberber/WePush", "v1.7.0", "WePush-1.7.0-windows-x64.exe")
        self.assertEqual(
            actual,
            "https://github.com/rememberber/WePush/releases/download/v1.7.0/WePush-1.7.0-windows-x64.exe",
        )

    def test_find_installer_prefers_exact_name(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            (root / "WePush-1.7.0-windows-x64.exe").write_text("exe", encoding="utf-8")
            (root / "WePush-1.7.0-windows-x64.zip").write_text("zip", encoding="utf-8")
            actual = find_installer([root / "WePush-1.7.0-windows-x64.exe", root / "WePush-1.7.0-windows-x64.zip"], "1.7.0", "windows-x64", (".exe", ".zip"))
            self.assertEqual(actual.name, "WePush-1.7.0-windows-x64.exe")

    def test_build_links_for_tag_release_assets(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            (root / "WePush-1.7.0-mac-apple-silicon.dmg").write_text("dmg", encoding="utf-8")
            (root / "WePush-1.7.0-windows-x64.exe").write_text("exe", encoding="utf-8")
            (root / "WePush-1.7.0-linux-x64.deb").write_text("deb", encoding="utf-8")

            links = build_links(root, "v1.7.0")
            self.assertEqual(
                links,
                {
                    "windows": "https://github.com/rememberber/WePush/releases/download/v1.7.0/WePush-1.7.0-windows-x64.exe",
                    "macSilicon": "https://github.com/rememberber/WePush/releases/download/v1.7.0/WePush-1.7.0-mac-apple-silicon.dmg",
                    "linux": "https://github.com/rememberber/WePush/releases/download/v1.7.0/WePush-1.7.0-linux-x64.deb",
                },
            )

    def test_build_links_merge_keeps_existing_mac_when_only_intel_rebuilt(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            (root / "WePush-1.7.0-mac-intel.dmg").write_text("dmg", encoding="utf-8")
            existing = {
                "windows": "https://github.com/rememberber/WePush/releases/download/v1.7.0/WePush-1.7.0-windows-x64.exe",
                "macSilicon": "https://github.com/rememberber/WePush/releases/download/v1.7.0/WePush-1.7.0-mac-apple-silicon.dmg",
            }

            links = build_links(root, "v1.7.0", existing_links=existing)
            self.assertEqual(
                links["mac"],
                "https://github.com/rememberber/WePush/releases/download/v1.7.0/WePush-1.7.0-mac-intel.dmg",
            )
            self.assertEqual(links["windows"], existing["windows"])
            self.assertEqual(links["macSilicon"], existing["macSilicon"])


if __name__ == "__main__":
    unittest.main()
