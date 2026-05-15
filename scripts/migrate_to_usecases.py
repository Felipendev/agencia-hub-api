#!/usr/bin/env python3
"""
One-shot Fase 2: move application/{feature}/* -> application/usecases/{feature}/{action}/...
and rewrite package + imports across src/main/java and src/test/java.
"""
from __future__ import annotations

import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "src/main/java"
TEST = ROOT / "src/test/java"
APP = MAIN / "com/agenciahub/api/application"

# (feature, subfolder or "", [filenames without .java])
# subfolder "" => usecases/{feature}/ (shared within feature)
PLAN: list[tuple[str, str, list[str]]] = [
    ("financial", "", ["FinancialEntryResponseMapper"]),
    ("financial", "createfinancialentry", ["CreateFinancialEntry", "CreateFinancialEntryUseCase"]),
    ("financial", "updatefinancialentry", ["UpdateFinancialEntry", "UpdateFinancialEntryUseCase", "UpdateFinancialEntryCommand"]),
    ("financial", "listfinancialentries", ["ListFinancialEntries", "ListFinancialEntriesUseCase", "ListFinancialEntriesQuery"]),
    ("financial", "getfinancialentrybyid", ["GetFinancialEntryById", "GetFinancialEntryByIdUseCase"]),
    ("agency", "", ["AgencyResponseMapper"]),
    ("agency", "getagency", ["GetAgency", "GetAgencyUseCase"]),
    ("agency", "updateagency", ["UpdateAgency", "UpdateAgencyUseCase", "UpdateAgencyCommand"]),
    ("customer", "", ["CustomerResponseMapper", "CustomerPhoneNormalizer"]),
    ("customer", "createcustomer", ["CreateCustomer", "CreateCustomerUseCase"]),
    ("customer", "updatecustomer", ["UpdateCustomer", "UpdateCustomerUseCase", "UpdateCustomerCommand"]),
    ("customer", "listcustomers", ["ListCustomers", "ListCustomersUseCase", "ListCustomersQuery"]),
    ("customer", "getcustomerbyid", ["GetCustomerById", "GetCustomerByIdUseCase"]),
    ("customer", "lookupcustomer", ["LookupCustomer", "LookupCustomerUseCase", "LookupCustomerQuery"]),
    ("customer", "deletecustomer", ["DeleteCustomer", "DeleteCustomerUseCase"]),
    ("auth", "", ["AuthBetaWhitelist"]),
    ("auth", "login", ["Login", "LoginUseCase"]),
    ("auth", "registeragency", ["RegisterAgency", "RegisterAgencyUseCase"]),
    ("auth", "verifyemail", ["VerifyEmail", "VerifyEmailUseCase"]),
    ("auth", "resendcode", ["ResendCode", "ResendCodeUseCase"]),
    ("auth", "forgotpassword", ["ForgotPassword", "ForgotPasswordUseCase"]),
    ("auth", "resetpassword", ["ResetPassword", "ResetPasswordUseCase"]),
    ("auth", "changepassword", ["ChangePassword", "ChangePasswordUseCase", "ChangePasswordCommand"]),
    ("auth", "registerviainvite", ["RegisterViaInvite", "RegisterViaInviteUseCase"]),
    ("auth", "validatetoken", ["ValidateInviteToken", "ValidateInviteTokenUseCase"]),
    ("invitation", "", ["InvitationResponseMapper", "InvitationLinkBuilder", "InvitationTokenPolicy"]),
    ("invitation", "createinvitation", ["CreateInvitation", "CreateInvitationUseCase", "CreateInvitationCommand"]),
    ("invitation", "listinvitations", ["ListInvitations", "ListInvitationsUseCase"]),
    ("invitation", "revokeinvitation", ["RevokeInvitation", "RevokeInvitationUseCase", "RevokeInvitationCommand"]),
    ("solicitacao", "", ["SolicitacaoConfigSupport", "SolicitacaoSubmissionResponseMapper"]),
    ("solicitacao", "getorcreatesolicitacaoconfigforagency", ["GetOrCreateSolicitacaoConfigForAgency", "GetOrCreateSolicitacaoConfigForAgencyUseCase"]),
    ("solicitacao", "upsertsolicitacaoconfigforagency", ["UpsertSolicitacaoConfigForAgency", "UpsertSolicitacaoConfigForAgencyUseCase", "UpsertSolicitacaoConfigCommand"]),
    ("solicitacao", "getpublicsolicitacaoconfigbyslug", ["GetPublicSolicitacaoConfigBySlug", "GetPublicSolicitacaoConfigBySlugUseCase"]),
    ("solicitacao", "submitpublicsolicitacao", ["SubmitPublicSolicitacao", "SubmitPublicSolicitacaoUseCase"]),
    ("solicitacao", "listsolicitacaosubmissionsforagency", ["ListSolicitacaoSubmissionsForAgency", "ListSolicitacaoSubmissionsForAgencyUseCase"]),
    ("solicitacao", "deletesolicitacaosubmissionforagency", ["DeleteSolicitacaoSubmissionForAgency", "DeleteSolicitacaoSubmissionForAgencyUseCase", "DeleteSolicitacaoSubmissionCommand"]),
    ("quotation", "", ["QuotationSupport", "QuotationResponseMapper"]),
    ("quotation", "createquotation", ["CreateQuotation", "CreateQuotationUseCase", "CreateQuotationCommand"]),
    ("quotation", "updatequotation", ["UpdateQuotation", "UpdateQuotationUseCase", "UpdateQuotationCommand"]),
    ("quotation", "listquotations", ["ListQuotations", "ListQuotationsUseCase", "ListQuotationsQuery"]),
    ("quotation", "getquotationbyid", ["GetQuotationById", "GetQuotationByIdUseCase"]),
    ("quotation", "deletequotation", ["DeleteQuotation", "DeleteQuotationUseCase"]),
    ("user", "", ["UserResponseMapper"]),
    ("user", "createuser", ["CreateUser", "CreateUserUseCase"]),
    ("user", "updateuser", ["UpdateUser", "UpdateUserUseCase", "UpdateUserCommand"]),
    ("user", "listusers", ["ListUsers", "ListUsersUseCase"]),
    ("user", "listactivesellers", ["ListActiveSellers", "ListActiveSellersUseCase"]),
    ("user", "getuserbyid", ["GetUserById", "GetUserByIdUseCase"]),
    ("user", "getuserentitybyid", ["GetUserEntityById", "GetUserEntityByIdUseCase"]),
    ("sellerdashboard", "buildsellerdashboard", ["BuildSellerDashboard", "BuildSellerDashboardUseCase"]),
]


def old_package(feature: str) -> str:
    return f"com.agenciahub.api.application.{feature}"


def new_package(feature: str, sub: str) -> str:
    if sub:
        return f"com.agenciahub.api.application.usecases.{feature}.{sub}"
    return f"com.agenciahub.api.application.usecases.{feature}"


def main() -> None:
    class_to_pkg: dict[str, str] = {}
    moves: list[tuple[Path, Path, str]] = []

    for feature, sub, classes in PLAN:
        np = new_package(feature, sub)
        for c in classes:
            class_to_pkg[c] = np
            old_path = APP / feature / f"{c}.java"
            if not old_path.exists():
                raise SystemExit(f"Missing source: {old_path}")
            rel = Path("com/agenciahub/api/application") / "usecases" / feature
            if sub:
                rel = rel / sub
            dest_dir = MAIN / rel
            dest_dir.mkdir(parents=True, exist_ok=True)
            dest = dest_dir / f"{c}.java"
            moves.append((old_path, dest, np))

    # Apply moves: read, rewrite package line, write dest, delete source
    for old_path, dest, np in moves:
        text = old_path.read_text(encoding="utf-8")
        text = re.sub(r"^package\s+[\w.]+;", f"package {np};", text, count=1, flags=re.MULTILINE)
        dest.write_text(text, encoding="utf-8")
        old_path.unlink()

    # Remove empty old feature dirs
    for feature in {f for f, _, _ in PLAN}:
        d = APP / feature
        if d.exists():
            try:
                d.rmdir()
            except OSError:
                pass  # not empty

    # Global import rewrites in all Java sources
    roots = [MAIN, TEST]
    import_re = re.compile(r"import\s+(com\.agenciahub\.api\.application\.(?:financial|agency|customer|auth|invitation|solicitacao|quotation|user|sellerdashboard)(?:\.[\w]+)?)\.([\w]+);")

    def replace_import(m: re.Match[str]) -> str:
        full = m.group(0)
        cls = m.group(2)
        if cls in class_to_pkg:
            return f"import {class_to_pkg[cls]}.{cls};"
        return full

    for root in roots:
        for path in root.rglob("*.java"):
            t = path.read_text(encoding="utf-8")
            nt = import_re.sub(replace_import, t)
            # Also fix non-import references in code? (rare) — FQN in comments skipped
            if nt != t:
                path.write_text(nt, encoding="utf-8")

    # Fix same-package imports that used short names — already same package after move
    # Fix cross-feature imports inside moved files (already handled if they use FQN imports)

    print("Done. Classes mapped:", len(class_to_pkg))


if __name__ == "__main__":
    main()
