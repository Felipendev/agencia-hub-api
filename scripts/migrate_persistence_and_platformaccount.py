#!/usr/bin/env python3
"""Move entity/repository to application.persistence.*; rename usecases.user → platformaccount."""
from __future__ import annotations

import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/agenciahub/api"
TEST = ROOT / "src/test/java/com/agenciahub/api"

REPLACEMENTS = [
    ("com.agenciahub.api.entity.", "com.agenciahub.api.application.persistence.entity."),
    ("com.agenciahub.api.repository.", "com.agenciahub.api.application.persistence.repository."),
    ("application.usecases.user.", "application.usecases.platformaccount."),
    ("usecases.user.", "usecases.platformaccount."),
    ("UserSummaryResponseDTO", "PlatformAccountSummaryResponseDTO"),
    ("UserResponseMapper", "PlatformAccountResponseMapper"),
    ("CreateUserRequestDTO", "CreatePlatformAccountRequestDTO"),
    ("CreateUserUseCase", "CreatePlatformAccountUseCase"),
    ("CreateUser", "CreatePlatformAccount"),
    ("UpdateUserRequestDTO", "UpdatePlatformAccountRequestDTO"),
    ("UpdateUserCommand", "UpdatePlatformAccountCommand"),
    ("UpdateUserUseCase", "UpdatePlatformAccountUseCase"),
    ("UpdateUser", "UpdatePlatformAccount"),
    ("GetUserByIdUseCase", "GetPlatformAccountByIdUseCase"),
    ("GetUserById", "GetPlatformAccountById"),
    ("GetUserEntityByIdUseCase", "GetPlatformAccountEntityByIdUseCase"),
    ("GetUserEntityById", "GetPlatformAccountEntityById"),
    ("ListUsersUseCase", "ListPlatformAccountsUseCase"),
    ("ListUsers", "ListPlatformAccounts"),
    ("SalesAgentDashboardResponseDTO(\n                seller,", "SalesAgentDashboardResponseDTO(\n                salesAgent,"),
    (".seller(", ".salesAgent("),
    ("$.seller.", "$.salesAgent."),
    ('UserSummaryResponseDTO seller', 'PlatformAccountSummaryResponseDTO salesAgent'),
    ('PlatformAccount sellerEntity', 'PlatformAccount salesAgentEntity'),
    ('sellerEntity.getId()', 'salesAgentEntity.getId()'),
    ('sellerEntity,', 'salesAgentEntity,'),
    ('(PlatformAccount seller,', '(PlatformAccount salesAgent,'),
    ('seller.getCommissionPct', 'salesAgent.getCommissionPct'),
    ('seller.getCommissionFixed', 'salesAgent.getCommissionFixed'),
    ('/users/sellers', '/users/sales-agents'),
    ('listActiveSalesAgents', 'listActiveSalesAgents'),  # no-op anchor
]

# Avoid over-replacing CreateUser in wrong contexts - order matters; CreateUser after longer names


def move_tree(src: Path, dst: Path, pkg_from: str, pkg_to: str) -> None:
    if not src.exists():
        return
    dst.parent.mkdir(parents=True, exist_ok=True)
    if dst.exists():
        shutil.rmtree(dst)
    shutil.copytree(src, dst)
    shutil.rmtree(src)
    for java in dst.rglob("*.java"):
        text = java.read_text(encoding="utf-8")
        text = text.replace(f"package {pkg_from}", f"package {pkg_to}")
        java.write_text(text, encoding="utf-8")


def apply_replacements(text: str) -> str:
    # Protect CreatePlatformAccount from becoming CreatePlatformAccountformAccount if we had chained issues
    for old, new in REPLACEMENTS:
        if old == new:
            continue
        text = text.replace(old, new)
    return text


def process_java_files(base: Path) -> None:
    for java in base.rglob("*.java"):
        if "target" in java.parts:
            continue
        text = java.read_text(encoding="utf-8")
        updated = apply_replacements(text)
        if updated != text:
            java.write_text(updated, encoding="utf-8")


def main() -> None:
    entity_src = SRC / "entity"
    entity_dst = SRC / "application/persistence/entity"
    repo_src = SRC / "repository"
    repo_dst = SRC / "application/persistence/repository"

    move_tree(entity_src, entity_dst, "com.agenciahub.api.entity", "com.agenciahub.api.application.persistence.entity")
    move_tree(repo_src, repo_dst, "com.agenciahub.api.repository", "com.agenciahub.api.application.persistence.repository")

    user_src = SRC / "application/usecases/user"
    user_dst = SRC / "application/usecases/platformaccount"
    move_tree(
        user_src,
        user_dst,
        "com.agenciahub.api.application.usecases.user",
        "com.agenciahub.api.application.usecases.platformaccount",
    )

    # Rename test package createuser → createplatformaccount
    test_user = TEST / "application/usecases/user"
    if test_user.exists():
        test_dst = TEST / "application/usecases/platformaccount"
        move_tree(
            test_user,
            test_dst,
            "com.agenciahub.api.application.usecases.user",
            "com.agenciahub.api.application.usecases.platformaccount",
        )
        createuser = test_dst / "createuser"
        if createuser.exists():
            createplatform = test_dst / "createplatformaccount"
            createuser.rename(createplatform)
            for java in createplatform.rglob("*.java"):
                t = java.read_text(encoding="utf-8")
                t = t.replace("createuser", "createplatformaccount")
                java.write_text(t, encoding="utf-8")
            if (createplatform / "CreateUserTest.java").exists():
                (createplatform / "CreateUserTest.java").rename(createplatform / "CreatePlatformAccountTest.java")

    process_java_files(ROOT / "src")
    process_java_files(ROOT / "docs")

    # SalesAgentDashboardResponseDTO record field
    dto = SRC / "application/usecases/salesagent/dashboard/build/SalesAgentDashboardResponseDTO.java"
    if dto.exists():
        t = dto.read_text(encoding="utf-8")
        t = t.replace("UserSummaryResponseDTO seller", "PlatformAccountSummaryResponseDTO salesAgent")
        dto.write_text(t, encoding="utf-8")

    # UserAPI method name
    api = SRC / "application/controller/doc/UserAPI.java"
    if api.exists():
        t = api.read_text(encoding="utf-8")
        t = t.replace("List<PlatformAccountSummaryResponseDTO> sellers()", "List<PlatformAccountSummaryResponseDTO> listSalesAgents()")
        t = t.replace('summary = "Lista vendedores ativos"', 'summary = "Lista agentes de venda ativos"')
        api.write_text(t, encoding="utf-8")

    ctrl = SRC / "application/controller/UserController.java"
    if ctrl.exists():
        t = ctrl.read_text(encoding="utf-8")
        t = t.replace("public List<PlatformAccountSummaryResponseDTO> sellers()", "public List<PlatformAccountSummaryResponseDTO> listSalesAgents()")
        ctrl.write_text(t, encoding="utf-8")

    print("Done. Run mvn test and fix any over-replacements manually.")


if __name__ == "__main__":
    main()
