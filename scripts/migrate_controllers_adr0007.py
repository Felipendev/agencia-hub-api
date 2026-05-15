#!/usr/bin/env python3
"""Migrate controllers and OpenAPI doc interfaces per ADR 0007."""
from __future__ import annotations

import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "src/main/java/com/agenciahub/api"
TEST = ROOT / "src/test/java/com/agenciahub/api"

OLD_DOC_PKG = "com.agenciahub.api.application.controllers.docs"
NEW_DOC_PKG = "com.agenciahub.api.application.controller.doc"
NEW_CTRL_PKG = "com.agenciahub.api.application.controller"

REPLACEMENTS = [
    (OLD_DOC_PKG, NEW_DOC_PKG),
    (r"com\.agenciahub\.api\.controller\.[a-z0-9.]+", NEW_CTRL_PKG),
    (r"package com\.agenciahub\.api\.controller\.[a-z0-9.]+;", f"package {NEW_CTRL_PKG};"),
]


def flatten_controller(src: Path, dest_dir: Path) -> None:
    dest_dir.mkdir(parents=True, exist_ok=True)
    for f in src.rglob("*Controller.java"):
        if f.is_file():
            target = dest_dir / f.name
            shutil.copy2(f, target)
            text = target.read_text(encoding="utf-8")
            # package from path like controller.auth -> wrong; force flat package
            text = re.sub(
                r"package com\.agenciahub\.api\.controller(?:\.[a-z0-9.]+)?;",
                f"package {NEW_CTRL_PKG};",
                text,
            )
            text = text.replace(OLD_DOC_PKG, NEW_DOC_PKG)
            target.write_text(text, encoding="utf-8")


def migrate_doc_interfaces() -> None:
    old_doc = MAIN / "application/controllers/docs"
    new_doc = MAIN / "application/controller/doc"
    new_doc.mkdir(parents=True, exist_ok=True)
    for f in old_doc.glob("*.java"):
        target = new_doc / f.name
        text = f.read_text(encoding="utf-8")
        text = text.replace(f"package {OLD_DOC_PKG};", f"package {NEW_DOC_PKG};")
        text = text.replace(
            "Os {@code @RestController} em {@code com.agenciahub.api.controller.*} implementam estas interfaces.",
            f"Os {{@code @RestController}} em {{{NEW_CTRL_PKG}}} implementam estas interfaces.",
        )
        target.write_text(text, encoding="utf-8")


def flatten_controller_tests(src: Path, dest_dir: Path) -> None:
    dest_dir.mkdir(parents=True, exist_ok=True)
    for f in src.rglob("*.java"):
        if f.is_file():
            target = dest_dir / f.name
            shutil.copy2(f, target)
            text = target.read_text(encoding="utf-8")
            text = re.sub(
                r"package com\.agenciahub\.api\.controller(?:\.[a-z0-9.]+)?;",
                f"package {NEW_CTRL_PKG};",
                text,
            )
            text = text.replace(OLD_DOC_PKG, NEW_DOC_PKG)
            target.write_text(text, encoding="utf-8")


def replace_in_tree(tree: Path, extensions: tuple[str, ...]) -> None:
    for path in tree.rglob("*"):
        if not path.is_file() or path.suffix not in extensions:
            continue
        if "migrate_controllers_adr0007.py" in str(path):
            continue
        text = path.read_text(encoding="utf-8")
        original = text
        text = text.replace(OLD_DOC_PKG, NEW_DOC_PKG)
        # controller imports: any subpackage -> flat
        text = re.sub(
            r"import com\.agenciahub\.api\.controller\.[a-z0-9.]+\.(\w+Controller);",
            rf"import {NEW_CTRL_PKG}.\1;",
            text,
        )
        text = re.sub(
            r"com\.agenciahub\.api\.controller\.[a-z0-9.]+\.(\w+Controller)",
            rf"{NEW_CTRL_PKG}.\1",
            text,
        )
        if text != original:
            path.write_text(text, encoding="utf-8")


def remove_old_dirs() -> None:
    old_ctrl = MAIN / "controller"
    old_doc = MAIN / "application/controllers"
    old_test_ctrl = TEST / "controller"
    if old_ctrl.exists():
        shutil.rmtree(old_ctrl)
    if old_doc.exists():
        shutil.rmtree(old_doc)
    if old_test_ctrl.exists():
        shutil.rmtree(old_test_ctrl)


def main() -> None:
    dest_main = MAIN / "application/controller"
    dest_test = TEST / "application/controller"

    migrate_doc_interfaces()
    flatten_controller(MAIN / "controller", dest_main)
    flatten_controller_tests(TEST / "controller", dest_test)

    replace_in_tree(ROOT / "src", (".java",))
    replace_in_tree(ROOT / "docs", (".md",))
    replace_in_tree(ROOT / ".cursor", (".md",))

    remove_old_dirs()
    print("ADR 0007 migration done.")


if __name__ == "__main__":
    main()
