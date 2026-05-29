#!/usr/bin/env zsh
set -euo pipefail

cd "$(dirname "$0")/.."

if [[ ! -f maven-central-publish.properties ]]; then
  echo "Missing maven-central-publish.properties" >&2
  echo "Create it with: cp maven-central-publish.properties.example maven-central-publish.properties" >&2
  exit 1
fi

python3 - <<'PY'
from pathlib import Path
import getpass
import os
import re
import subprocess
import sys

props = Path("maven-central-publish.properties")
text = props.read_text()

def value(name: str) -> str | None:
    match = re.search(rf"^{re.escape(name)}=(.*)$", text, re.MULTILINE)
    return match.group(1).strip() if match else None

required = ["mavenCentralUsername", "mavenCentralPassword", "signingInMemoryKey"]
missing = [name for name in required if not value(name)]
placeholders = [name for name in required if (value(name) or "").startswith("YOUR_")]
key = value("signingInMemoryKey") or ""
key_for_gradle = key.replace("\\r", "\r").replace("\\n", "\n")

errors: list[str] = []
if missing:
    errors.append("Missing required properties: " + ", ".join(missing))
if placeholders:
    errors.append("Replace placeholder values for: " + ", ".join(placeholders))
if key and ("BEGIN PGP PRIVATE KEY BLOCK" not in key or "END PGP PRIVATE KEY BLOCK" not in key):
    errors.append("signingInMemoryKey must be an ASCII-armored PGP PRIVATE KEY block")
if key and "BEGIN PGP PRIVATE KEY BLOCK-----\\n\\n" not in key:
    errors.append(
        "signingInMemoryKey looks malformed: the blank armor line after BEGIN is missing. "
        "Re-export with: gpg --armor --export-secret-keys YOUR_KEY_ID | "
        "awk '{sub(/\\r$/, \"\"); printf \"%s\\\\n\", $0;}'"
    )
if key and "-----BEGIN PGP PRIVATE KEY BLOCK-----\n\n" not in key_for_gradle:
    errors.append("signingInMemoryKey could not be decoded to a valid ASCII-armored private key")

if errors:
    print("Invalid maven-central-publish.properties:", file=sys.stderr)
    for error in errors:
        print(f"- {error}", file=sys.stderr)
    sys.exit(1)

passphrase = getpass.getpass("GPG passphrase: ")
if not passphrase:
    print("GPG passphrase must not be empty.", file=sys.stderr)
    sys.exit(1)

env = os.environ.copy()
env.update({
    "ORG_GRADLE_PROJECT_mavenCentralUsername": value("mavenCentralUsername") or "",
    "ORG_GRADLE_PROJECT_mavenCentralPassword": value("mavenCentralPassword") or "",
    "ORG_GRADLE_PROJECT_signingInMemoryKey": key_for_gradle,
    "ORG_GRADLE_PROJECT_signingInMemoryKeyPassword": passphrase,
})

raise SystemExit(subprocess.run([
    "./gradlew",
    "checkSigningConfiguration",
    "clean",
    "build",
    "publishToMavenCentral",
], env=env).returncode)
PY
