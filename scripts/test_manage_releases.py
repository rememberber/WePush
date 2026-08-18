from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from scripts.manage_releases import app_version_from_pom, java_release_info, release_outputs


class ManageReleasesTests(unittest.TestCase):
    def test_app_version_keeps_underscore_prefix(self) -> None:
        self.assertEqual(app_version_from_pom("5.0.5"), "v_5.0.5")

    def test_java_release_requires_aligned_sources(self) -> None:
        project_root = Path(__file__).resolve().parents[1]
        info = java_release_info(project_root, "v5.0.8")
        self.assertEqual(info.version, "5.0.8")
        self.assertEqual(info.app_version, "v_5.0.8")
        self.assertEqual(info.title, "WePush 5.0.8")
        self.assertFalse(info.prerelease)
        self.assertEqual(release_outputs(info)["make_latest"], "true")
        self.assertIn("极光", info.notes)

    def test_java_release_rejects_wrong_tag(self) -> None:
        project_root = Path(__file__).resolve().parents[1]
        with self.assertRaisesRegex(ValueError, "Java tag must be"):
            java_release_info(project_root, "v_5.0.8")

    def test_java_release_rejects_missing_notes(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            (root / "pom.xml").write_text(
                """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.fangxuele.tool</groupId>
  <artifactId>WePush</artifactId>
  <version>9.9.9</version>
  <name>WePush</name>
</project>
""",
                encoding="utf-8",
            )
            ui = root / "src/main/java/com/fangxuele/tool/push/ui"
            ui.mkdir(parents=True)
            (ui / "UiConsts.java").write_text('public class UiConsts { public static final String APP_VERSION = "v_9.9.9"; }\n', encoding="utf-8")
            resources = root / "src/main/resources"
            resources.mkdir(parents=True)
            (resources / "version_summary.json").write_text(json.dumps({
                "currentVersion": "v_9.9.9",
                "versionIndex": {"v_9.9.9": "1"},
                "versionDetailList": [{"version": "v_9.9.9", "title": "t", "log": ""}],
            }), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "missing log"):
                java_release_info(root, "v9.9.9")


if __name__ == "__main__":
    unittest.main()
