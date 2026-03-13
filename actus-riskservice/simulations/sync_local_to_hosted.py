import os
import re
import hashlib
import time
import argparse
import logging
from datetime import datetime

# ─────────────────────────────────────────────
#  CONFIG
# ─────────────────────────────────────────────
LOCAL_DIR  = r"C:/KALAIVANI M/ChainAim/mcp server/ACTUS-EXT/actus-risk-service-extension1/actus-riskservice/simulations/local"
HOSTED_DIR = r"C:/KALAIVANI M/ChainAim/mcp server/ACTUS-EXT/actus-risk-service-extension1/actus-riskservice/simulations/hosted"

AWS_HOST = "34.203.247.32"

# How often to re-scan in watch mode (seconds)
WATCH_INTERVAL = 10

# ─────────────────────────────────────────────
#  LOGGING
# ─────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s  %(levelname)-8s  %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
log = logging.getLogger(__name__)


# ─────────────────────────────────────────────
#  HELPERS
# ─────────────────────────────────────────────
def transform_content(content: str) -> str:
    """Replace all localhost references with the AWS host."""
    content = content.replace("localhost:8083", f"{AWS_HOST}:8083")
    content = content.replace("localhost:8082", f"{AWS_HOST}:8082")
    # Bare localhost (no port) – must come last to avoid double-replacing
    content = re.sub(r'localhost(?!:\d)', AWS_HOST, content)
    return content


def file_hash(path: str) -> str:
    """MD5 of a file's content – fast enough for JSON files."""
    h = hashlib.md5()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def should_sync(local_path: str, hosted_path: str) -> tuple[bool, str]:
    """
    Returns (needs_sync: bool, reason: str).

    Decision tree:
      1. Hosted file does not exist          → sync  (NEW)
      2. Local mtime > hosted mtime          → sync  (NEWER)
      3. Content hash differs                → sync  (CHANGED – e.g. mtime touched but content differs)
      4. Everything matches                  → skip  (UNCHANGED)
    """
    if not os.path.exists(hosted_path):
        return True, "NEW"

    local_mtime  = os.path.getmtime(local_path)
    hosted_mtime = os.path.getmtime(hosted_path)

    if local_mtime > hosted_mtime + 0.5:       # 0.5 s tolerance for FAT/NTFS rounding
        return True, "NEWER"

    # Same or older mtime – do a content-hash check to catch edge cases
    # (e.g., file was restored, mtime reset, manual edits in hosted, etc.)
    local_content    = open(local_path,  "r", encoding="utf-8").read()
    transformed      = transform_content(local_content)
    hosted_content   = open(hosted_path, "r", encoding="utf-8").read()

    if hashlib.md5(transformed.encode()).hexdigest() != hashlib.md5(hosted_content.encode()).hexdigest():
        return True, "CHANGED"

    return False, "UNCHANGED"


# ─────────────────────────────────────────────
#  CORE SYNC
# ─────────────────────────────────────────────
def run_sync(local_dir: str = LOCAL_DIR, hosted_dir: str = HOSTED_DIR) -> dict:
    stats = {"new": [], "updated": [], "skipped": [], "errors": []}

    for root, dirs, files in os.walk(local_dir):
        # Sort so output is deterministic
        dirs.sort()
        for filename in sorted(files):
            if not filename.endswith(".json"):
                continue

            local_path   = os.path.join(root, filename)
            relative     = os.path.relpath(local_path, local_dir)
            hosted_path  = os.path.join(hosted_dir, relative)

            try:
                needs_sync, reason = should_sync(local_path, hosted_path)

                if not needs_sync:
                    stats["skipped"].append(relative)
                    continue

                # Read, transform, write
                with open(local_path, "r", encoding="utf-8") as f:
                    content = f.read()

                transformed = transform_content(content)

                os.makedirs(os.path.dirname(hosted_path), exist_ok=True)
                with open(hosted_path, "w", encoding="utf-8") as f:
                    f.write(transformed)

                # Preserve local mtime so next run can detect changes correctly
                local_stat = os.stat(local_path)
                os.utime(hosted_path, (local_stat.st_atime, local_stat.st_mtime))

                if reason == "NEW":
                    stats["new"].append(relative)
                    log.info(f"[NEW]     {relative}")
                else:
                    stats["updated"].append(relative)
                    log.info(f"[{reason:<7}] {relative}")

            except Exception as exc:
                stats["errors"].append((relative, str(exc)))
                log.error(f"[ERROR]   {relative} – {exc}")

    return stats


def print_summary(stats: dict):
    total = len(stats["new"]) + len(stats["updated"]) + len(stats["skipped"])

    print("\n================ SYNC SUMMARY ================")
    print(f"New files copied    : {len(stats['new'])}")
    print(f"Updated files       : {len(stats['updated'])}")
    print(f"Unchanged (skipped) : {len(stats['skipped'])}")
    print(f"Errors              : {len(stats['errors'])}")
    print(f"Total scanned       : {total}")
    print("==============================================\n")
    


# ─────────────────────────────────────────────
#  ENTRY POINT
# ─────────────────────────────────────────────
def main():
    parser = argparse.ArgumentParser(
        description="Sync local ACTUS simulation JSONs → hosted (AWS) directory."
    )
    parser.add_argument(
        "--watch", action="store_true",
        help=f"Keep running and re-sync every {WATCH_INTERVAL}s"
    )
    parser.add_argument(
        "--interval", type=int, default=WATCH_INTERVAL,
        help="Watch interval in seconds (default: 10)"
    )
    args = parser.parse_args()

    if args.watch:
        log.info(f"Watch mode ON – scanning every {args.interval}s. Ctrl+C to stop.")
        try:
            while True:
                log.info(f"=== Sync run @ {datetime.now().strftime('%H:%M:%S')} ===")
                stats = run_sync()
                print_summary(stats)
                time.sleep(args.interval)
        except KeyboardInterrupt:
            log.info("Watch mode stopped.")
    else:
        log.info("=== One-shot sync ===")
        stats = run_sync()
        print_summary(stats)


if __name__ == "__main__":
    main()