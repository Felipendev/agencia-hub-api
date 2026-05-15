#!/usr/bin/env python3
"""Fix bare DTO type names after migrate_dto_feature (controllers + use cases)."""
from __future__ import annotations

import re
from pathlib import Path

REPLACEMENTS = [
    ("RegisterAgencyResponse", "RegisterAgencyResultDTO"),
    ("LoginRequest", "LoginRequestDTO"),
    ("LoginResponse", "LoginResponseDTO"),
    ("RegisterAgencyRequest", "RegisterAgencyRequestDTO"),
    ("RegisterViaInviteRequest", "RegisterViaInviteRequestDTO"),
    ("VerifyEmailRequest", "VerifyEmailRequestDTO"),
    ("VerifyEmailResponse", "VerifyEmailResponseDTO"),
    ("ResendCodeRequest", "ResendCodeRequestDTO"),
    ("ForgotPasswordRequest", "ForgotPasswordRequestDTO"),
    ("ResetPasswordRequest", "ResetPasswordRequestDTO"),
    ("ChangePasswordRequest", "ChangePasswordRequestDTO"),
    ("InviteValidationResponse", "InviteValidationResponseDTO"),
    ("QuotationResponse", "QuotationSummaryResponseDTO"),
    ("CreateQuotationRequest", "CreateQuotationRequestDTO"),
    ("UpdateQuotationRequest", "UpdateQuotationRequestDTO"),
    ("UserResponse", "UserSummaryResponseDTO"),
    ("CreateUserRequest", "CreateUserRequestDTO"),
    ("UpdateUserRequest", "UpdateUserRequestDTO"),
    ("SolicitacaoConfigResponse", "SolicitacaoConfigSummaryResponseDTO"),
    ("SolicitacaoConfigRequest", "SolicitacaoConfigRequestDTO"),
    ("SolicitacaoSubmissionResponse", "SolicitacaoSubmissionSummaryResponseDTO"),
    ("PublicSolicitacaoSubmitRequest", "PublicSolicitacaoSubmitRequestDTO"),
    ("PublicSolicitacaoSubmitResponse", "PublicSolicitacaoSubmitResponseDTO"),
]

SKIP_SUFFIXES = ("DTO", "Mapper", "UseCase", "Command", "Support", "Policy", "Builder")


def replace_in_text(text: str) -> str:
    for old, new in sorted(REPLACEMENTS, key=lambda x: -len(x[0])):
        if old == new:
            continue

        def repl(m: re.Match) -> str:
            start = m.start()
            if start > 0 and text[start - 1] == ".":
                return m.group(0)
            end = m.end()
            if end < len(text):
                nxt = text[end : end + 3]
                if nxt == "DTO" or text[end : end + 6] == "Mapper":
                    return m.group(0)
            return new

        text = re.sub(rf"\b{re.escape(old)}\b", repl, text)
    # dedupe accidental DTODTO
    text = text.replace("RequestDTODTO", "RequestDTO")
    text = text.replace("ResponseDTODTO", "ResponseDTO")
    text = text.replace("ResultDTODTO", "ResultDTO")
    return text


def main() -> None:
    root = Path(__file__).resolve().parents[1] / "src"
    for path in root.rglob("*.java"):
        original = path.read_text(encoding="utf-8")
        updated = replace_in_text(original)
        if updated != original:
            path.write_text(updated, encoding="utf-8")
    print("Fixed DTO type names.")


if __name__ == "__main__":
    main()
