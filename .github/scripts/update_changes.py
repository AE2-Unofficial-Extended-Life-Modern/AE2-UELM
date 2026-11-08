from __future__ import annotations

import base64
import html
import os
import re
import sys
from datetime import datetime, timezone
from pathlib import Path


def read_menu_body() -> str:
    menu_file = os.environ.get("MENU_FILE")

    if menu_file:
        path = Path(menu_file)

        if path.exists():
            return path.read_text(encoding="utf-8")

    return ""


def is_enabled(menu_body: str) -> bool:
    return "<!-- changes-state:enabled -->" in menu_body


def custom_title(menu_body: str) -> str | None:
    match = re.search(
        r"<!-- changes-title-b64: ([A-Za-z0-9+/=]*) -->",
        menu_body,
    )

    if not match or not match.group(1):
        return None

    try:
        title = base64.b64decode(
            match.group(1),
            validate=True,
        ).decode("utf-8").strip()
    except Exception:
        return None

    if not title or "\n" in title:
        return None

    return title


def format_merged_time(value: str) -> str:
    try:
        merged_at = datetime.fromisoformat(value.replace("Z", "+00:00"))
        return merged_at.astimezone(timezone.utc).strftime(
            "%Y-%m-%d %H:%M UTC"
        )
    except ValueError:
        return value or "unknown time"


def safe_pr_description(description: str) -> str:
    """
    Keep the PR body as Markdown, but escape tags that could close
    the generated <details> entry early.
    """
    return re.sub(
        r"</?(?:details|summary)\b[^>]*>",
        lambda match: html.escape(match.group(0)),
        description,
        flags=re.IGNORECASE,
    )


menu_body = read_menu_body()

pr_number = os.environ["PR_NUMBER"]
pr_title = os.environ["PR_TITLE"].strip()
pr_author = os.environ["PR_AUTHOR"].strip()
pr_body = os.environ.get("PR_BODY", "").strip()
pr_merged_at = os.environ.get("PR_MERGED_AT", "")

if not is_enabled(menu_body):
    print("This pull request was not selected for CHANGES.md.")
    sys.exit(0)

changes_file = Path("CHANGES.md")

if not changes_file.exists():
    raise SystemExit("CHANGES.md does not exist at the repository root.")

contents = changes_file.read_text(encoding="utf-8")

entry_id = f"<!-- CHANGELOG-PR:{pr_number} -->"

if entry_id in contents:
    print(f"Pull request #{pr_number} is already listed in CHANGES.md.")
    sys.exit(0)

insertion_marker = "<!-- CHANGES:ENTRIES -->"

if insertion_marker not in contents:
    raise SystemExit(
        f"Could not find {insertion_marker} in CHANGES.md."
    )

title = custom_title(menu_body) or pr_title
description = safe_pr_description(pr_body)

if not description:
    description = "_No pull-request description was provided._"

entry = f"""\
{entry_id}
<details>
<summary><strong>{html.escape(title, quote=False)}</strong> · @{html.escape(pr_author, quote=False)} · merged {format_merged_time(pr_merged_at)}</summary>

{description}

</details>

"""

updated_contents = contents.replace(
    insertion_marker,
    f"{entry}{insertion_marker}",
    1,
)

changes_file.write_text(updated_contents, encoding="utf-8")

print(f"Added pull request #{pr_number} to CHANGES.md.")