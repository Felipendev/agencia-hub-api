#!/usr/bin/env python3
"""Add cross-subpackage imports after Fase 2 move (same-feature types in parent package)."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "src/main/java"

# feature -> { SimpleName -> full import }
SHARED: dict[str, dict[str, str]] = {
    "financial": {
        "FinancialEntryResponseMapper": "com.agenciahub.api.application.usecases.financial.FinancialEntryResponseMapper",
    },
    "quotation": {
        "QuotationResponseMapper": "com.agenciahub.api.application.usecases.quotation.QuotationResponseMapper",
        "QuotationSupport": "com.agenciahub.api.application.usecases.quotation.QuotationSupport",
    },
    "customer": {
        "CustomerResponseMapper": "com.agenciahub.api.application.usecases.customer.CustomerResponseMapper",
        "CustomerPhoneNormalizer": "com.agenciahub.api.application.usecases.customer.CustomerPhoneNormalizer",
    },
    "agency": {
        "AgencyResponseMapper": "com.agenciahub.api.application.usecases.agency.AgencyResponseMapper",
    },
    "user": {
        "UserResponseMapper": "com.agenciahub.api.application.usecases.user.UserResponseMapper",
    },
    "solicitacao": {
        "SolicitacaoConfigSupport": "com.agenciahub.api.application.usecases.solicitacao.SolicitacaoConfigSupport",
        "SolicitacaoSubmissionResponseMapper": "com.agenciahub.api.application.usecases.solicitacao.SolicitacaoSubmissionResponseMapper",
    },
    "invitation": {
        "InvitationLinkBuilder": "com.agenciahub.api.application.usecases.invitation.InvitationLinkBuilder",
        "InvitationResponseMapper": "com.agenciahub.api.application.usecases.invitation.InvitationResponseMapper",
        "InvitationTokenPolicy": "com.agenciahub.api.application.usecases.invitation.InvitationTokenPolicy",
    },
}


def feature_from_path(path: Path) -> str | None:
    parts = path.parts
    try:
        i = parts.index("usecases")
        return parts[i + 1]
    except (ValueError, IndexError):
        return None


def ensure_import_after_package(text: str, imp: str) -> str:
    if f"import {imp}" in text:
        return text
    return re.sub(
        r"(package\s+[^;]+;\s*\n)",
        r"\1import " + imp + ";\n",
        text,
        count=1,
    )


def main() -> None:
    base = MAIN / "com/agenciahub/api/application/usecases"
    if not base.exists():
        return
    for path in base.rglob("*.java"):
        feat = feature_from_path(path)
        if not feat or feat not in SHARED:
            continue
        # files already in feature root (no action subdir) are siblings of mappers — skip only if path depth is minimal
        rel = path.relative_to(base / feat)
        if len(rel.parts) == 1:
            continue  # e.g. financial/FinancialEntryResponseMapper.java — no self-import needed
        text = path.read_text(encoding="utf-8")
        orig = text
        for name, imp in SHARED[feat].items():
            if name == path.stem:
                continue
            if re.search(rf"\b{name}\b", text):
                text = ensure_import_after_package(text, imp)
        if text != orig:
            path.write_text(text, encoding="utf-8")
            print("fixed", path.relative_to(ROOT))

    # Auth use cases need InvitationTokenPolicy from invitation feature
    auth_base = base / "auth"
    itp = "com.agenciahub.api.application.usecases.invitation.InvitationTokenPolicy"
    for path in (auth_base / "registerviainvite").glob("*.java"):
        t = path.read_text(encoding="utf-8")
        if "InvitationTokenPolicy" in t and f"import {itp}" not in t:
            path.write_text(ensure_import_after_package(t, itp), encoding="utf-8")
            print("auth fix", path)
    for path in (auth_base / "validatetoken").glob("*.java"):
        t = path.read_text(encoding="utf-8")
        if "InvitationTokenPolicy" in t and f"import {itp}" not in t:
            path.write_text(ensure_import_after_package(t, itp), encoding="utf-8")
            print("auth fix", path)

    # Seller dashboard: quotation + user imports
    sd = base / "sellerdashboard" / "buildsellerdashboard"
    for path in sd.glob("*.java"):
        t = path.read_text(encoding="utf-8")
        for imp in (
            "com.agenciahub.api.application.usecases.user.UserResponseMapper",
            "com.agenciahub.api.application.usecases.quotation.listquotations.ListQuotationsQuery",
            "com.agenciahub.api.application.usecases.quotation.listquotations.ListQuotationsUseCase",
        ):
            if re.search(r"\b" + imp.rsplit(".", 1)[-1] + r"\b", t) and f"import {imp}" not in t:
                t = ensure_import_after_package(t, imp)
        path.write_text(t, encoding="utf-8")


if __name__ == "__main__":
    main()
