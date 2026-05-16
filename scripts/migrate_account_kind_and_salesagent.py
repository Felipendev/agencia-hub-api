#!/usr/bin/env python3
"""AccountKind + salesagent package move + entity renames (ADR 0008 final)."""
from __future__ import annotations

import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "src/main/java/com/agenciahub/api"
TEST = ROOT / "src/test/java/com/agenciahub/api"


def replace_all_java(text: str) -> str:
    pairs = [
        ("com.agenciahub.api.domain.UserRole", "com.agenciahub.api.domain.enums.AccountKind"),
        ("UserRole.OWNER", "AccountKind.AGENCY_OWNER"),
        ("UserRole.SELLER", "AccountKind.SALES_AGENT"),
        ("UserRole", "AccountKind"),
        ("hasRole('OWNER')", "hasRole('AGENCY_OWNER')"),
        ('hasRole("OWNER")', 'hasRole("AGENCY_OWNER")'),
        ("hasRole('SELLER')", "hasRole('SALES_AGENT')"),
        ('hasRole("SELLER")', 'hasRole("SALES_AGENT")'),
        ("papel **OWNER**", "papel **AGENCY_OWNER**"),
        ("role (OWNER, SELLER)", "role (AGENCY_OWNER, SALES_AGENT)"),
        ("role OWNER/SELLER", "role AGENCY_OWNER/SALES_AGENT"),
        ("com.agenciahub.api.entity.User", "com.agenciahub.api.entity.PlatformAccount"),
        ("com.agenciahub.api.repository.UserRepository", "com.agenciahub.api.repository.PlatformAccountRepository"),
        ("com.agenciahub.api.entity.Customer", "com.agenciahub.api.entity.CrmCustomer"),
        ("com.agenciahub.api.repository.CustomerRepository", "com.agenciahub.api.repository.CrmCustomerRepository"),
    ]
    for old, new in pairs:
        text = text.replace(old, new)
    # word-boundary entity types (avoid CrmCustomerRepository -> CrmCrmCustomer...)
    text = re.sub(r"\bUserRepository\b", "PlatformAccountRepository", text)
    text = re.sub(r"\bCustomerRepository\b", "CrmCustomerRepository", text)
    text = re.sub(r"(?<![.\w])User(?![.\wA-Za-z])", "PlatformAccount", text)
    text = re.sub(r"(?<![.\w])Customer(?![.\wA-Za-z])", "CrmCustomer", text)
    # fix over-replacement
    text = text.replace("PlatformAccountKind", "AccountKind")
    text = text.replace("CrmCrmCustomer", "CrmCustomer")
    text = text.replace("PlatformAccountSummaryResponseDTO", "UserSummaryResponseDTO")
    text = text.replace("CreatePlatformAccount", "CreateUser")
    text = text.replace("UpdatePlatformAccount", "UpdateUser")
    text = text.replace("GetPlatformAccount", "GetUser")
    text = text.replace("ListPlatformAccount", "ListUser")
    text = text.replace("PlatformAccountController", "UserController")
    text = text.replace("PlatformAccountAPI", "UserAPI")
    text = text.replace("DeleteCrmCustomer", "DeleteCustomer")
    text = text.replace("CreateCrmCustomer", "CreateCustomer")
    text = text.replace("UpdateCrmCustomer", "UpdateCustomer")
    text = text.replace("LookupCrmCustomer", "LookupCustomer")
    text = text.replace("GetCrmCustomer", "GetCustomer")
    text = text.replace("ListCrmCustomer", "ListCustomers")
    text = text.replace("CrmCustomerController", "CustomerController")
    text = text.replace("CrmCustomerAPI", "CustomerAPI")
    text = text.replace("DuplicateCrmCustomerException", "DuplicateCustomerException")
    text = text.replace("CrmCustomerResponseMapper", "CustomerResponseMapper")
    text = text.replace("CrmCustomerPhoneNormalizer", "CustomerPhoneNormalizer")
    text = text.replace("CrmCustomerSummaryResponseDTO", "CustomerSummaryResponseDTO")
    return text


def move_salesagent() -> None:
    uc = MAIN / "application/usecases"
    sa = uc / "salesagent"
    sa.mkdir(parents=True, exist_ok=True)

    moves = [
        (uc / "user/retrieve/listactive", sa / "retrieve/listactive", {
            "ListActiveSellers": "ListActiveSalesAgents",
            "ListActiveSellersUseCase": "ListActiveSalesAgentsUseCase",
        }),
        (uc / "sellerdashboard/buildsellerdashboard", sa / "dashboard/build", {
            "BuildSellerDashboard": "BuildSalesAgentDashboard",
            "BuildSellerDashboardUseCase": "BuildSalesAgentDashboardUseCase",
            "SellerDashboardResponseDTO": "SalesAgentDashboardResponseDTO",
        }),
    ]
    for src_dir, dest_dir, renames in moves:
        if not src_dir.exists():
            continue
        dest_dir.mkdir(parents=True, exist_ok=True)
        for f in src_dir.glob("*.java"):
            text = replace_all_java(f.read_text(encoding="utf-8"))
            text = text.replace(
                f"package com.agenciahub.api.application.usecases.{src_dir.relative_to(uc).as_posix().replace('/', '.')};",
                f"package com.agenciahub.api.application.usecases.{dest_dir.relative_to(uc).as_posix().replace('/', '.')};",
            )
            for old, new in renames.items():
                text = text.replace(old, new)
            name = f.name
            for old, new in renames.items():
                if old in name:
                    name = name.replace(old, new)
            (dest_dir / name).write_text(text, encoding="utf-8")
        shutil.rmtree(src_dir)
    if (uc / "sellerdashboard").exists() and not any((uc / "sellerdashboard").rglob("*.java")):
        shutil.rmtree(uc / "sellerdashboard")
    parent = uc / "user/retrieve/listactive"
    if parent.parent.exists() and not any(parent.parent.rglob("*.java")):
        # only remove listactive folder
        pass


def rename_entity_files() -> None:
    renames = [
        (MAIN / "entity/User.java", MAIN / "entity/PlatformAccount.java"),
        (MAIN / "entity/Customer.java", MAIN / "entity/CrmCustomer.java"),
        (MAIN / "repository/UserRepository.java", MAIN / "repository/PlatformAccountRepository.java"),
        (MAIN / "repository/CustomerRepository.java", MAIN / "repository/CrmCustomerRepository.java"),
    ]
    for src, dst in renames:
        if src.exists():
            text = replace_all_java(src.read_text(encoding="utf-8"))
            dst.write_text(text, encoding="utf-8")
            src.unlink()


def update_controllers_salesagent() -> None:
  # SellerDashboardController imports
    ctrl = MAIN / "application/controller/SellerDashboardController.java"
    if ctrl.exists():
        text = replace_all_java(ctrl.read_text(encoding="utf-8"))
        text = text.replace(
            "usecases.sellerdashboard.buildsellerdashboard",
            "usecases.salesagent.dashboard.build",
        )
        text = text.replace("BuildSellerDashboard", "BuildSalesAgentDashboard")
        text = text.replace("SellerDashboardResponseDTO", "SalesAgentDashboardResponseDTO")
        ctrl.write_text(text, encoding="utf-8")

    api = MAIN / "application/controller/doc/SellerDashboardAPI.java"
    if api.exists():
        text = replace_all_java(api.read_text(encoding="utf-8"))
        text = text.replace("SellerDashboardResponseDTO", "SalesAgentDashboardResponseDTO")
        api.write_text(text, encoding="utf-8")

    user_ctrl = MAIN / "application/controller/UserController.java"
    if user_ctrl.exists():
        text = replace_all_java(user_ctrl.read_text(encoding="utf-8"))
        text = text.replace(
            "usecases.user.retrieve.listactive.ListActiveSellersUseCase",
            "usecases.salesagent.retrieve.listactive.ListActiveSalesAgentsUseCase",
        )
        user_ctrl.write_text(text, encoding="utf-8")


def process_tree(base: Path) -> None:
    for path in base.rglob("*.java"):
        if "migrate_account_kind" in path.name:
            continue
        original = path.read_text(encoding="utf-8")
        updated = replace_all_java(original)
        if updated != original:
            path.write_text(updated, encoding="utf-8")


def delete_user_role() -> None:
    p = MAIN / "domain/UserRole.java"
    if p.exists():
        p.unlink()


def main() -> None:
    move_salesagent()
    rename_entity_files()
    process_tree(MAIN)
    process_tree(TEST)
    update_controllers_salesagent()
    delete_user_role()
    print("AccountKind + salesagent + entity renames done.")


if __name__ == "__main__":
    main()
