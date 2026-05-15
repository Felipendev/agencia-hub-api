#!/usr/bin/env python3
"""Migrate one feature from dto/<feature> to usecases colocation (ADR 0008)."""
from __future__ import annotations

import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "src/main/java/com/agenciahub/api"

# feature -> config
FEATURES = {
    "agency": {
        "folders": {"getagency": "retrieve", "updateagency": "update"},
        "dto": {
            "AgencyResponse": ("shared", "AgencySummaryResponseDTO"),
            "UpdateAgencyRequest": ("update", "UpdateAgencyRequestDTO"),
        },
        "root_components": ["AgencyResponseMapper"],
    },
    "terms": {
        "folders": {
            "acceptterms": "accept",
            "getlatesttermspublic": "retrieve/latest",
        },
        "dto": {
            "AcceptTermsRequest": ("accept", "AcceptTermsRequestDTO"),
        },
        "root_components": ["TermsConstants"],
    },
    "financial": {
        "folders": {
            "createfinancialentry": "create",
            "getfinancialentrybyid": "retrieve/byid",
            "listfinancialentries": "retrieve/list",
            "updatefinancialentry": "update",
        },
        "dto": {
            "FinancialEntryResponse": ("shared", "FinancialEntrySummaryResponseDTO"),
            "CreateFinancialEntryRequest": ("create", "CreateFinancialEntryRequestDTO"),
            "UpdateFinancialEntryRequest": ("update", "UpdateFinancialEntryRequestDTO"),
        },
        "root_components": ["FinancialEntryResponseMapper"],
    },
    "invitation": {
        "folders": {
            "createinvitation": "create",
            "listinvitations": "retrieve/list",
            "revokeinvitation": "revoke",
        },
        "dto": {
            "InvitationResponse": ("shared", "InvitationSummaryResponseDTO"),
            "CreateInvitationRequest": ("create", "CreateInvitationRequestDTO"),
        },
        "root_components": [
            "InvitationResponseMapper",
            "InvitationLinkBuilder",
            "InvitationTokenPolicy",
        ],
    },
}


def pkg(feature: str, sub: str) -> str:
    return f"com.agenciahub.api.application.usecases.{feature}" + (f".{sub}" if sub else "")


def migrate_feature(feature: str) -> None:
    cfg = FEATURES[feature]
    uc = MAIN / "application/usecases" / feature
    dto_dir = MAIN / "dto" / feature

    # DTOs from dto/<feature>
    for old_name, (sub, new_name) in cfg["dto"].items():
        src = dto_dir / f"{old_name}.java"
        if not src.exists():
            raise FileNotFoundError(src)
        dest_dir = uc / sub.replace(".", "/")
        dest_dir.mkdir(parents=True, exist_ok=True)
        text = src.read_text(encoding="utf-8")
        text = re.sub(
            rf"package com\.agenciahub\.api\.dto\.{feature};",
            f"package {pkg(feature, sub)};",
            text,
        )
        text = text.replace(f"public record {old_name}(", f"public record {new_name}(")
        (dest_dir / f"{new_name}.java").write_text(text, encoding="utf-8")

    # shared components at feature root
    for comp in cfg["root_components"]:
        src = uc / f"{comp}.java"
        if not src.exists():
            continue
        dest_dir = uc / "shared"
        dest_dir.mkdir(parents=True, exist_ok=True)
        text = src.read_text(encoding="utf-8")
        text = text.replace(f"package {pkg(feature, '')};", f"package {pkg(feature, 'shared')};")
        for old_name, (sub, new_name) in cfg["dto"].items():
            if old_name.endswith("Response"):
                text = text.replace(
                    f"import com.agenciahub.api.dto.{feature}.{old_name};",
                    f"import {pkg(feature, sub)}.{new_name};",
                )
                text = text.replace(f"{old_name} ", f"{new_name} ")
                text = text.replace(f"new {old_name}(", f"new {new_name}(")
        (dest_dir / f"{comp}.java").write_text(text, encoding="utf-8")
        src.unlink()

    # use case folders
    for old_folder, new_rel in cfg["folders"].items():
        src_dir = uc / old_folder
        if not src_dir.exists():
            continue
        dest_dir = uc / new_rel.replace(".", "/")
        dest_dir.mkdir(parents=True, exist_ok=True)
        new_pkg = pkg(feature, new_rel.replace("/", "."))
        for java_file in src_dir.glob("*.java"):
            text = java_file.read_text(encoding="utf-8")
            text = re.sub(
                rf"package {re.escape(pkg(feature, old_folder))};",
                f"package {new_pkg};",
                text,
            )
            text = re.sub(
                rf"package com\.agenciahub\.api\.application\.usecases\.{feature}\.{re.escape(old_folder)};",
                f"package {new_pkg};",
                text,
            )
            for old_name, (sub, new_name) in cfg["dto"].items():
                fqn_old = f"com.agenciahub.api.dto.{feature}.{old_name}"
                fqn_new = f"{pkg(feature, sub)}.{new_name}"
                text = text.replace(f"import {fqn_old};", f"import {fqn_new};")
                if old_name.endswith("Response"):
                    text = re.sub(rf"\b{old_name}\b(?!Mapper)", new_name, text)
                else:
                    text = re.sub(rf"\b{old_name}\b", new_name, text)
            # mappers em shared (sem duplicar .shared.shared)
            text = re.sub(
                rf"import {re.escape(pkg(feature, ''))}\.(\w+Mapper);",
                rf"import {pkg(feature, 'shared')}.\1;",
                text,
            )
            old_uc_pkg = f"com.agenciahub.api.application.usecases.{feature}.{old_folder}"
            for other_old, other_new in cfg["folders"].items():
                if other_old == old_folder:
                    continue
                text = text.replace(
                    f"import com.agenciahub.api.application.usecases.{feature}.{other_old}.",
                    f"import {pkg(feature, other_new.replace('/', '.'))}.",
                )
            (dest_dir / java_file.name).write_text(text, encoding="utf-8")
        shutil.rmtree(src_dir)

    if dto_dir.exists():
        shutil.rmtree(dto_dir)

    # global FQN replacements in src
    replacements: list[tuple[str, str]] = []
    for old_name, (sub, new_name) in cfg["dto"].items():
        replacements.append(
            (f"com.agenciahub.api.dto.{feature}.{old_name}", f"{pkg(feature, sub)}.{new_name}")
        )
    for old_folder, new_rel in cfg["folders"].items():
        replacements.append(
            (
                f"com.agenciahub.api.application.usecases.{feature}.{old_folder}",
                pkg(feature, new_rel.replace("/", ".")),
            )
        )
    replacements.append(
        (f"com.agenciahub.api.application.usecases.{feature}.", f"com.agenciahub.api.application.usecases.{feature}.")
    )
    # fix mapper import from feature root to shared
    replacements.append(
        (
            f"com.agenciahub.api.application.usecases.{feature}.InvitationResponseMapper",
            f"{pkg(feature, 'shared')}.InvitationResponseMapper",
        )
    )
    replacements.append(
        (
            f"com.agenciahub.api.application.usecases.{feature}.InvitationLinkBuilder",
            f"{pkg(feature, 'shared')}.InvitationLinkBuilder",
        )
    )
    replacements.append(
        (
            f"com.agenciahub.api.application.usecases.{feature}.InvitationTokenPolicy",
            f"{pkg(feature, 'shared')}.InvitationTokenPolicy",
        )
    )
    replacements.append(
        (
            f"com.agenciahub.api.application.usecases.{feature}.AgencyResponseMapper",
            f"{pkg(feature, 'shared')}.AgencyResponseMapper",
        )
    )
    replacements.append(
        (
            f"com.agenciahub.api.application.usecases.{feature}.FinancialEntryResponseMapper",
            f"{pkg(feature, 'shared')}.FinancialEntryResponseMapper",
        )
    )
    replacements.append(
        (
            f"com.agenciahub.api.application.usecases.{feature}.TermsConstants",
            f"{pkg(feature, 'shared')}.TermsConstants",
        )
    )

    for path in (ROOT / "src").rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        original = text
        for old, new in sorted(replacements, key=lambda x: -len(x[0])):
            if old == new:
                continue
            text = text.replace(old, new)
        if text != original:
            path.write_text(text, encoding="utf-8")

    print(f"Migrated feature: {feature}")


def main() -> None:
    targets = sys.argv[1:] or list(FEATURES.keys())
    for f in targets:
        migrate_feature(f)


if __name__ == "__main__":
    main()
