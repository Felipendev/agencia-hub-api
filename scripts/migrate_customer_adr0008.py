#!/usr/bin/env python3
"""Pilot ADR 0008: customer use cases + DTO colocation."""
from __future__ import annotations

import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
UC = ROOT / "src/main/java/com/agenciahub/api/application/usecases/customer"
DTO_OLD = ROOT / "src/main/java/com/agenciahub/api/dto/customer"

PKG = "com.agenciahub.api.application.usecases.customer"
SHARED = f"{PKG}.shared"
CREATE = f"{PKG}.create"
UPDATE = f"{PKG}.update"
BYID = f"{PKG}.retrieve.byid"
LIST = f"{PKG}.retrieve.list"
DELETE = f"{PKG}.delete"
LOOKUP = f"{PKG}.lookup"

FOLDER_MAP = {
    "createcustomer": "create",
    "getcustomerbyid": "retrieve/byid",
    "listcustomers": "retrieve/list",
    "updatecustomer": "update",
    "deletecustomer": "delete",
    "lookupcustomer": "lookup",
}

REPLACEMENTS_IN_JAVA = [
    ("com.agenciahub.api.dto.customer.CreateCustomerRequest", f"{CREATE}.CreateCustomerRequestDTO"),
    ("com.agenciahub.api.dto.customer.UpdateCustomerRequest", f"{UPDATE}.UpdateCustomerRequestDTO"),
    ("com.agenciahub.api.dto.customer.CustomerResponse", f"{SHARED}.CustomerSummaryResponseDTO"),
    ("CreateCustomerRequest", "CreateCustomerRequestDTO"),
    ("UpdateCustomerRequest", "UpdateCustomerRequestDTO"),
    ("CustomerResponse", "CustomerSummaryResponseDTO"),
    (f"{PKG}.createcustomer", CREATE),
    (f"{PKG}.getcustomerbyid", BYID),
    (f"{PKG}.listcustomers", LIST),
    (f"{PKG}.updatecustomer", UPDATE),
    (f"{PKG}.deletecustomer", DELETE),
    (f"{PKG}.lookupcustomer", LOOKUP),
    (f"{PKG}.CustomerPhoneNormalizer", f"{SHARED}.CustomerPhoneNormalizer"),
    (f"{PKG}.CustomerResponseMapper", f"{SHARED}.CustomerResponseMapper"),
]


def pkg_for_folder(folder_key: str) -> str:
    sub = FOLDER_MAP[folder_key].replace("/", ".")
    return f"{PKG}.{sub}"


def apply_replacements(text: str) -> str:
    for old, new in REPLACEMENTS_IN_JAVA:
        text = text.replace(old, new)
    return text


def write_shared() -> None:
    shared_dir = UC / "shared"
    shared_dir.mkdir(parents=True, exist_ok=True)

    summary = (DTO_OLD / "CustomerResponse.java").read_text(encoding="utf-8")
    summary = summary.replace("package com.agenciahub.api.dto.customer;", f"package {SHARED};")
    summary = summary.replace("CustomerResponse", "CustomerSummaryResponseDTO")
    (shared_dir / "CustomerSummaryResponseDTO.java").write_text(summary, encoding="utf-8")

    norm = (UC / "CustomerPhoneNormalizer.java").read_text(encoding="utf-8")
    norm = norm.replace(f"package {PKG};", f"package {SHARED};")
    (shared_dir / "CustomerPhoneNormalizer.java").write_text(norm, encoding="utf-8")

    mapper = (UC / "CustomerResponseMapper.java").read_text(encoding="utf-8")
    mapper = mapper.replace(f"package {PKG};", f"package {SHARED};")
    mapper = apply_replacements(mapper)
    mapper = mapper.replace("toResponse", "toSummary")
    mapper = mapper.replace("CustomerSummaryResponseDTOMapper", "CustomerResponseMapper")
    if "class CustomerResponseMapper" not in mapper:
        mapper = mapper.replace(
            "public class CustomerResponseMapper",
            "public class CustomerResponseMapper",
        )
    (shared_dir / "CustomerResponseMapper.java").write_text(mapper, encoding="utf-8")

    (UC / "create").mkdir(parents=True, exist_ok=True)
    (UC / "update").mkdir(parents=True, exist_ok=True)

    create_req = (DTO_OLD / "CreateCustomerRequest.java").read_text(encoding="utf-8")
    create_req = create_req.replace("package com.agenciahub.api.dto.customer;", f"package {CREATE};")
    create_req = create_req.replace("CreateCustomerRequest", "CreateCustomerRequestDTO")
    (UC / "create" / "CreateCustomerRequestDTO.java").write_text(create_req, encoding="utf-8")

    update_req = (DTO_OLD / "UpdateCustomerRequest.java").read_text(encoding="utf-8")
    update_req = update_req.replace("package com.agenciahub.api.dto.customer;", f"package {UPDATE};")
    update_req = update_req.replace("UpdateCustomerRequest", "UpdateCustomerRequestDTO")
    (UC / "update" / "UpdateCustomerRequestDTO.java").write_text(update_req, encoding="utf-8")


def migrate_use_case_folders() -> None:
    for old_name, rel_path in FOLDER_MAP.items():
        src_dir = UC / old_name
        if not src_dir.exists():
            continue
        dest_dir = UC / rel_path
        dest_dir.mkdir(parents=True, exist_ok=True)
        new_pkg = pkg_for_folder(old_name)
        for java_file in src_dir.glob("*.java"):
            if java_file.name.endswith("RequestDTO.java"):
                continue
            text = java_file.read_text(encoding="utf-8")
            text = re.sub(
                rf"package {PKG}\.{re.escape(old_name)};",
                f"package {new_pkg};",
                text,
            )
            text = apply_replacements(text)
            (dest_dir / java_file.name).write_text(text, encoding="utf-8")
        shutil.rmtree(src_dir)


def replace_in_sources() -> None:
    for path in (ROOT / "src").rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        original = text
        text = apply_replacements(text)
        if text != original:
            path.write_text(text, encoding="utf-8")


def cleanup() -> None:
    for name in FOLDER_MAP:
        d = UC / name
        if d.exists():
            shutil.rmtree(d)
    for f in ("CustomerPhoneNormalizer.java", "CustomerResponseMapper.java"):
        p = UC / f
        if p.exists():
            p.unlink()
    if DTO_OLD.exists():
        shutil.rmtree(DTO_OLD)


def main() -> None:
    write_shared()
    migrate_use_case_folders()
    cleanup()
    replace_in_sources()
    print("Customer ADR 0008 pilot done.")


if __name__ == "__main__":
    main()
